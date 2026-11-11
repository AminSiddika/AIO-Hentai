# Animetail Extensions (CloudStream)

A CloudStream 4 plugin that exposes the 69 sources from the
[Animetail Aniyomi extension suite](https://github.com/AminSiddika/animetail-extensions)
as CloudStream providers. One `.cs3`, one install, 69 sources.

All sources are adult (NSFW) English video sites.

## Install

1. CloudStream → **Settings → Extensions → Add repository**.
2. Paste the repository URL:

```
https://raw.githubusercontent.com/AminSiddika/animetail-cloudstream/builds/plugins.json
```

3. Install **Animetail** from the repository, then pick any of the sources in
   **Home → Anime/Movie**.

`repo.json` at the repository root is the same repository in the short-code format
(`Add repository` → paste `repo.json` URL).

## How it is built

The Aniyomi extensions were not rewritten. Each scraper is compiled as-is against a
small compatibility layer (`com.animetail.cs.compat`) that reproduces the pieces of the
Aniyomi extension API they use:

| Aniyomi type | Compat replacement |
|---|---|
| `AnimeHttpSource` | `compat.AnimeHttpSource` (same request/parse pair methods) |
| `SAnime`, `SEpisode`, `Video`, `AnimesPage` | `compat.Model.kt` (same field names) |
| `GET()`, `asJsoup()` | `compat.Http.kt` |
| `MediaVideo` | `compat.MediaVideo.kt` |
| `AnimeFilter(List)` | `compat.Model.kt` |

`compat.AniyomiProvider` maps them onto CloudStream's `MainAPI`:

- `getMainPage` → `popularAnime` / `latestUpdates`
- `search` → `searchAnime`
- `load` → `animeDetailsParse` + `episodeListParse`; a page with one episode becomes a
  `Movie`, a page with several becomes a `TvSeries`
- `loadLinks` → `videoListParse`, emitted as `ExtractorLink` with the site `Referer`

That means site selectors, player deobfuscation, WordPress/Cloudflare handling and the
local NanoHTTPD proxies stay exactly as they were debugged in the Aniyomi extensions.

### Differences from the Aniyomi build

- `MediaVideo.referer` was a global per-APK; here each provider sets it before parsing
  videos, so 69 sources in one plugin do not leak referers into each other.
- Several sources start a localhost NanoHTTPD proxy for range requests; that still works
  because the plugin runs inside the CloudStream process.
- Chromecast support is disabled per provider, since localhost proxy links cannot be cast.
- Cloudflare challenges are handled by CloudStream itself, not by the Aniyomi extension's
  WebView interceptor. If a site shows a challenge, open the site once in CloudStream's
  built-in browser.

## Sources

Generated from `Animetail/src/main/kotlin/com/animetail/cs/sites` — see
`SOURCES.md` for the full list with base URLs.

## Build

Local typecheck (no Android SDK needed):

```bash
./scripts/typecheck.sh      # compiles against CloudStream's classes.jar + a Base64 stub
```

Full plugin build (needs JDK 17 and the Android SDK):

```bash
./gradlew make makePluginsJson
# -> Animetail/build/Animetail.cs3 and build/plugins.json
```

CI (`.github/workflows/build.yml`) builds on every push to `main` and force-pushes the
`.cs3`, `plugins.json` and icon to the `builds` branch, which is what the app reads.

Publishing requires a `builds` branch to exist before the first run:

```bash
git checkout --orphan builds
git rm -rf .
echo "# build artifacts" > README.md
git add README.md && git commit -m "init builds"
git push -u origin builds
```

## Smoke tests

The scrapers can be exercised on the JVM without an Android device:

```bash
./scripts/typecheck.sh
./scripts/smoke.sh WatchHentai HentaiCity      # home, details, episodes
./scripts/smoke.sh --links HentaiCity          # also resolve video links
```

## Layout

```
Animetail/src/main/kotlin/com/animetail/cs/
  AnimetailPlugin.kt          plugin entry, registers 69 providers
  compat/                     Aniyomi-API compatibility layer
  sites/<site>/<Source>.kt    the ported scraper (unchanged site logic)
  sites/<site>/<Source>Provider.kt
scripts/port.py               re-import a scraper from ../animetail-extensions
```

After changing a source in the Aniyomi repo, re-run `python3 scripts/port.py` and rebuild.
