#!/usr/bin/env python3
"""Port the Aniyomi scrapers into the CloudStream plugin.

Only the scraper file itself is copied: the shared MediaVideo / VideoProxy helpers are
replaced by the compat layer, and the unused per-module VideoProxy is dropped. Site
selectors and decoders are left untouched.
"""
import os
import re
import sys

SRC = "/data/data/com.termux/files/home/animetail-extensions"
DST = "/data/data/com.termux/files/home/AIO-Hentai/AIO-Hentai/src/main/kotlin/com/aiohentai/cs/sites"

IMPORT_MAP = [
    ("eu.kanade.tachiyomi.animesource.model.", "com.aiohentai.cs.compat."),
    ("eu.kanade.tachiyomi.animesource.online.AnimeHttpSource", "com.aiohentai.cs.compat.AnimeHttpSource"),
    ("eu.kanade.tachiyomi.network.GET", "com.aiohentai.cs.compat.GET"),
    ("eu.kanade.tachiyomi.util.asJsoup", "com.aiohentai.cs.compat.asJsoup"),
    ("eu.kanade.tachiyomi.animeextension.proxy.MediaVideo", "com.aiohentai.cs.compat.MediaVideo"),
]


def convert(text: str, site: str) -> str:
    out = re.sub(r"^package .*$", f"package com.aiohentai.cs.sites.{site}", text, count=1, flags=re.M)
    for old, new in IMPORT_MAP:
        out = out.replace(old, new)
    if "eu.kanade." in out:
        raise SystemExit(f"unmapped import left in {site}: " + repr([l for l in out.splitlines() if "eu.kanade." in l]))
    return out


def main():
    modules = sorted(d for d in os.listdir(SRC) if d.startswith("extension") and os.path.isdir(os.path.join(SRC, d)))
    written = []
    for module in modules:
        site = "watchhentai" if module == "extension" else module.removeprefix("extension-")
        files = []
        for root, _, names in os.walk(os.path.join(SRC, module, "src")):
            for name in names:
                if name.endswith(".kt") and "/proxy/" not in root.replace(os.sep, "/"):
                    files.append(os.path.join(root, name))
        scraper = None
        for path in files:
            text = open(path, encoding="utf-8").read()
            if re.search(r"class \w+ : AnimeHttpSource\(\)", text):
                scraper = (path, text)
                break
        if scraper is None:
            raise SystemExit(f"no scraper found in {module}")

        path, text = scraper
        match = re.search(r"^class (\w+) : AnimeHttpSource\(\)", text, re.M)
        cls = match.group(1)

        outdir = os.path.join(DST, site)
        os.makedirs(outdir, exist_ok=True)
        with open(os.path.join(outdir, f"{cls}.kt"), "w", encoding="utf-8") as fh:
            fh.write(convert(text, site))

        with open(os.path.join(outdir, f"{cls}Provider.kt"), "w", encoding="utf-8") as fh:
            fh.write(
                f"""package com.aiohentai.cs.sites.{site}

import com.aiohentai.cs.compat.AniyomiProvider

class {cls}Provider : AniyomiProvider({cls}())
"""
            )
        written.append((site, cls))

    print(f"ported {len(written)} sources")
    names = [c for _, c in written]
    dupes = {n for n in names if names.count(n) > 1}
    if dupes:
        print("duplicate class names:", dupes)


if __name__ == "__main__":
    sys.exit(main())
