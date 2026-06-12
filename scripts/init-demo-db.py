import os
import shutil
import subprocess
import sys
import tempfile
import time

try:
    import psycopg2
    from psycopg2 import sql
except ImportError:
    print("pip install psycopg2-binary")
    sys.exit(1)

HOST = os.environ.get("POSTGRES_HOST", "localhost")
PORT = int(os.environ.get("POSTGRES_PORT", "5432"))
SOURCE_DB = os.environ.get("SOURCE_DB", "aiworkflow")
TARGET_DB = os.environ.get("TARGET_DB", "aiworkflow_demo")

ADMIN_CANDIDATES = [
    (os.environ.get("POSTGRES_ADMIN_USER", "postgres"), os.environ.get("POSTGRES_ADMIN_PASSWORD", "postgres")),
    (os.environ.get("POSTGRES_USERNAME", "aiworkflow"), os.environ.get("POSTGRES_PASSWORD", "aiworkflow")),
]


def connect(user: str, password: str, dbname: str = "postgres"):
    return psycopg2.connect(host=HOST, port=PORT, user=user, password=password, dbname=dbname)


def database_exists(cur, name: str) -> bool:
    cur.execute("SELECT 1 FROM pg_database WHERE datname = %s", (name,))
    return cur.fetchone() is not None


def force_disconnect(cur, name: str) -> None:
    for _ in range(5):
        try:
            cur.execute(
                sql.SQL("ALTER DATABASE {} WITH ALLOW_CONNECTIONS = false").format(sql.Identifier(name))
            )
        except Exception:
            pass
        cur.execute(
            """
            SELECT pg_terminate_backend(pid)
            FROM pg_stat_activity
            WHERE datname = %s AND pid <> pg_backend_pid()
            """,
            (name,),
        )
        time.sleep(0.5)


def allow_connections(cur, name: str) -> None:
    cur.execute(
        sql.SQL("ALTER DATABASE {} WITH ALLOW_CONNECTIONS = true").format(sql.Identifier(name))
    )


def drop_database(cur, name: str) -> None:
    force_disconnect(cur, name)
    cur.execute(sql.SQL("DROP DATABASE IF EXISTS {}").format(sql.Identifier(name)))


def clone_with_template(cur) -> None:
    if not database_exists(cur, SOURCE_DB):
        raise RuntimeError(f"source database not found: {SOURCE_DB}")

    staging = f"{TARGET_DB}_sync"
    if database_exists(cur, staging):
        drop_database(cur, staging)
    if database_exists(cur, TARGET_DB):
        drop_database(cur, TARGET_DB)

    force_disconnect(cur, SOURCE_DB)
    cur.execute(
        sql.SQL("CREATE DATABASE {} WITH TEMPLATE {}").format(
            sql.Identifier(staging),
            sql.Identifier(SOURCE_DB),
        )
    )
    allow_connections(cur, SOURCE_DB)
    allow_connections(cur, staging)

    if database_exists(cur, TARGET_DB):
        drop_database(cur, TARGET_DB)
    cur.execute(
        sql.SQL("ALTER DATABASE {} RENAME TO {}").format(
            sql.Identifier(staging),
            sql.Identifier(TARGET_DB),
        )
    )
    allow_connections(cur, TARGET_DB)
    print(f"cloned {SOURCE_DB} -> {TARGET_DB} (template)")


def find_pg_tool(name: str) -> str | None:
    found = shutil.which(name)
    if found:
        return found
    for base in (r"C:\Program Files\PostgreSQL", r"C:\Program Files (x86)\PostgreSQL"):
        if not os.path.isdir(base):
            continue
        for version_dir in sorted(os.listdir(base), reverse=True):
            candidate = os.path.join(base, version_dir, "bin", f"{name}.exe")
            if os.path.isfile(candidate):
                return candidate
    return None


def clone_with_dump(user: str, password: str) -> None:
    pg_dump = find_pg_tool("pg_dump")
    psql = find_pg_tool("psql")
    if not pg_dump or not psql:
        raise RuntimeError("pg_dump/psql not found")

    env = os.environ.copy()
    env["PGPASSWORD"] = password

    with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False, encoding="utf-8") as tmp:
        dump_path = tmp.name

    try:
        subprocess.run(
            [pg_dump, "-h", HOST, "-p", str(PORT), "-U", user, "-d", SOURCE_DB, "--no-owner", "--no-acl", "-f", dump_path],
            check=True,
            env=env,
        )

        with connect(user, password) as conn:
            conn.autocommit = True
            cur = conn.cursor()
            drop_database(cur, TARGET_DB)
            cur.execute(sql.SQL("CREATE DATABASE {}").format(sql.Identifier(TARGET_DB)))
            cur.close()

        subprocess.run(
            [psql, "-h", HOST, "-p", str(PORT), "-U", user, "-d", TARGET_DB, "-f", dump_path],
            check=True,
            env=env,
        )
        print(f"cloned {SOURCE_DB} -> {TARGET_DB} (pg_dump)")
    finally:
        if os.path.exists(dump_path):
            os.remove(dump_path)


def list_tables(conn) -> list[str]:
    cur = conn.cursor()
    cur.execute(
        """
        SELECT table_name
        FROM information_schema.tables
        WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
        ORDER BY table_name
        """
    )
    return [row[0] for row in cur.fetchall()]


def sync_data(user: str, password: str) -> None:
    with connect(user, password) as admin_conn:
        admin_conn.autocommit = True
        admin_cur = admin_conn.cursor()
        if not database_exists(admin_cur, SOURCE_DB):
            raise RuntimeError(f"source database not found: {SOURCE_DB}")
        admin_cur.close()

    with connect(user, password) as admin_conn:
        admin_conn.autocommit = True
        cur = admin_conn.cursor()
        if not database_exists(cur, TARGET_DB):
            cur.execute(sql.SQL("CREATE DATABASE {}").format(sql.Identifier(TARGET_DB)))
        cur.close()

    src = connect(user, password, SOURCE_DB)
    tgt = connect(user, password, TARGET_DB)
    src.autocommit = True
    tgt.autocommit = False

    tables = list_tables(src)
    tgt_cur = tgt.cursor()
    try:
        tgt_cur.execute("SET session_replication_role = replica")
    except Exception:
        pass

    table_idents = sql.SQL(", ").join(sql.Identifier(t) for t in tables)
    if tables:
        tgt_cur.execute(sql.SQL("TRUNCATE TABLE {} RESTART IDENTITY CASCADE").format(table_idents))

    src_cur = src.cursor()
    for table in tables:
        src_cur.execute(sql.SQL("SELECT * FROM {}").format(sql.Identifier(table)))
        rows = src_cur.fetchall()
        if not rows:
            continue
        cols = [desc[0] for desc in src_cur.description]
        insert = sql.SQL("INSERT INTO {} ({}) VALUES ({})").format(
            sql.Identifier(table),
            sql.SQL(", ").join(sql.Identifier(c) for c in cols),
            sql.SQL(", ").join(sql.Placeholder() for _ in cols),
        )
        tgt_cur.executemany(insert.as_string(tgt), rows)
        print(f"  copied {table}: {len(rows)} rows")

    try:
        tgt_cur.execute("SET session_replication_role = DEFAULT")
    except Exception:
        pass
    tgt.commit()
    src.close()
    tgt.close()
    print(f"synced data {SOURCE_DB} -> {TARGET_DB}")


def main() -> int:
    print(f"sync demo database: {SOURCE_DB} -> {TARGET_DB}")
    print("please stop demo(18080) and dev(8080) first")

    last_error: Exception | None = None
    for user, password in ADMIN_CANDIDATES:
        if not user or not password:
            continue
        try:
            with connect(user, password) as conn:
                conn.autocommit = True
                cur = conn.cursor()
                try:
                    clone_with_template(cur)
                    return 0
                except Exception as exc:
                    last_error = exc
                    print(f"template clone failed ({user}): {exc}")
                finally:
                    try:
                        allow_connections(cur, SOURCE_DB)
                    except Exception:
                        pass
                    cur.close()
        except Exception as exc:
            last_error = exc
            print(f"connect failed ({user}): {exc}")
            continue

        try:
            clone_with_dump(user, password)
            return 0
        except Exception as exc:
            last_error = exc
            print(f"dump clone failed ({user}): {exc}")

        try:
            sync_data(user, password)
            return 0
        except Exception as exc:
            last_error = exc
            print(f"data sync failed ({user}): {exc}")

    print(f"sync failed: {last_error}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
