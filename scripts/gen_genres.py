#!/usr/bin/env python3
"""Discover genre/category browse paths for each source from its own navigation.

Preference order per site:
  1. genre/category links on the home page
  2. genre/category links on a dedicated index page (/genre/, /genres/, ...)
  3. tag links on the home page (last resort, fewer rows)

The result is verified by `provider_smoke.sh --genres`; rows that return no items are
pruned afterwards.
"""
import os
import re
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor

ROOT = "/data/data/com.termux/files/home/AIO-Hentai"
OUT = os.path.join(ROOT, "AIO-Hentai/src/main/kotlin/com/aiohentai/cs/compat/Genres.kt")
AUDIT = "/data/data/com.termux/files/usr/tmp/opencode/genre_audit.txt"
MAX_GENRES = 8
MAX_TAGS = 6
UA = ("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
      "(KHTML, like Gecko) Chrome/124.0 Safari/537.36")

INDEX_PATHS = ["/genre/", "/genres/", "/hentai-genre/", "/category/", "/tags/"]

BAD = re.compile(
    r"(/page/\d|/tag/|/genre/?$|/category/?$|/genres/?$|uncategorized|"
    r"\?|#|javascript|/profile/|/user/|/login|/register|/dmca|/contact|/privacy)",
    re.IGNORECASE,
)
BAD_TAG = re.compile(
    r"(/page/\d|/tag/?$|/genre/?$|/category/?$|/genres/?$|uncategorized|"
    r"\?|#|javascript|/profile/|/user/|/login|/register)",
    re.IGNORECASE,
)

GENERIC_TEXT = {
    "view more", "see more", "more", "view all", "all", "show more",
    "browse", "browse all", "genre", "genres", "category", "categories", "tags",
}


def fetch(url: str) -> str:
    try:
        out = subprocess.run(
            ["curl", "-sL", "-m", "20", "-A", UA, "--compressed", url],
            capture_output=True, timeout=30,
        )
        return out.stdout.decode("utf-8", "replace")[:600000]
    except Exception:
        return ""


def slug_title(href: str) -> str:
    slug = href.rstrip("/").rsplit("/", 1)[-1]
    slug = re.sub(r"[-_]+", " ", slug).strip()
    if not slug:
        return "More"
    return " ".join(w.capitalize() if w.isalpha() else w for w in slug.split())


def collect(html: str, pattern: str, bad, limit: int):
    seen = {}
    names = set()
    for m in re.finditer(r'<a[^>]+href="([^"]+)"[^>]*>(.*?)</a>', html, re.S | re.I):
        href = m.group(1)
        text = re.sub(r"\s+", " ", re.sub(r"<[^>]+>", " ", m.group(2))).strip()
        if hud := re.match(r"^(https?:)?//[^/]+(/.*)$", href):
            href = hud.group(2)
        if not href.startswith("/") or not re.search(pattern, href, re.I):
            continue
        if bad.search(href):
            continue
        if len(text) > 30:
            continue
        if not text or text.lower() in GENERIC_TEXT:
            text = slug_title(href)
        if bad.search(text):
            continue
        key = href.rstrip("/")
        if key in seen or text.lower() in names:
            continue
        seen[key] = text
        names.add(text.lower())
        if len(seen) >= limit:
            break
    return list(seen.items())


def genres_for(site: str, base: str):
    base = base if base.endswith("/") else base + "/"
    home = fetch(base)
    if home:
        items = collect(home, r"/(genre|category|genres)/", BAD, MAX_GENRES)
        if items:
            return site, items

    for path in INDEX_PATHS:
        page = fetch(base.rstrip("/") + path)
        if not page:
            continue
        items = collect(page, r"/(genre|category|genres)/", BAD, MAX_GENRES)
        if items:
            return site, items

    if home:
        items = collect(home, r"/tag/", BAD_TAG, MAX_TAGS)
        if items:
            return site, items
    return site, []


def main():
    targets = []
    for line in open(AUDIT, encoding="utf-8"):
        site, _name, url = line.rstrip("\n").split("\t")
        targets.append((site, url))

    result = {}
    with ThreadPoolExecutor(max_workers=12) as pool:
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

    print(f"{len(result)} sites with browse rows -> {OUT}")
    for site in sorted(result):
        print(f"  {site}: {len(result[site])}")


if __name__ == "__main__":
    sys.exit(main())
