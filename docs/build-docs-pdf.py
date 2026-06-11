import base64
import html
import mimetypes
import re
import sys
from pathlib import Path

import fitz
import markdown
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent

DOCS = [
    ("aiworkflow-whitepaper.md", "AI Workflow 白皮书", "aiworkflow-whitepaper.pdf"),
    ("aiworkflow-system-design.md", "AI Workflow 系统设计说明书", "aiworkflow-system-design.pdf"),
    ("aiworkflow-user-manual.md", "AI Workflow 操作手册", "aiworkflow-user-manual.pdf"),
    ("aiworkflow-tech-manual.md", "AI Workflow 技术手册", "aiworkflow-tech-manual.pdf"),
]

CSS = """
@page { size: A4; margin: 18mm 16mm; }
body {
  font-family: "Microsoft YaHei", "PingFang SC", "Segoe UI", sans-serif;
  font-size: 11pt;
  line-height: 1.6;
  color: #1f1f1f;
}
h1 { font-size: 22pt; border-bottom: 2px solid #1677ff; padding-bottom: 8px; page-break-before: always; }
h1:first-child { page-break-before: auto; }
h2 { font-size: 16pt; margin-top: 24px; color: #1677ff; }
h3 { font-size: 13pt; margin-top: 18px; }
h4 { font-size: 12pt; margin-top: 14px; }
p, li { orphans: 3; widows: 3; }
ul, ol { padding-left: 1.4em; }
table { width: 100%; border-collapse: collapse; margin: 12px 0; font-size: 10pt; }
th, td { border: 1px solid #d9d9d9; padding: 6px 8px; text-align: left; vertical-align: top; }
th { background: #f5f5f5; }
code { font-family: Consolas, "Courier New", monospace; background: #f5f5f5; padding: 1px 4px; border-radius: 3px; font-size: 0.92em; }
pre { background: #f5f5f5; padding: 12px; border-radius: 6px; overflow-wrap: anywhere; white-space: pre-wrap; font-size: 9.5pt; }
pre code { background: none; padding: 0; }
img { max-width: 100%; height: auto; display: block; margin: 12px auto; border: 1px solid #eee; border-radius: 6px; }
hr { border: none; border-top: 1px solid #e8e8e8; margin: 24px 0; }
blockquote { margin: 12px 0; padding: 8px 12px; border-left: 4px solid #1677ff; background: #f6faff; color: #555; }
a { color: #1677ff; text-decoration: none; }
.bm { font-size: 1px; color: #ffffff; line-height: 0; user-select: none; }
"""


def embed_images(text: str) -> str:
    def repl(match: re.Match[str]) -> str:
        alt, path = match.group(1), match.group(2)
        if path.startswith("data:"):
            return match.group(0)
        img_path = (ROOT / path).resolve()
        if not img_path.is_file():
            return match.group(0)
        mime = mimetypes.guess_type(img_path.name)[0] or "image/png"
        encoded = base64.b64encode(img_path.read_bytes()).decode("ascii")
        return f"![{alt}](data:{mime};base64,{encoded})"

    return re.sub(r"!\[([^\]]*)\]\(([^)]+)\)", repl, text)


def clean_title(raw: str) -> str:
    t = re.sub(r"\[([^\]]+)\]\([^)]+\)", r"\1", raw.strip())
    return re.sub(r"\s*\{#.+?\}\s*$", "", t)


def extract_headings(text: str) -> list[tuple[int, str, str]]:
    in_fence = False
    skip_toc = False
    headings: list[tuple[int, str, str]] = []
    idx = 0
    for line in text.splitlines():
        stripped = line.strip()
        if stripped.startswith("```"):
            in_fence = not in_fence
            continue
        if in_fence:
            continue
        m = re.match(r"^(#{1,4})\s+(.+?)\s*$", line)
        if not m:
            continue
        level = len(m.group(1))
        title = clean_title(m.group(2))
        if not title:
            continue
        if level == 2 and title == "目录":
            skip_toc = True
            continue
        if skip_toc:
            if level == 2:
                skip_toc = False
            else:
                continue
        idx += 1
        headings.append((level, title, f"BM{idx:04d}"))
    return headings


def md_to_html(md_text: str) -> str:
    return markdown.markdown(
        md_text,
        extensions=["tables", "fenced_code", "sane_lists"],
    )


def inject_bookmark_markers(body_html: str, headings: list[tuple[int, str, str]]) -> str:
    for level, _title, bm_id in headings:
        marker = f'<p class="bm">{bm_id}</p>'
        pattern = rf"<h{level}>(.*?)</h{level}>"

        def repl(match: re.Match[str], level=level, marker=marker) -> str:
            return f"{marker}<h{level}>{match.group(1)}</h{level}>"

        body_html, n = re.subn(pattern, repl, body_html, count=1, flags=re.DOTALL)
        if n == 0:
            break
    return body_html


def build_html(md_text: str, title: str, headings: list[tuple[int, str, str]]) -> str:
    body = inject_bookmark_markers(md_to_html(md_text), headings)
    return f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>{html.escape(title)}</title>
  <style>{CSS}</style>
</head>
<body>{body}</body>
</html>"""


def find_bookmark(doc: fitz.Document, bm_id: str) -> tuple[int, float] | None:
    for pno in range(len(doc)):
        rects = doc[pno].search_for(bm_id)
        if rects:
            return pno, rects[0].y0
    return None


def add_pdf_outline(pdf_path: Path, headings: list[tuple[int, str, str]]) -> int:
    doc = fitz.open(pdf_path)
    toc: list[list] = []
    for level, title, bm_id in headings:
        loc = find_bookmark(doc, bm_id)
        if loc is None:
            continue
        page, y = loc
        toc.append([
            level,
            title,
            page + 1,
            {
                "kind": fitz.LINK_GOTO,
                "page": page,
                "to": fitz.Point(72, y),
                "zoom": 0,
            },
        ])
    if toc:
        doc.set_toc(toc)
    try:
        doc.pdf_catalog()[fitz.PDF_NAME("PageMode")] = fitz.PDF_NAME("UseOutlines")
    except Exception:
        pass
    count = len(toc)
    tmp = pdf_path.with_suffix(".tmp.pdf")
    doc.save(str(tmp), garbage=4, deflate=True)
    doc.close()
    tmp.replace(pdf_path)
    return count


def render_pdf(md_file: Path, pdf_file: Path, title: str) -> None:
    md_raw = md_file.read_text(encoding="utf-8")
    md_text = embed_images(md_raw)
    headings = extract_headings(md_raw)
    html_doc = build_html(md_text, title, headings)

    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        page.set_content(html_doc, wait_until="load")
        page.pdf(
            path=str(pdf_file),
            format="A4",
            print_background=True,
            margin={"top": "18mm", "right": "16mm", "bottom": "18mm", "left": "16mm"},
        )
        browser.close()

    bookmark_count = add_pdf_outline(pdf_file, headings)
    size_mb = pdf_file.stat().st_size / (1024 * 1024)
    print(f"{pdf_file.name} ({size_mb:.2f} MB, {bookmark_count} bookmarks)")


def main() -> None:
    targets = sys.argv[1:] if len(sys.argv) > 1 else [d[0] for d in DOCS]
    name_map = {d[0]: (d[1], d[2]) for d in DOCS}
    for md_name in targets:
        if md_name not in name_map:
            print(f"skip unknown: {md_name}")
            continue
        title, pdf_name = name_map[md_name]
        md_path = ROOT / md_name
        pdf_path = ROOT / pdf_name
        if not md_path.is_file():
            print(f"missing: {md_path}")
            continue
        print(f"building {md_name} ...")
        render_pdf(md_path, pdf_path, title)
    print("done")


if __name__ == "__main__":
    main()
