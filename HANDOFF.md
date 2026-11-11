# AIO-Hentai — Engineering Handoff

Published: **v7** (`70600d7`); current working tree: **v8** (not pushed).
Upstream: <https://github.com/AminSiddika/AIO-Hentai>

A CloudStream 4 plugin that ships **73 adult (NSFW) video providers** in one `.cs3`.
Most scrapers are ported from the Aniyomi suite
[`AminSiddika/animetail-extensions`](https://github.com/AminSiddika/animetail-extensions);
MissAV, SupJav and SexTB are native scrapers. The rest is a small compatibility layer
plus one adapter per source.

> **Publishing note:** v3 is live on `builds` (`plugins.json` version `3`, 249,721-byte
> `.cs3`). v7 fixed lazy thumbnails. v8 adds 91Porn, AsianViral, JavTsunami, Manko and
> AVPorner; its local build is 287,238 bytes and has not been pushed yet.

---

## 1. Current state

| Item | Value |
|---|---|
| Plugin version | `8` (`AIO-Hentai/build.gradle.kts`) |
| Providers registered | 73 |
| Kotlin files | 155 (73 scrapers + 73 provider wrappers + 8 compat + 1 plugin) |
| Genre rows | 37 sites, 184 verified rows |
| JAV/NSFW sources | MissAV, SupJav, SexTB, JavGG, JavGuru, 91Porn, AsianViral, JavTsunami, Manko, AVPorner (`TvType.NSFW`) |
| Removed in v3 | H-Anime, Hentai-JP, HentaiCloud, Hentaigasm, UnderHentai |
| v3 sweep | 62/64 passed end to end; EroticHentai and XAnimePorn pass on retry |
| v8 check | typecheck + clean build pass; P91, AsianViral, JavTsunami and Manko verified live; AVPorner parser verified (CF blocks plain JVM) |
| Published artifact | `builds`: v3, 249,721-byte `.cs3`, hash `sha256-b687eb44…b2d583` |
| Local build | **success** — 281 KB `.cs3` (287,238 bytes), zero compiler warnings |
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

- **37 of 68 sites** expose rows
- **184 rows total**: 93 `/genre/`, 35 `/category/`, 24 `/tag/`, 10 `/genres/`,
  8 `/en/genres/` (MissAV), 6 `/rule34/`, 5 `/hentai/`, 3 `/jav-*` (SexTB)
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

### 3.7 MissAV (v4)

MissAV is JAV, not anime, so `MissavProvider` advertises only
`TvType.NSFW` + `TvType.Movie` and sets `preferredType = TvType.NSFW`. That keeps it in
the adult/JAV area instead of Home -> Anime.

- Base URL: `https://missav.ws` (overridable)
- Browse: `/en/new` (latest), `/en/today-hot` (popular), `/en/search/<query>` (search),
  `/en/genres/<name>?page=N` (8 static rows)
- Cards: `div.thumbnail.group` -> `a[alt][href]`, lazy `img[data-src]`
- Player: packed JS (`eval(function(p,a,c,k,e,d){...})`) expands to `source` (master),
  `source842` (720p) and `source1280` (1080p) m3u8 URLs on `surrit.com`
- Cloudflare: `appHttpClient()` adds `CloudflareKiller` when `AppContext.value` is set
  (in the app). A plain JVM test uses the normal client and sees the challenge.

Verified from browser-captured live HTML:

```
list:    2 cards, next=true
detail:  title, poster, description, genres (VR filtered)
episodes: 1 (Full Video)
videos:  3 -> master HLS + 720p + 1080p
streams: HTTP 200 with Referer https://missav.ws/ (playlist bodies checked)
```

`provider_smoke.sh Missav` on the JVM reports `empty home page` because the Cloudflare
challenge cannot be solved outside Android. Confirm in the app after opening MissAV once
in CloudStream's browser.

---

### 3.8 SupJav, SexTB and JavGuru (v5)

All four JAV sources use `TvType.NSFW` + `Movie` with `preferredType = TvType.NSFW`, so
they stay out of Home -> Anime.

**JavGuru** keeps its existing scraper; only its provider type changed to NSFW/Movie.

**SupJav** (`https://supjav.com`)

- Browse: `/popular` (popular), `/` (latest), `/?s=<query>` (search),
  `/category/<name>` (6 genre rows); pagination `/page/N`.
- Cards: `div.post` with `h3 a` and `img.thumb`.
- Player: each `a.btn-server[data-link]` goes through
  `lk1.supremejav.com/supjav.php` -> reversed token -> embed host. The embed's packed JS
  expands to `hls4`/`hls3`/`hls2`; the player prefers `hls4`, which was returned as a
  working HLS master. Verified end to end from a saved detail page.
- Cloudflare on `supjav.com`; `lk1`/embed hosts are reachable from the JVM.

**SexTB** (`https://sextb.net`)

- Browse: `/jav-censored` (latest), `/jav-uncensored` (popular), `/search/<slug>` (search),
  three genre rows; pagination `/pg-N`.
- Cards: `.movie-card` -> `.tray-item-title`, `img.tray-item-thumbnail`.
- Player: `POST /ajax/player` with `episode`/`filmId`/`pt` and a Basic
  `_token:_socket` header, then XOR-decrypt `player_enc` with `__pk`. The decrypted HTML
  contains either a direct MP4 or the f4scdn iframe; the iframe's `/api/play/<id>`
  (`ab=0&n=1`) returns the HLS `src`.
- The detail page also has a direct `<video>`/MP4 fallback for entries where the AJAX
  player is unavailable. Both paths were verified live; the HLS playlist returned 200.
- Cloudflare on `sextb.net`; `appHttpClient()` handles it in-app.

---

### 3.9 JavGG (v6)

**JavGG** (`https://javgg.net`) is free and typed `TvType.NSFW` + `Movie`.

- Browse: `/new-post/` (latest), `/trending/` (popular), `/tag/...` (5 verified rows);
  pagination `/page/N/`; search uses the site page plus the Dooplay search API with the
  page nonce.
- Cards: `article.item.movies` -> `.titlecontent`, `h3 a`, lazy `img[data-src]`.
- Player: prefers the `f4jav.xyz/e/...` iframe (`data-api` -> `/api/play/...` HLS);
  falls back to the VidHide-style `earnvidjavgg.xyz/embed/...` packed JS (`hls4`).
- Live check: home 40, search 6, load MovieLoadResponse, links 1 HLS; five tag rows
  returned 40 each.

AvJiali was checked and **not added**: its public pages expose only paid-trailer MP4s.

---

### 3.10 v8 batch

- **91Porn**: `index.php` listing, `view_video.php` details; the real `<source>` is
  inside a comment and written with `strencode2("%3c...")`, so the parser percent-decodes
  that payload. Verified 1 MP4.
- **AsianViral**: `article.loop-video`, search `?s=`, PlayMogo iframe resolved via
  `pass_md5` + token. Verified 1 MP4.
- **JavTsunami**: `article.video-card`; `data-server-token` POSTed to
  `/wp-json/jw-dom-tabs/v1/resolve`, then the returned player's packed JS expands to
  hls4/hls2. Verified 1 HLS.
- **Manko**: SPA + `healertanker.com` API. The API `data` field is a custom base-91
  packer; decoding it yields list/detail JSON. Detail exposes `wasabi_url_1` and
  `missav_video`. Verified 2 HLS links.
- **AVPorner**: `.video-list` cards; detail `jwplayer_config` exposes MP4 sources with
  labels. Parser verified with a fixture; live listing needs Cloudflare clearance.

Skipped: **AvJiali** (paywall/trailers), **herexxx** (embed says video not allowed),
**Ikisoda** (public file URLs 404; player iframe absent from server HTML).

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

v4 MissAV check (parser fixtures built from the live browser DOM):

```
[Missav] list items=2 next=true
[Missav] detail title/poster/description/genres OK
[Missav] videos=3 -> HLS + 720p + 1080p
[Missav] provider_smoke on JVM -> empty home page (Cloudflare; expected)
```

v5 JAV checks:

```
[Sextb] home 29, search 12, load MovieLoadResponse, links 1 HLS (HTTP 200)
[Sextb] genres: Censored 29, Uncensored 29, Amateur 29
[Supjav] player chain from saved detail: 1 HLS master (HTTP 200)
[JavGuru] load MovieLoadResponse, links 1 HLS
[Supjav] provider_smoke on JVM -> empty home (Cloudflare; expected)
```

v6 JavGG check:

```
[Javgg] home 40, search 6, load MovieLoadResponse, links 1 HLS
[Javgg] genres: English Subtitle 40, Uncensored Leak 40, Reduce Mosaic 40, Censored 40, Chinese Subtitle 40
```

v8 checks:

```
[P91]         home 24, search 24, links 1 MP4
[Asianviral]  home 30, search 2, links 1 MP4
[Javtsunami]  home 32, search 1, links 1 HLS
[Manko]       home 24, search 1, links 2 HLS
[Avporner]    parser fixture -> 2 MP4s; plain JVM home empty (Cloudflare)
```

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
| MissAV | Cloudflare managed challenge; `CloudflareKiller` retries in-app, plain JVM gets 403 |
| SupJav | Cloudflare on the listing/detail hosts; player chain (lk1 + embed) works from the JVM |
| SexTB | player is an AJAX + XOR response; direct-MP4 fallback covers entries without it |
| JavGG | Cloudflare on the listing/detail host; f4jav + VidHide player hosts work from the JVM |
| AvJiali | not added: paywalled, only trailer MP4s are public |
| 91Porn | real source is percent-encoded inside a comment; handled by `strencode2` decode |
| Manko | encrypted API; decoder ported from build.js |
| AVPorner | Cloudflare on listing/detail; parser verified, needs in-app clearance |
| herexxx / Ikisoda | skipped: paywalled / public file URLs return 404 |

Aki-H was confirmed in v3: home filters `/custom-udon/`, load returns 20 episodes and
3 HLS renditions. AnimeIDHentai/Hentai.TV unescape fixes remain local to this port.

---

## 9. Open items

1. v7 is live; v8 is local only. Commit/push v8 after the new-source device checks.
2. Confirm MissAV, SupJav, SexTB, JavGG, 91Porn, AsianViral, JavTsunami, Manko and
   AVPorner in the app (NSFW/JAV section, not Anime).
3. Confirm JavGuru now appears only in NSFW/JAV, not Anime.
4. Confirm Aki-H, Hentai2w, Hentaverse and EroticHentai playback on a device.
5. Re-run `provider_smoke.sh --genres` after the next network change; add rows only when
   a listing returns items.
6. 31 sites still have no browse rows (no static taxonomy).
7. Apply the parser/header fixes upstream in `animetail-extensions` so `port.py`
   does not reintroduce them.
8. Decide whether the dormant `MediaVideo` worker proxy should be removed.

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
- **Cloudflare**: `CloudflareKiller` is available in `classes.jar`; add it as an interceptor on a per-source client, not globally (JVM tests have no WebView).
- **Docs-only commits** do not trigger CI (`paths-ignore: *.md`).
- **`port.py` overwrites** scraper files; keep local fixes in a patch or upstream them.

---

## 11. Quick verification

```bash
grep -c 'registerMainAPI' AIO-Hentai/src/main/kotlin/com/aiohentai/cs/AIOHentaiPlugin.kt   # 73
find AIO-Hentai/src -name '*.kt' | wc -l                                                  # 155
grep -c ' to listOf(' AIO-Hentai/src/main/kotlin/com/aiohentai/cs/compat/Genres.kt       # 37
grep -o '" to "/' AIO-Hentai/src/main/kotlin/com/aiohentai/cs/compat/Genres.kt | wc -l    # 184
```

If those counts drift, update this file and `SOURCES.md` in the same commit.
