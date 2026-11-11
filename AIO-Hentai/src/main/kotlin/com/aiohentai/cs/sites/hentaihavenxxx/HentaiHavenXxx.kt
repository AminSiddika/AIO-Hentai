package com.aiohentai.cs.sites.hentaihavenxxx

import com.aiohentai.cs.compat.MediaVideo

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.net.URLEncoder

class HentaiHavenXxx : AnimeHttpSource() {

    override val name = "@JalebiBae - HentaiHaven.xxx"
    override val id: Long = 746394674563039269L
    override val baseUrl = "https://hentaihaven.xxx"
    init {
        MediaVideo.referer = "$baseUrl/"
    }
    override val lang = "en"
    override val supportsLatest = true

    override fun popularAnimeRequest(page: Int): Request = GET(listUrl(page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request = GET(listUrl(page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) {
            "$baseUrl/?s=$encoded"
        } else {
            "$baseUrl/page/$page/?s=$encoded"
        }
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    private fun listUrl(page: Int): String =
        if (page <= 1) "$baseUrl/browse/trending/" else "$baseUrl/browse/trending/page/$page/"

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val watchLinks = doc.select("a[href^=/watch/]")
        val links = if (watchLinks.isNotEmpty()) {
            watchLinks
        } else {
            // Rule34 category pages link straight to /rule34/video/<slug>/.
            doc.select("a[href*='/rule34/video/']")
        }
        val items = links
            .mapNotNull { link ->
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!SERIES_URL.matches(href) && !VIDEO_URL.matches(href)) return@mapNotNull null
                val img = link.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = img?.attr("alt")?.trim()
                        ?: link.attr("title").ifBlank { link.text() }.trim()
                    thumbnail_url = img?.absUrl("src")
                }
            }
            .distinctBy { it.url }

        val current = Regex("/page/(\\d+)/").find(response.request.url.toString())
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val hasNext = doc.select("a[href]").any {
            it.attr("href").contains("/page/${current + 1}/")
        }
        return AnimesPage(items, hasNext)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            genre = doc.select("a[href^=/series/]")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
            description = doc.selectFirst("meta[name=description]")?.attr("content")
            status = SAnime.COMPLETED
            initialized = true
        }
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val seriesPath = response.request.url.encodedPath.trimEnd('/')
        val episodes = response.asJsoup()
            .select("a[href]")
            .mapNotNull { link ->
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("$seriesPath/episode-")) return@mapNotNull null
                val number = EPISODE.find(href)?.groupValues?.get(1)?.toIntOrNull()
                    ?: return@mapNotNull null
                SEpisode.create().apply {
                    setUrlWithoutDomain(href.trimEnd('/') + "/")
                    name = "Episode $number"
                    episode_number = number.toFloat()
                }
            }
            .distinctBy { it.url }
            .sortedBy { it.episode_number }

        if (episodes.isNotEmpty()) return episodes

        // Rule34 pages are single videos, not multi-episode series.
        return listOf(
            SEpisode.create().apply {
                setUrlWithoutDomain(response.request.url.toString())
                name = "Full Video"
                episode_number = 1f
            },
        )
    }

    override fun videoListParse(response: Response): List<Video> {
        val doc = response.asJsoup()
        val html = doc.html()
        val playlist = doc.selectFirst("source[src*='.m3u8']")?.absUrl("src")
            ?: Regex(""""contentUrl":"([^"]+\.m3u8[^"]*)"""").find(html)?.groupValues?.get(1)
            ?: return emptyList()
        val videos = mutableListOf(MediaVideo.create(playlist, "HLS", playlist))
        // Some entries expose direct MP4 renditions in structured data.
        for (m in Regex(""""contentUrl":"([^"]+\.mp4[^"]*)"""").findAll(html)) {
            val url = m.groupValues[1].replace("\\/", "/")
            if (url.startsWith("http")) videos.add(MediaVideo.create(url, "MP4", url))
        }
        return videos.distinctBy { it.url }
    }

    companion object {
        private val SERIES_URL = Regex("""^https://hentaihaven\.xxx/watch/[^/?#]+/?$""")
        private val VIDEO_URL = Regex("""^https://hentaihaven\.xxx/rule34/video/[^/?#]+/?$""")
        private val EPISODE = Regex("""/episode-(\d+)""")
    }
}
