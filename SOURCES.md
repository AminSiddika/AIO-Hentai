# Sources

**64 providers** in plugin **v3**. All are ported from the Aniyomi suite
[`AminSiddika/animetail-extensions`](https://github.com/AminSiddika/animetail-extensions).
Browse names keep the upstream `@JalebiBae - ` prefix, omitted here for readability.
Every active source implements a latest section and is overridable in CloudStream settings.

- Package root: `com.aiohentai.cs.sites.<package>`
- Each entry has `<Source>.kt` (scraper) and `<Source>Provider.kt` (`AniyomiProvider(<Source>())`).
- "Browse rows" is the number of verified genre/category rows in `compat/Genres.kt` (33 sites, 162 rows; `—` = none).

| Browse name | Class | Package | Base URL | Browse rows |
|---|---|---|---|---|
| Aki-H | `AkiH` | `akih` | https://aki-h.com | — |
| AnimeIDHentai | `AnimeIdHentai` | `animeidhentai` | https://animeidhentai.com | 8 |
| AnimeSex | `AnimeSex` | `animesex` | https://animesex.club | 1 |
| CartoonPorn | `CartoonPorn` | `cartoonporn` | https://cartoonporn.pro | — |
| EroticHentai | `EroticHentai` | `erotichentai` | https://erotic-hentai.com | 1 |
| HanimeHentai | `HanimeHentai` | `hanimehentai` | https://hanimehentai.tv | — |
| Hanime.red | `HanimeRed` | `hanimered` | https://hanime.red | 6 |
| hanime.tv | `HanimeTv` | `hanimetv` | https://hanime.tv | — |
| HdHentai | `HdHentai` | `hdhentai` | https://hdhentai.tv | — |
| Hentai0 | `Hentai0` | `hentai0` | https://hentai0.com | — |
| Hentai222 | `Hentai222` | `hentai222` | https://hentai222.com | — |
| Hentai2W | `Hentai2w` | `hentai2w` | https://hentai2w.com | — |
| Hentai4K | `Hentai4k` | `hentai4k` | https://hentai4k.com | — |
| HentaiAstra | `HentaiAstra` | `hentaiastra` | https://hentaiastra.com | — |
| HentaiBros | `Hentaibros` | `hentaibros` | https://hentaibros.net | 1 |
| HentaiCity | `HentaiCity` | `hentaicity` | https://www.hentaicity.com | — |
| Hentai-For | `HentaiFor` | `hentaifor` | https://hentai-for.net | — |
| HentaiFox | `HentaiFox` | `hentaifox` | https://hentaifox.tv | — |
| HentaiFreak | `HentaiFreak` | `hentaifreak` | https://hentaifreak.org | 1 |
| HentaiGem | `HentaiGem` | `hentaigem` | https://www.hentaigem.com | — |
| HentaiHaven | `HentaiHaven` | `hentaihaven` | https://hentaihaven.icu | 8 |
| HentaiHaven.com | `HentaiHavenCom` | `hentaihavencom` | https://hentaihaven.com | 8 |
| HentaiHaven.red | `HentaiHavenRed` | `hentaihavenred` | https://hentaihaven.red | 5 |
| HentaiHaven.xxx | `HentaiHavenXxx` | `hentaihavenxxx` | https://hentaihaven.xxx | 6 |
| HentaiHD | `HentaiHD` | `hentaihd` | https://hentaihd.net | 1 |
| HentaiHorizon | `HentaiHorizon` | `hentaihorizon` | https://hentaihorizon.com | 2 |
| HentaiKing | `HentaiKing` | `hentaiking` | https://hentaiking.com | 8 |
| HentaiMama | `HentaiMama` | `hentaimama` | https://hentaimama.io | 8 |
| Hentai-Moon | `HentaiMoon` | `hentaimoon` | https://hentai-moon.com | — |
| HentaiOcean | `HentaiOcean` | `hentaiocean` | https://hentaiocean.com | 3 |
| HentaiPlay | `HentaiPlay` | `hentaiplay` | https://hentaiplay.net | 8 |
| HentaiSea | `HentaiSea` | `hentaisea` | https://hentaisea.com | 8 |
| HentaiSmile | `HentaiSmile` | `hentaismile` | https://www.hentaismile.com | — |
| HentaiStream | `HentaiStream` | `hentaistream` | https://tube.hentaistream.com | — |
| Hentaistream.tv | `HentaistreamTv` | `hentaistreamtv` | https://hentaistream.tv | 8 |
| Hentais.Tube | `HentaisTube` | `hentaistube` | https://www.hentais.tube | 1 |
| HentaiTube.online | `HentaiTubeOnline` | `hentaitubeonline` | https://www.hentaitube.online | 7 |
| Hentai.TV | `HentaiTv` | `hentaitv` | https://hentai.tv | 8 |
| HentaiVideos | `HentaiVideos` | `hentaivideos` | https://hentaivideos.net | — |
| HentaiYes | `HentaiYes` | `hentaiyes` | https://hentaiyes.com | 5 |
| Hentai.yoga | `HentaiYoga` | `hentaiyoga` | https://hentai.yoga | 8 |
| Hentaverse | `Hentaverse` | `hentaverse` | https://hentaverse.com | — |
| HStream | `HStream` | `hstream` | https://hstream.moe | — |
| Jav.Guru | `JavGuru` | `javguru` | https://jav.guru | 7 |
| MioHentai | `MioHentai` | `miohentai` | https://miohentai.com | 6 |
| MuchoHentai | `MuchoHentai` | `muchohentai` | https://muchohentai.com | — |
| MundoHentai | `MundoHentai` | `mundohentai` | https://mundohentai.net | — |
| NaughtyHentai | `NaughtyHentai` | `naughtyhentai` | https://www.naughtyhentai.com | — |
| NekoPoi | `Nekopoi` | `nekopoi` | https://nekopoi.care | 1 |
| NSFWR34 | `NsfwR34` | `nsfwr34` | https://nsfwr34.com | — |
| Ohentai | `Ohentai` | `ohentai` | https://ohentai.org | — |
| Okioto | `Okioto` | `okioto` | https://okioto.net | 1 |
| Oppai.Stream | `Oppai` | `oppai` | https://oppai.stream | 8 |
| R34Tube | `R34Tube` | `r34tube` | https://r34tube.com | — |
| Rule34 Paheal | `Rule34Paheal` | `rule34paheal` | https://rule34.paheal.net | — |
| Rule34s | `Rule34s` | `rule34s` | https://rule34s.com | — |
| Rule34Video | `Rule34Video` | `rule34video` | https://rule34video.com | — |
| The JOI Database | `TheJoiDatabase` | `thejoidatabase` | https://www.the-joi-database.com | — |
| 3DHentai | `ThreeDHentai` | `threehentai` | https://3dhentai.it.com | — |
| UncensoredHentai | `UncensoredHentai` | `uncensoredhentai` | https://uncensoredhentai.xxx | 8 |
| WatchAnimeHentai | `WatchAnimeHentai` | `watchanimehentai` | https://www.watchanimehentai.com | 1 |
| Watch Hentai | `WatchHentai` | `watchhentai` | https://watchhentai.net | 8 |
| XAnimePorn | `XAnimePorn` | `xanimeporn` | https://xanimeporn.com | 1 |
| ZhenTube | `ZhenTube` | `zhentube` | https://zhentube.com | 1 |

## Sources with genre rows

These **33** sources expose static home-screen browse rows. Rows that returned zero items were pruned; the counts below were verified with `provider_smoke.sh --genres`.

| Package | Rows |
|---|---|
| `animeidhentai` | 8 |
| `animesex` | 1 |
| `erotichentai` | 1 |
| `hanimered` | 6 |
| `hentaibros` | 1 |
| `hentaifreak` | 1 |
| `hentaihaven` | 8 |
| `hentaihavencom` | 8 |
| `hentaihavenred` | 5 |
| `hentaihavenxxx` | 6 |
| `hentaihd` | 1 |
| `hentaihorizon` | 2 |
| `hentaiking` | 8 |
| `hentaimama` | 8 |
| `hentaiocean` | 3 |
| `hentaiplay` | 8 |
| `hentaisea` | 8 |
| `hentaistreamtv` | 8 |
| `hentaistube` | 1 |
| `hentaitubeonline` | 7 |
| `hentaitv` | 8 |
| `hentaiyes` | 5 |
| `hentaiyoga` | 8 |
| `javguru` | 7 |
| `miohentai` | 6 |
| `nekopoi` | 1 |
| `okioto` | 1 |
| `oppai` | 8 |
| `uncensoredhentai` | 8 |
| `watchanimehentai` | 1 |
| `watchhentai` | 8 |
| `xanimeporn` | 1 |
| `zhentube` | 1 |

Distribution: **93** `/genre/`, **29** `/category/`, **19** `/tag/` (fallback), **10** `/genres/`, **6** `/rule34/`, **5** `/hentai/` — **162 total**.

## Removed in v3

These providers were deleted from the plugin because they had no working playback path. Their source directories, registrations and genre rows are gone.

| Source | Reason |
|---|---|
| H-Anime (`hanime`) | browse/detail worked, but every player embed had been removed (`doodst.com` returns “Video is Unavailable”) |
| Hentai-JP (`hentaijp`) | same dead Doodstream mirrors; `serverData` listed two unavailable servers |
| HentaiCloud (`hentaicloud`) | origin unreachable (no route to host) |
| Hentaigasm (`hentaigasm`) | domain no longer resolves |
| UnderHentai (`underhentai`) | Cloudflare timeout/403 for non-browser clients on every run |

## Site status

Final v3 sweep: **62 of 64 passed** end to end (home → details → episodes → links) with `provider_smoke.sh`. The two misses are intermittent, not structural:

| Source | Status |
|---|---|
| EroticHentai | passed on retry; its `aniporn.com` API throttles repeated requests |
| XAnimePorn | slow; times out under full-sweep concurrency, returns 4 quality links when run alone |
| HentaiStream | slow first request; passed in the final sweep |
| Hentai0 | occasional read timeout under load; passed in the final sweep |
| MuchoHentai, Ohentai, hanime.tv, Hentaiking | Cloudflare-fronted; first request can be slow |

Fixes verified in v3: Aki-H (details-only entries filtered, 3 HLS renditions), Hentai2w (age-gate cookie on redirect), EroticHentai (one-shot API request), Hentaverse (series regex no longer returns categories), HentaiSea/HentaiYoga/MioHentai/HentaiHavenXxx (genre-page parsers), Nekopoi (episode-post fallback).

## Adding a source

1. Port `sites/<package>/<Source>.kt` and `<Source>Provider.kt`.
2. Register the provider in `AIOHentaiPlugin.load()`.
3. Add it to `scripts/SmokeSites.kt` (or run `scripts/gen_smoke.py`).
4. Optionally add `Genres.bySite["<package>"]` and verify it with `provider_smoke.sh --genres <Class>`.
5. Add a row to this table, bump the plugin version, and push.
