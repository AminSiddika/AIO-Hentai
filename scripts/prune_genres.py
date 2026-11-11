#!/usr/bin/env python3
"""Prune genre rows that returned no items in a `provider_smoke.sh --genres` run.

Usage:
  ./scripts/provider_smoke.sh --genres > /tmp/genres.log 2>&1 || true
  python3 scripts/prune_genres.py /tmp/genres.log [--dry-run]

Rules:
  * a row with `count == 0` is removed
  * a site that failed at the provider level (`GENRE FAIL`) is left untouched,
    because its rows were never measured
  * sites whose rows all disappear are removed from the map
"""
import os
import re
import sys
from collections import defaultdict

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SITES = os.path.join(ROOT, "AIO-Hentai/src/main/kotlin/com/aiohentai/cs/sites")
GENRES = os.path.join(ROOT, "AIO-Hentai/src/main/kotlin/com/aiohentai/cs/compat/Genres.kt")

HEADER = """package com.aiohentai.cs.compat

/**
 * Genre/category browse sections per source, discovered from each site's own
 * navigation by scripts/gen_genres.py. Rows are static because CloudStream reads
 * `mainPage` synchronously when the provider is built.
 */
object Genres {

    val bySite: Map<String, List<Pair<String, String>>> = mapOf(
"""

ROW_RE = re.compile(r"^\s+\"([a-z0-9]+)\" to listOf\((.*)\),?\s*$")
PAIR_RE = re.compile(r'"([^"]*)" to "([^"]*)"')
LOG_RE = re.compile(r"^\[([^\]]+)\] genre '([^']*)' \(([^)]*)\): (\d+)")
FAIL_RE = re.compile(r"^\[([^\]]+)\] GENRE FAIL")


def class_to_site():
    mapping = {}
    for site in sorted(os.listdir(SITES)):
        directory = os.path.join(SITES, site)
        if not os.path.isdir(directory):
            continue
        for name in os.listdir(directory):
            if not name.endswith(".kt") or name.endswith("Provider.kt"):
                continue
            text = open(os.path.join(directory, name), encoding="utf-8").read()
            match = re.search(r"^class (\w+) : AnimeHttpSource\(\)", text, re.M)
            if match:
                mapping[match.group(1)] = site
                break
    return mapping


def read_rows():
    rows = {}
    for line in open(GENRES, encoding="utf-8"):
        match = ROW_RE.match(line)
        if match:
            rows[match.group(1)] = PAIR_RE.findall(match.group(2))
    return rows


def read_log(path):
    counts = {}
    failed = set()
    for line in open(path, encoding="utf-8", errors="replace"):
        fail = FAIL_RE.match(line)
        if fail:
            failed.add(fail.group(1))
            continue
        match = LOG_RE.match(line)
        if match:
            site, name, row_path, count = match.groups()
            counts[(site, name, row_path)] = int(count)
    return counts, failed


def main():
    args = [a for a in sys.argv[1:]]
    dry_run = "--dry-run" in args
    args = [a for a in args if not a.startswith("--")]
    log_path = args[0] if args else "/tmp/genres.log"

    rows = read_rows()
    counts, failed = read_log(log_path)
    classes = class_to_site()

    removed = kept = unverified = 0
    lines = []
    for site in sorted(rows):
        kept_rows = []
        for name, row_path in rows[site]:
            cls = next((c for c, s in classes.items() if s == site), None)
            measured = counts.get((cls, name, row_path)) if cls else None
            if measured == 0 and cls not in failed:
                removed += 1
                print(f"remove  {site:18} {name:28} {row_path}")
                continue
            if measured is None and cls in failed:
                unverified += 1
            else:
                kept += 1
            kept_rows.append((name, row_path))
        if kept_rows:
            pairs = ", ".join(f'"{name}" to "{row_path}"' for name, row_path in kept_rows)
            lines.append(f'        "{site}" to listOf({pairs}),')

    if not lines:
        lines.append("        // no verified rows yet")

    output = HEADER + "\n".join(lines) + "\n    )\n}\n"

    print(
        f"\nrows kept={kept} removed={removed} unverified={unverified} "
        f"sites={len(rows)} -> {len(lines)}"
    )
    if dry_run:
        print("dry run; Genres.kt unchanged")
        return 0

    with open(GENRES, "w", encoding="utf-8") as fh:
        fh.write(output)
    print(f"wrote {GENRES}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
