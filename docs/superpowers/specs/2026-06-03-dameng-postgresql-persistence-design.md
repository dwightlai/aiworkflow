# Dameng and PostgreSQL Persistence Design

## Goal

Extend the server persistence layer so it can run on Dameng while keeping the existing PostgreSQL path working by default. The change must not add a Dameng JDBC dependency because the runtime environment will provide the driver.

## Current Context

The server uses Spring Boot 3.3.5, MyBatis-Plus 3.5.9, and Flyway. Persistence code is mostly database-neutral MyBatis-Plus mapper usage with `LambdaQueryWrapper`. Entity classes already point at final `agi_*` table names.

The database-specific behavior is concentrated in configuration and Flyway SQL. Existing migrations use PostgreSQL/H2-compatible syntax such as `CREATE TABLE IF NOT EXISTS`, `TIMESTAMP WITH TIME ZONE`, `BOOLEAN`, `ADD COLUMN IF NOT EXISTS`, and `ALTER TABLE IF EXISTS RENAME`.

## Recommended Architecture

Use database-specific Flyway locations:

- `classpath:db/migration/postgresql` for the current PostgreSQL migrations.
- `classpath:db/migration/dameng` for Dameng migrations.

PostgreSQL remains the default runtime configuration. Dameng is selected by profile or explicit properties. The application exposes configurable datasource and Flyway settings through environment variables, while keeping the default URL and credentials compatible with the existing Docker PostgreSQL setup.

## Dameng Migration Model

The Dameng migration set creates the final `agi_*` schema directly. It does not replay the PostgreSQL history where old non-prefixed tables are created and later renamed in `V12__agi_table_prefix.sql`.

This keeps Dameng support focused on new Dameng deployments and avoids fragile conditional DDL for historical PostgreSQL-only schema states.

## Type Mapping

Dameng DDL should use conservative types:

- String identifiers and short text: `VARCHAR`.
- Large text and JSON strings: `CLOB`.
- Timestamps: `TIMESTAMP`.
- Integers: `INTEGER`.
- Decimal prices: `NUMERIC(18, 6)`.
- Boolean fields: `NUMBER(1)`.

The `NUMBER(1)` choice is deliberate for Dameng compatibility. MyBatis-Plus entity fields can remain Java `Boolean` or primitive `boolean`, but verification must include mapping `true`/`false` through insert and select paths. If the driver does not map `NUMBER(1)` to Boolean automatically in the deployment environment, add a small MyBatis type handler in the implementation.

## Configuration

Default configuration keeps PostgreSQL:

- `spring.datasource.url` defaults to `jdbc:postgresql://localhost:5432/aiworkflow`.
- `spring.datasource.username` defaults to `aiworkflow`.
- `spring.datasource.password` defaults to `aiworkflow`.
- `spring.flyway.locations` defaults to `classpath:db/migration/postgresql`.

Dameng profile configuration should provide examples only:

- `spring.datasource.url=${DAMENG_JDBC_URL:jdbc:dm://localhost:5236/AIWORKFLOW}`.
- `spring.datasource.driver-class-name=${DAMENG_DRIVER_CLASS_NAME:dm.jdbc.driver.DmDriver}`.
- `spring.flyway.locations=classpath:db/migration/dameng`.

No Dameng driver dependency is added to `pom.xml`.

## Testing Strategy

PostgreSQL compatibility:

- Move existing Flyway migration tests to the PostgreSQL migration location.
- Keep existing H2 PostgreSQL-mode MyBatis integration tests as the regression suite for current behavior.

Dameng compatibility without a driver:

- Add static migration tests that read the Dameng SQL files and fail on PostgreSQL-only syntax.
- Verify the Dameng scripts define all `@TableName("agi_*")` tables used by persistence entities.
- Verify Boolean columns in Dameng scripts use `NUMBER(1)`.

Real Dameng integration remains an environment test because the driver and database are provided outside this repository.

## Error Handling and Operational Notes

If the Dameng profile is used without the runtime driver, startup should fail with Spring Boot's normal datasource driver error. The project should document that this is expected until the deployment provides `DmDriver`.

Schema history is independent per database because Flyway locations differ. Existing PostgreSQL installations should continue using the PostgreSQL location and their existing `flyway_schema_history`.

## Implementation Boundaries

In scope:

- Configuration changes for selecting PostgreSQL or Dameng.
- Moving PostgreSQL migration scripts into a PostgreSQL-specific location.
- Adding Dameng migration scripts for the final `agi_*` schema.
- Tests that prove PostgreSQL migration behavior remains intact and Dameng SQL follows the supported contract.

Out of scope:

- Adding Dameng JDBC dependency.
- Migrating existing PostgreSQL data to Dameng.
- Supporting in-place upgrade from non-prefixed Dameng tables.
- Replacing MyBatis-Plus or changing domain store APIs.
