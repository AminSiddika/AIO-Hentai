# AIO-Hentai (CloudStream)

A CloudStream 4 plugin that exposes the 69 sources from the
[Animetail Aniyomi extension suite](https://github.com/AminSiddika/animetail-extensions)
as CloudStream providers. One `.cs3`, one install, 69 sources.

All sources are adult (NSFW) English video sites.

## Install

CloudStream → **Settings → Extensions → Add repository**. Use one of:

```
https://raw.githubusercontent.com/AminSiddika/AIO-Hentai/main/repo.json
https://cdn.jsdelivr.net/gh/AminSiddika/AIO-Hentai@main/repo.json
```

Both point at the same repository (`repo.json` is the manifest; `pluginLists` in it points
to `plugins.json` on the `builds` branch). If GitHub raw is slow or blocked on your
network, use the jsDelivr one, or enable **Use jsDelivr proxy** in CloudStream settings so
it rewrites `raw.githubusercontent.com` links itself.

Opening the URL in a phone browser must show JSON. If it shows `404: Not Found`, the URL
is wrong — that is the only way CloudStream produces the "Invalid URL" / JSON parse error.

Then install **AIO-Hentai** from the repository and pick a source under
**Home → Anime / Movie**.

## How it is built

The Aniyomi extensions were not rewritten. Each scraper is compiled as-is against a
small compatibility layer (`com.aiohentai.cs.compat`) that reproduces the pieces of the
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

Generated from `AIO-Hentai/src/main/kotlin/com/aiohentai/cs/sites` — see
`SOURCES.md` for the full list with base URLs.

## Build

Local typecheck (no Android SDK needed):

```bash
./scripts/typecheck.sh      # compiles against CloudStream's classes.jar + a Base64 stub
```

Full plugin build (needs JDK 17 and the Android SDK):

```bash
./gradlew make makePluginsJson
# -> AIO-Hentai/build/AIO-Hentai.cs3 and build/plugins.json
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

## Tests

`scripts/setup_env.sh` downloads CloudStream's `classes.jar` and the runtime libs into
`~/.cache/cs3`, so everything below runs on a plain JVM — no Android SDK, no device.

```bash
./scripts/typecheck.sh                        # all 138 files against the real CS API
./scripts/smoke.sh WatchHentai HentaiCity     # scraper level: home, details, episodes
./scripts/smoke.sh --links HentaiCity         # also resolve video links
./scripts/provider_smoke.sh WatchHentai       # adapter level: getMainPage/search/load/loadLinks
```

What the adapter test proves end to end (WatchHentai, HentaiCity, Hentai0):

```
[Hentai0]    load: MovieLoadResponse  -> links: 5  (240p/360p/480p MP4s)
[HentaiCity] load: MovieLoadResponse  -> links: 2  (MP4 + HLS)
[WatchHentai] load: TvSeriesLoadResponse -> links: 2 (localhost proxy + direct)
```

## Layout

```
AIO-Hentai/src/main/kotlin/com/aiohentai/cs/
  AIOHentaiPlugin.kt          plugin entry, registers 69 providers
  compat/                     Aniyomi-API compatibility layer
  sites/<site>/<Source>.kt    the ported scraper (unchanged site logic)
  sites/<site>/<Source>Provider.kt
scripts/port.py               re-import a scraper from ../animetail-extensions
```

After changing a source in the Aniyomi repo, re-run `python3 scripts/port.py` and rebuild.
