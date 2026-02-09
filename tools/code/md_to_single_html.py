from __future__ import annotations

import argparse
import html
import re
from pathlib import Path


HEADING_RE = re.compile(r"^(#{1,6})\s+(.*)$")
LIST_RE = re.compile(r"^(\s*)-\s+(.*)$")
HR_RE = re.compile(r"^\s*---+\s*$")


def slugify(text: str) -> str:
    slug = re.sub(r"[^a-z0-9]+", "-", text.lower()).strip("-")
    return slug or "section"


def inline_md(text: str) -> str:
    code_tokens: list[str] = []

    def replace_code(match: re.Match[str]) -> str:
        code_tokens.append(match.group(1))
        return f"\x00CODE{len(code_tokens) - 1}\x00"

    text = re.sub(r"`([^`]+)`", replace_code, text)
    text = html.escape(text, quote=False)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
    text = re.sub(r"\*([^*]+)\*", r"<em>\1</em>", text)

    for i, token in enumerate(code_tokens):
        text = text.replace(f"\x00CODE{i}\x00", f"<code>{html.escape(token, quote=False)}</code>")
    return text


def clean_css() -> str:
    return """
    :root {
      --bg: #f7f7f5;
      --paper: #ffffff;
      --fg: #1f2328;
      --muted: #57606a;
      --line: #d0d7de;
      --code-bg: #f6f8fa;
      --accent: #0b57d0;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      background: linear-gradient(180deg, #eef2f8 0%, var(--bg) 180px);
      color: var(--fg);
      font: 15px/1.55 "Segoe UI", "Helvetica Neue", Arial, sans-serif;
    }
    .topbar {
      height: 8px;
      background: linear-gradient(90deg, #2b5dad, #5a8be0);
    }
    .layout {
      max-width: 1120px;
      margin: 24px auto 64px;
      background: var(--paper);
      border: 1px solid var(--line);
      border-radius: 12px;
      box-shadow: 0 6px 24px rgba(0,0,0,.06);
      padding: 28px 34px;
      overflow-wrap: anywhere;
    }
    .toc { display: none; }
    h1, h2, h3, h4, h5, h6 {
      margin: 1.2em 0 0.5em;
      line-height: 1.25;
      color: #111827;
      scroll-margin-top: 60px;
    }
    h1 { margin-top: 0; font-size: 1.9rem; border-bottom: 1px solid var(--line); padding-bottom: .35em; }
    h2 { font-size: 1.35rem; border-bottom: 1px solid #eef1f4; padding-bottom: .25em; }
    h3 { font-size: 1.15rem; }
    h4 { font-size: 1.03rem; color: #213547; }
    h5 { font-size: 0.98rem; color: #334155; }
    p { margin: 0.5em 0 0.85em; }
    ul { margin: 0.35em 0 0.85em 1.3em; padding: 0; }
    li { margin: 0.18em 0; }
    code {
      background: var(--code-bg);
      border: 1px solid #d8dee4;
      border-radius: 6px;
      padding: .1em .35em;
      font: 0.92em "Cascadia Code", Consolas, Menlo, monospace;
    }
    pre {
      margin: .7em 0 1em;
      background: #0f172a;
      color: #e5e7eb;
      border-radius: 10px;
      padding: 14px;
      overflow: auto;
      border: 1px solid #1f2937;
    }
    pre code {
      background: transparent;
      border: 0;
      color: inherit;
      padding: 0;
      font-size: 0.9em;
      white-space: pre;
    }
    hr {
      border: 0;
      border-top: 1px solid var(--line);
      margin: 1.2em 0;
    }
    strong { color: #0f172a; }
    em { color: var(--muted); }
    a { color: var(--accent); text-decoration: none; }
    a:hover { text-decoration: underline; }
    @media (max-width: 900px) {
      .layout { margin: 0; border-radius: 0; border-left: 0; border-right: 0; padding: 16px; }
    }
"""


def javadoc_css() -> str:
    return """
    :root {
      --bg: #ffffff;
      --fg: #1a1a1a;
      --line: #cfd6e0;
      --line-soft: #e7ebf1;
      --head-bg: #4d7a97;
      --head-fg: #ffffff;
      --subhead-bg: #edf3fb;
      --code-bg: #f7f8fa;
      --link: #2156a5;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      background: var(--bg);
      color: var(--fg);
      font: 14px/1.45 Arial, Helvetica, sans-serif;
    }
    .topbar {
      background: var(--head-bg);
      color: var(--head-fg);
      border-bottom: 1px solid #3f6078;
      padding: 10px 16px;
      font-weight: bold;
    }
    .layout {
      display: grid;
      grid-template-columns: 300px 1fr;
      gap: 16px;
      align-items: start;
      max-width: 1600px;
      margin: 0 auto;
      padding: 14px 16px 28px;
    }
    .toc {
      position: sticky;
      top: 10px;
      border: 1px solid var(--line);
      background: #fbfcfe;
    }
    .toc h2 {
      margin: 0;
      padding: 8px 10px;
      font-size: 14px;
      background: var(--subhead-bg);
      border-bottom: 1px solid var(--line);
    }
    .toc ul {
      list-style: none;
      margin: 0;
      padding: 8px 10px;
      max-height: calc(100vh - 120px);
      overflow: auto;
    }
    .toc li {
      margin: 0;
      padding: 2px 0;
      font-size: 12px;
      line-height: 1.35;
    }
    .toc .lv2 { margin-top: 6px; font-weight: bold; }
    .toc .lv3 { padding-left: 10px; }
    .toc .lv4 { padding-left: 20px; font-size: 11px; color: #444; }
    .toc a {
      color: var(--link);
      text-decoration: none;
    }
    .toc a:hover { text-decoration: underline; }
    main {
      border: 1px solid var(--line);
      min-width: 0;
      overflow-wrap: anywhere;
    }
    article {
      padding: 12px 14px 22px;
    }
    h1, h2, h3, h4, h5, h6 {
      margin: 14px 0 8px;
      line-height: 1.25;
      scroll-margin-top: 10px;
    }
    h1 {
      margin-top: 2px;
      font-size: 25px;
      border-bottom: 2px solid #8fb0d5;
      padding-bottom: 6px;
    }
    h2 {
      font-size: 20px;
      background: var(--subhead-bg);
      border: 1px solid var(--line);
      padding: 6px 8px;
    }
    h3 {
      font-size: 17px;
      color: #102a43;
      border-bottom: 1px solid var(--line-soft);
      padding-bottom: 3px;
    }
    h4 {
      font-size: 15px;
      color: #1f3f62;
      background: #f7f9fc;
      border: 1px solid var(--line-soft);
      padding: 4px 6px;
    }
    h5 {
      font-size: 14px;
      color: #284b74;
      font-family: "Courier New", Consolas, monospace;
    }
    p { margin: 6px 0 10px; }
    hr {
      border: 0;
      border-top: 1px solid var(--line);
      margin: 14px 0;
    }
    ul { margin: 6px 0 10px 18px; padding: 0; }
    li { margin: 2px 0; }
    code {
      background: var(--code-bg);
      border: 1px solid #d8dee9;
      border-radius: 3px;
      padding: 1px 4px;
      font: 12px/1.3 "Courier New", Consolas, monospace;
    }
    pre {
      margin: 8px 0 12px;
      background: #fcfdff;
      border: 1px solid #d8dee9;
      border-left: 4px solid #9db9d7;
      padding: 8px 10px;
      overflow: auto;
    }
    pre code {
      background: transparent;
      border: 0;
      padding: 0;
      white-space: pre;
      font-size: 12px;
    }
    a { color: var(--link); text-decoration: none; }
    a:hover { text-decoration: underline; }
    @media (max-width: 1100px) {
      .layout {
        grid-template-columns: 1fr;
        padding: 10px;
      }
      .toc {
        position: static;
      }
      .toc ul {
        max-height: 260px;
      }
    }
"""


def render_toc(headings: list[tuple[int, str, str]]) -> str:
    if not headings:
        return ""
    items: list[str] = ["<aside class=\"toc\">", "<h2>Navigation</h2>", "<ul>"]
    for level, text, anchor in headings:
        if level > 4:
            continue
        cls = f"lv{level}"
        items.append(f'<li class="{cls}"><a href="#{html.escape(anchor)}">{html.escape(text)}</a></li>')
    items.extend(["</ul>", "</aside>"])
    return "\n".join(items)


def markdown_to_html(markdown: str, title: str, theme: str) -> str:
    out: list[str] = []
    paragraph: list[str] = []
    list_stack: list[int] = []
    heading_counts: dict[str, int] = {}
    headings: list[tuple[int, str, str]] = []
    in_code = False
    code_lang = ""
    code_lines: list[str] = []

    def flush_paragraph() -> None:
        nonlocal paragraph
        if paragraph:
            out.append(f"<p>{inline_md(' '.join(part.strip() for part in paragraph))}</p>")
            paragraph = []

    def close_lists(to_level: int = -1) -> None:
        while list_stack and list_stack[-1] > to_level:
            out.append("</li></ul>")
            list_stack.pop()

    lines = markdown.splitlines()
    for line in lines:
        if line.startswith("```"):
            if in_code:
                out.append(f'<pre><code class="lang-{html.escape(code_lang)}">{html.escape("\\n".join(code_lines), quote=False)}</code></pre>')
                in_code = False
                code_lang = ""
                code_lines = []
            else:
                flush_paragraph()
                close_lists(-1)
                in_code = True
                code_lang = line[3:].strip()
            continue

        if in_code:
            code_lines.append(line)
            continue

        if not line.strip():
            flush_paragraph()
            continue

        heading = HEADING_RE.match(line)
        if heading:
            flush_paragraph()
            close_lists(-1)
            level = len(heading.group(1))
            plain = heading.group(2).strip()
            base = slugify(plain)
            count = heading_counts.get(base, 0)
            heading_counts[base] = count + 1
            anchor = f"{base}-{count}" if count else base
            content = inline_md(plain)
            out.append(f'<h{level} id="{anchor}">{content}</h{level}>')
            headings.append((level, plain, anchor))
            continue

        if HR_RE.match(line):
            flush_paragraph()
            close_lists(-1)
            out.append("<hr/>")
            continue

        list_item = LIST_RE.match(line)
        if list_item:
            flush_paragraph()
            indent = len(list_item.group(1).replace("\t", "  "))
            level = indent // 2
            content = inline_md(list_item.group(2).strip())

            if not list_stack:
                out.append("<ul><li>")
                list_stack.append(level)
            else:
                current = list_stack[-1]
                if level > current:
                    while list_stack and level > list_stack[-1]:
                        out.append("<ul><li>")
                        list_stack.append(list_stack[-1] + 1)
                elif level < current:
                    close_lists(level)
                    out.append("</li><li>")
                else:
                    out.append("</li><li>")

            out.append(content)
            continue

        if list_stack:
            out.append("<br/>" + inline_md(line.strip()))
            continue

        paragraph.append(line)

    flush_paragraph()
    if in_code:
        out.append(f'<pre><code class="lang-{html.escape(code_lang)}">{html.escape("\\n".join(code_lines), quote=False)}</code></pre>')
    close_lists(-1)

    body = "\n".join(out)
    toc = render_toc(headings)
    css = javadoc_css() if theme == "javadoc" else clean_css()
    topbar_title = f"{title} - API Documentation" if theme == "javadoc" else title
    layout_class = "layout"
    main_start = "<main><article>"
    main_end = "</article></main>"
    return f"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>{html.escape(title)}</title>
  <style>
{css}
  </style>
</head>
<body>
  <div class="topbar">{html.escape(topbar_title)}</div>
  <div class="{layout_class}">
  {toc}
  {main_start}
{body}
  {main_end}
  </div>
</body>
</html>
"""


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert Markdown file to single self-contained HTML.")
    parser.add_argument("input", type=Path, help="Input Markdown file path")
    parser.add_argument("output", type=Path, help="Output HTML file path")
    parser.add_argument("--title", type=str, default=None, help="Optional HTML title")
    parser.add_argument("--theme", choices=("clean", "javadoc"), default="clean", help="HTML theme style")
    args = parser.parse_args()

    markdown = args.input.read_text(encoding="utf-8")
    title = args.title or args.input.stem
    html_doc = markdown_to_html(markdown, title, args.theme)
    args.output.write_text(html_doc, encoding="utf-8")


if __name__ == "__main__":
    main()
