#!/usr/bin/env python3
"""Discover genre/category browse paths for each source from its own home page.

Writes a Kotlin data file consumed by compat.Genres. Only paths that look like
listing sections are kept; the result is verified later by scripts/genre_smoke.
"""
import json
import os
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor

ROOT = "/data/data/com.termux/files/home/AIO-Hentai"
SITES = os.path.join(ROOT, "AIO-Hentai/src/main/kotlin/com/aiohentai/cs/sites")
OUT = os.path.join(ROOT, "AIO-Hentai/src/main/kotlin/com/aiohentai/cs/compat/Genres.kt")
AUDIT = "/data/data/com.termux/files/usr/tmp/opencode/genre_audit.txt"
MAX_GENRES = 8
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")

BAD = re.compile(
    r"(/page/\d|^/tag/|/tag/|/genre/?$|/category/?$|/genres/?$|uncategorized|"
    r"\?|#|javascript|/profile/|/user/|/login|/register|/dmca|/contact|/privacy)",
    re.IGNORECASE,
)

GENERIC_TEXT = {
    "view more", "see more", "more", "view all", "all", "show more",
    "browse", "browse all", "genre", "genres", "category", "categories",
}


def slug_title(href: str) -> str:
    slug = href.rstrip("/").rsplit("/", 1)[-1]
    slug = re.sub(r"[-_]+", " ", slug).strip()
    if not slug:
        return "More"
    return " ".join(w.capitalize() if w.isalpha() else w for w in slug.split())


def fetch(url: str) -> str:
    try:
        out = subprocess.run(
            ["curl", "-sL", "-m", "20", "-A", UA, "--compressed", url],
            capture_output=True, timeout=30,
        )
        return out.stdout.decode("utf-8", "replace")[:600000]
    except Exception:
        return ""


def genres_for(site: str, base: str):
    html = fetch(base if base.endswith("/") else base + "/")
    if not html:
        return site, []
    seen = {}
    # anchor text + href
    for m in re.finditer(r'<a[^>]+href="([^"]+)"[^>]*>(.*?)</a>', html, re.S | re.I):
        href, inner = m.group(1), m.group(2)
        text = re.sub(r"<[^>]+>", " ", inner)
        text = re.sub(r"\s+", " ", text).strip()
        if not text or len(text) > 30:
            continue
        if hud := re.match(r"^(https?:)?//[^/]+(/.*)$", href):
            href = hud.group(2)
        if not href.startswith("/"):
            continue
        if not re.search(r"/(genre|category|genres)/", href, re.I):
            continue
        if BAD.search(href) or BAD.search(text):
            continue
        if not text or text.lower() in GENERIC_TEXT:
            text = slug_title(href)
        key = href.rstrip("/")
        if key in seen or text.lower() in {v.lower() for v in seen.values()}:
            continue
        seen[key] = text
        if len(seen) >= MAX_GENRES:
            break
    return site, list(seen.items())


def main():
    targets = []
    for line in open(AUDIT, encoding="utf-8"):
        site, _name, url = line.rstrip("\n").split("\t")
        targets.append((site, url))

    result = {}
    with ThreadPoolExecutor(max_workers=10) as pool:
        for site, items in pool.map(lambda t: genres_for(*t), targets):
            if items:
                result[site] = items

    lines = [
        "package com.aiohentai.cs.compat",
        "",
        "/**",
        " * Genre/category browse sections per source, discovered from each site's own",
        " * navigation by scripts/gen_genres.py. Rows are static because CloudStream reads",
        " * `mainPage` synchronously when the provider is built.",
        " */",
        "object Genres {",
        "",
        "    val bySite: Map<String, List<Pair<String, String>>> = mapOf(",
    ]
    for site in sorted(result):
        pairs = ", ".join(f'"{name}" to "{path}"' for path, name in result[site])
        lines.append(f'        "{site}" to listOf({pairs}),')
    lines += ["    )", "}", ""]
    with open(OUT, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines))

    print(f"{len(result)} sites with genres -> {OUT}")
    for site in sorted(result):
        print(f"  {site}: {len(result[site])}")


if __name__ == "__main__":
    sys.exit(main())
