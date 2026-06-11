import base64
import mimetypes
import re
from pathlib import Path

import markdown
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parent
MD_FILE = ROOT / "aiworkflow-user-manual.md"
PDF_FILE = ROOT / "aiworkflow-user-manual.pdf"

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


def build_html(md_text: str) -> str:
    body = markdown.markdown(
        md_text,
        extensions=["tables", "fenced_code", "toc", "sane_lists"],
    )
    return f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8" />
  <title>AI Workflow 操作手册</title>
  <style>{CSS}</style>
</head>
<body>{body}</body>
</html>"""


def main() -> None:
    md_text = embed_images(MD_FILE.read_text(encoding="utf-8"))
    html = build_html(md_text)
    with sync_playwright() as p:
        browser = p.chromium.launch()
        page = browser.new_page()
        page.set_content(html, wait_until="load")
        page.pdf(
            path=str(PDF_FILE),
            format="A4",
            print_background=True,
            margin={"top": "18mm", "right": "16mm", "bottom": "18mm", "left": "16mm"},
        )
        browser.close()
    print(PDF_FILE)


if __name__ == "__main__":
    main()
