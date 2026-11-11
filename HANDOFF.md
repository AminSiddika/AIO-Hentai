# AIO-Hentai — Engineering Handoff

Snapshot: **v3 working tree**, based on upstream commit `75d07d3`.
Upstream: <https://github.com/AminSiddika/AIO-Hentai>

A CloudStream 4 plugin that ships **64 adult (NSFW) video providers** in one `.cs3`.
The scrapers are ported from the Aniyomi suite
[`AminSiddika/animetail-extensions`](https://github.com/AminSiddika/animetail-extensions);
the only new code is a small compatibility layer plus one adapter per source.

> **Publishing note:** `main`/`builds` still serve **v2** (69 sources). This working tree
> is **v3** (64 sources) and has not been pushed. CI will publish v3 after the version
> bump and push are committed.

---

## 1. Current state

| Item | Value |
|---|---|
| Plugin version | `3` (`AIO-Hentai/build.gradle.kts`) |
| Providers registered | 64 |
| Kotlin files | 135 (64 scrapers + 64 provider wrappers + 6 compat + 1 plugin) |
| Genre rows | 33 sites, 162 verified rows |
| Removed in v3 | H-Anime, Hentai-JP, HentaiCloud, Hentaigasm, UnderHentai |
| Final sweep | 62/64 passed end to end; 2 slow/throttled (EroticHentai, XAnimePorn), both pass on retry |
| Local build | **success** — 244 KB `.cs3` (249,721 bytes), zero compiler warnings |
| Local build requirement | JDK 17+ and Android SDK 36 |
| Test requirement | JDK + `kotlinc` + network; **no** Android SDK |

Install URLs (unchanged):

```
https://raw.githubusercontent.com/AminSiddika/AIO-Hentai/main/repo.json
https://cdn.jsdelivr.net/gh/AminSiddika/AIO-Hentai@main/repo.json
```

`repo.json` is a manifest; its `pluginLists` entry points at `builds/plugins.json`,
which carries the real `.cs3` URL, version and SHA-256 hash.

---

## 2. Repository map

```
AIO-Hentai/
  build.gradle.kts              plugin metadata + cloudstream block
  src/main/AndroidManifest.xml  empty <manifest />
  src/main/kotlin/com/aiohentai/cs/
    AIOHentaiPlugin.kt          @CloudstreamPlugin, registers all 64 providers
    compat/
      AnimeHttpSource.kt        Aniyomi source contract + fetch/parse helpers + genre URL rules
      AniyomiProvider.kt        MainAPI adapter — the only real CloudStream-side logic
      Model.kt                  SAnime / SEpisode / Video / AnimesPage / AnimeFilter*
      Http.kt                   OkHttp client, cookie jar, GET(), asJsoup(), replayable bodies
      MediaVideo.kt             playback wrapper (UA + Referer/Origin, optional egress proxy)
      Genres.kt                 GENERATED static genre/category rows per source
    sites/<package>/<Source>.kt          ported scraper (site logic unchanged)
    sites/<package>/<Source>Provider.kt  one-line wrapper: AniyomiProvider(<Source>())
scripts/                        port, typecheck, live smoke tests, generators, genre prune
scripts/android-stub/           desktop stubs for android.content.Context / android.util.Base64
.github/workflows/build.yml     builds and force-publishes to the builds branch
```

Every site provider is exactly this shape:

```kotlin
class WatchHentaiProvider : AniyomiProvider(WatchHentai())
```

If an adapter rule needs to change, change `compat/AniyomiProvider.kt` once.

---

## 3. Architecture

### 3.1 Compatibility layer

The port keeps Aniyomi's class and field names, so the scrapers' selectors and player
decoders did not need rewriting.

| Aniyomi API | Compat replacement |
|---|---|
| `AnimeHttpSource` | `compat.AnimeHttpSource` (request/parse pair contract) |
| `SAnime`, `SEpisode`, `AnimesPage`, `Video` | `compat.Model.kt` (same field names) |
| `AnimeFilter`, `AnimeFilterList` | `compat.Model.kt` |
| `GET()`, `asJsoup()` | `compat.Http.kt` |
| `MediaVideo` | `compat.MediaVideo.kt` |

### 3.2 Scraper contract

`AnimeHttpSource` exposes the methods the scrapers already implement:

- browsing: `popularAnimeRequest/Parse`, `latestUpdatesRequest/Parse`,
  `searchAnimeRequest/Parse`
- details: `animeDetailsParse`, `episodeListParse`
- playback: `videoListRequest`, `videoListParse`
- filters: `getFilterList`

Additional helpers supplied by the compat layer:

- `fetch(request|url)` returns a buffered `FetchedPage` whose body can be replayed.
  Several scrapers call `response.asJsoup()` more than once.
- `details(page)`, `episodes(page)`, `videos(page)`, `popularAnime(page)`, etc.
- `genres` is read from `Genres.bySite[<package>]`.
- `genrePageUrl(path, page)` defaults to WordPress-style `path/page/N/`, or `&page=N`
  when the path already has a query string.

### 3.3 CloudStream adapter (`AniyomiProvider`)

| CloudStream | Source call |
|---|---|
| `getMainPage(page, request)` | `popularAnime` for `"popular"`, `latestUpdates` for `"latest"`, list parse for `genre\|<path>` |
| `search(query, page)` | `searchAnime` |
| `load(url)` | `animeDetailsParse` + `episodeListParse` |
| `loadLinks(data, …)` | `videoListParse` → `ExtractorLink` |

Provider flags:

- `canBeOverridden = true` — users can change each source's base URL.
- `hasChromecastSupport = false` — localhost proxies and hotlink-protected URLs do not cast.
- `supportedTypes = Movie, TvSeries, Anime, AsianDrama`.
- `hasQuickSearch = false`; `hasMainPage = true`.

Adapter rules:

1. **0 episodes** → `ErrorLoadingException("No episodes available")`.
2. **1 episode** → `MovieLoadResponse`, with `dataUrl` set to the episode URL.
3. **>1 episodes** → `TvSeriesLoadResponse`; season `1`, episode numbers only when `> 0`.
4. **HLS detection**: link is `ExtractorLinkType.M3U8` when the URL contains `.m3u8`
   **or** the quality label contains `hls` (Aki-H serves an extension-less master).
5. **Referer isolation**: `prepare()` sets `MediaVideo.referer` and `useProxy` from the
   source before every home/search/load/links call.
6. **Link dedupe**: repeated and blank URLs are dropped.

`mainPage` is `Popular`, `Latest`, and every `genre|<path>` row except duplicates of
those names. Rows are static because CloudStream reads `mainPage` synchronously.

### 3.4 HTTP

`compat/Http.kt` owns one lazy `OkHttpClient`:

- Chrome 124 desktop UA and browser-like `Accept`/`Accept-Language`
- in-memory cookie jar keyed by host
- 30 s connect/read/write, 60 s call timeout, redirects followed
- interceptor adds `Accept: */*` + `Accept-Language` only when a raw request omitted them
- `asJsoup()` strips a UTF-8 BOM and uses the response URL as base URI
- `FetchedPage.response()` re-wraps an in-memory `ReplayableBody`

Aki-H and EroticHentai both needed site-specific header workarounds: Aki-H's stream host
returns `null` without `Accept`, and EroticHentai's API stalls when gzip or a pooled
connection is used (`Accept-Encoding: identity` + `Connection: close`).

### 3.5 Playback (`MediaVideo`)

`MediaVideo.create(url, quality, videoUrl?, headers?)` sets a browser UA, `Referer`,
derived `Origin`, and keeps scraper headers. If `useProxy` is true and the target is not
`.mpd`, it rewrites through `https://animextension.mobiusdie.workers.dev/api/proxy`.

No scraper sets `mediaUseProxy = true`, so the worker proxy is dormant. The only
in-process proxy is `WatchHentai.VideoProxy` (NanoHTTPD, Range-aware).

### 3.6 Genre rows (`Genres.kt`)

Generated by `scripts/gen_genres.py`, verified with `provider_smoke.sh --genres`, and
pruned by `scripts/prune_genres.py`.

- **33 of 64 sites** expose rows
- **162 rows total**: 93 `/genre/`, 29 `/category/`, 19 `/tag/`, 10 `/genres/`,
  6 `/rule34/`, 5 `/hentai/`
- **31 sites** have no static taxonomy (JS menus, no browse pages, tags only in search)

v3 fixes that made previously empty rows work:

| Source | Fix |
|---|---|
| HentaiSea | accept `/watch/` cards on genre pages |
| HentaiYoga | accept `/watch/` cards on category pages |
| MioHentai | accept `a.tag-card-link` on tag pages |
| HentaiHavenXxx | accept `/rule34/video/` cards and fall back to a single episode |
| Nekopoi | accept category episode posts and fall back to a single episode |
| HentaiVideos | genre pages are unprefixed A-Z indexes, so its 8 rows were pruned |

Rows that returned zero and were removed: 15 (8 HentaiVideos, 4 empty Nekopoi
categories, 1 HAnime download row, 1 HentaiHorizon trailing-space path, 1
HentaiTubeOnline empty 3D path).

---

## 4. Sources removed in v3

Deleted directories, registrations and genre rows:

| Source | Package | Why |
|---|---|---|
| H-Anime | `hanime` | Doodstream embeds removed; `doodst.com` returns “Video is Unavailable” |
| Hentai-JP | `hentaijp` | both `serverData` mirrors unavailable |
| HentaiCloud | `hentaicloud` | origin unreachable (no route to host) |
| Hentaigasm | `hentaigasm` | domain no longer resolves |
| UnderHentai | `underhentai` | Cloudflare timeout/403 for non-browser clients on every run |

The remaining slow-but-working sources were kept: EroticHentai, HentaiStream,
XAnimePorn, Hentai0.

---

## 5. Adding or re-importing a source

```bash
python3 scripts/port.py
```

`port.py` copies each upstream module's `class X : AnimeHttpSource()` file, rewrites
imports, writes `sites/<site>/X.kt`, and generates `XProvider.kt`.

Caveats:

- `SRC` defaults to `../animetail-extensions`; override with `ANIMETAIL_EXTENSIONS`.
- It **overwrites** `sites/<site>/<Source>.kt`; upstream the local fixes or re-apply them.
- Upstream class names must match exactly; duplicate class names are reported.

Manual checklist:

1. Add `sites/<package>/<Source>.kt` and `<Source>Provider.kt`.
2. Register in `AIOHentaiPlugin.load()`.
3. Regenerate `scripts/SmokeSites.kt` (`python3 scripts/gen_smoke.py`).
4. Optionally add `Genres.bySite["<package>"]` and verify it.
5. Update `SOURCES.md`.
6. Bump `version` in `AIO-Hentai/build.gradle.kts`.
7. Typecheck and smoke it.

---

## 6. Build

```bash
./gradlew make makePluginsJson
# -> AIO-Hentai/build/AIO-Hentai.cs3
# -> build/plugins.json
```

Toolchain: Gradle 9.3.1, AGP 9.1.0, Kotlin 2.3.20, `compileSdk 36`, `minSdk 21`,
JVM target 1.8. Runtime libs: jsoup 1.22.2, OkHttp 4.12.0, NanoHTTPD 2.3.1,
coroutines 1.10.2; `org.json` is `compileOnly`.

CI (`.github/workflows/build.yml`) runs on push to `main`/`master` except Markdown-only
changes, builds with JDK 17 + Android 36, and force-pushes artifacts to `builds`:
`AIO-Hentai.cs3`, `plugins.json`, `repo.json`, `repo-jsdelivr.json`, icon.

Release: bump `version`, commit, push. CloudStream compares `version` in `plugins.json`.

---

## 7. Tests (JVM, no device)

```bash
./scripts/setup_env.sh                 # CloudStream classes.jar + libs -> ~/.cache/cs3
./scripts/typecheck.sh                 # compile all 135 .kt files
./scripts/smoke.sh WatchHentai         # scraper level
./scripts/provider_smoke.sh WatchHentai HentaiCity Hentai0
./scripts/provider_smoke.sh AkiH --url <seriesUrl>
./scripts/provider_smoke.sh --genres   # every row with item counts
python3 scripts/prune_genres.py /tmp/genres.log [--dry-run]
```

The scripts now run outside Termux:

- `scripts/android-stub/` supplies `android.content.Context` and a real
  `android.util.Base64` implementation.
- `port.py`, `gen_smoke.py`, `gen_genres.py`, `typecheck.sh`, `smoke.sh` and
  `provider_smoke.sh` derive paths from the repo; `KOTLINC` is overridable.
- `setup_env.sh` falls back to Python when curl cannot resolve DNS.
- `gen_genres.py` scans the sources when the old audit TSV is absent.
- `prune_genres.py` removes rows that returned 0 items and leaves failed sites alone.

To run locally on macOS: install JDK 17+ and a Kotlin compiler (2.3.20 was used here),
then export `JAVA_HOME`/`PATH` and run the commands above. The CloudStream `classes.jar`
and all Maven libs are downloaded into `~/.cache/cs3`.

### v3 results

```
provider summary: ok=62 failed=2 total=64
EroticHentai  transient: aniporn.com API throttles repeated requests (passes on retry)
XAnimePorn    slow: times out under full-sweep concurrency (passes when run alone)
```

Earlier individual retries: EroticHentai → 1 link, Hentai0 → 5 links,
HentaiStream → 1 link, XAnimePorn → 4 links.

Canonical adapter check:

```
[Hentai0]     MovieLoadResponse    -> 5 links (240p/360p/480p/720p/1080p)
[HentaiCity]  MovieLoadResponse    -> 2 links (MP4 + HLS)
[WatchHentai] TvSeriesLoadResponse -> 2 links (localhost proxy + direct)
[AkiH]        TvSeriesLoadResponse -> 3 HLS renditions (1080p/720p/360p)
```

---

## 8. Known issues

| Source | Status |
|---|---|
| EroticHentai | API host throttles repeated calls; first request after a pause succeeds |
| HentaiStream | slow first request; passed in the final sweep |
| Hentai0 | occasional read timeout under load; passed in the final sweep |
| XAnimePorn | slow; times out under full-sweep concurrency, returns 4 quality links when run alone |
| MuchoHentai, Ohentai, hanime.tv, Hentaiking | Cloudflare-fronted; first request can be slow |
| WatchHentai | in-process NanoHTTPD proxy; Chromecast disabled by design |

Aki-H was confirmed in v3: home filters `/custom-udon/`, load returns 20 episodes and
3 HLS renditions. AnimeIDHentai/Hentai.TV unescape fixes remain local to this port.

---

## 9. Open items

1. Commit and push v3; confirm CI publishes `"version": 3` to `builds/plugins.json`
   (the local build already produces a 249,721-byte `.cs3` with the expected hash).
2. Confirm Aki-H, Hentai2w, Hentaverse and EroticHentai playback on a device.
3. Re-run `provider_smoke.sh --genres` after the next network change; add rows only when
   a listing returns items.
4. 31 sites still have no browse rows (no static taxonomy).
5. Apply the parser/header fixes upstream in `animetail-extensions` so `port.py`
   does not reintroduce them.
6. Decide whether the dormant `MediaVideo` worker proxy should be removed.

---

## 10. Gotchas

- **Invalid repository URL**: CloudStream reports a JSON parse error when the URL 404s
  (`404: Not Found`, 14 bytes). That always means a wrong/stale URL.
- **jsDelivr mirror** exists for networks that block `raw.githubusercontent.com`.
- **Gradle Kotlin DSL**: inside `subprojects { dependencies { … } }`, fetch each
  configuration with `val X by configurations`.
- **Android setup action**: `setup-android@v3` fails on current runners; v4 plus
  `sdkmanager "platforms;android-36"` works.
- **jsoup 1.22** needs `org.jspecify:jspecify` on the compile classpath; `build.gradle.kts` now declares it (without it the build warns and Kotlin 2.4 will fail).
- **Replayable bodies**: scrapers call `asJsoup()` twice; keep `FetchedPage`.
- **Referer leakage**: never reintroduce a global `MediaVideo.referer`.
- **Redirect cookies**: read `Set-Cookie` from the 302 for Laravel age gates
  (Hentai2w); a following client hides it.
- **Gzip hang**: EroticHentai's API needs `Accept-Encoding: identity` + `Connection: close`.
- **Docs-only commits** do not trigger CI (`paths-ignore: *.md`).
- **`port.py` overwrites** scraper files; keep local fixes in a patch or upstream them.

---

## 11. Quick verification

```bash
grep -c 'registerMainAPI' AIO-Hentai/src/main/kotlin/com/aiohentai/cs/AIOHentaiPlugin.kt   # 64
find AIO-Hentai/src -name '*.kt' | wc -l                                                  # 135
grep -c ' to listOf(' AIO-Hentai/src/main/kotlin/com/aiohentai/cs/compat/Genres.kt       # 33
grep -o '" to "/' AIO-Hentai/src/main/kotlin/com/aiohentai/cs/compat/Genres.kt | wc -l    # 162
```

If those counts drift, update this file and `SOURCES.md` in the same commit.
