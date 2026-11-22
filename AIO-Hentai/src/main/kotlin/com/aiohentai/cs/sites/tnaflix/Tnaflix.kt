package com.aiohentai.cs.sites.tnaflix

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.MediaVideo
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.asJsoup
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

/**
 * TNAFlix. Listing cards lazy-load covers through `data-src`; the detail page exposes
 * direct MP4 renditions (`1080p`, `720p`, `480p`, `360p`) on the stream host.
 */
class Tnaflix : AnimeHttpSource() {

    override val name = "@JalebiBae - TNAFlix"
    override val id: Long = 5148037726154092467L
    override val baseUrl = "https://www.tnaflix.com"
    override val lang = "en"
    override val supportsLatest = true

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request =
        GET(if (page <= 1) "$baseUrl/featured" else "$baseUrl/featured/$page", headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(if (page <= 1) "$baseUrl/new" else "$baseUrl/new/$page", headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/search?what=$encoded" else "$baseUrl/search?what=$encoded&page=$page"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        val base = if (path.startsWith("/")) baseUrl.trimEnd('/') + path else path
        return if (page <= 1) base else "$base/featured/$page"
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("a.video-thumb[href*='/video']")
            .mapNotNull { link ->
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("/video")) return@mapNotNull null
                val img = link.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = img?.attr("alt")?.trim()?.takeIf { it.isNotBlank() }
                        ?: link.attr("title").trim().takeIf { it.isNotBlank() }
                        ?: href.substringAfterLast('/').replace('-', ' ')
                    thumbnail_url = img?.absUrl("data-src")?.takeIf { it.startsWith("http") }
                        ?: img?.absUrl("src")?.takeIf {
                            it.startsWith("http") && !it.contains("video_cover_placeholder")
                        }
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.selectFirst("link[rel=next]") != null || items.size >= 20
        return AnimesPage(items, hasNext)
    }

    // --------------------------------------------------------------- details

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            genre = doc.select("a[href*='/porn-videos/'], a[href*='/categories/']")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
            description = doc.selectFirst("meta[name=description]")?.attr("content")
            status = SAnime.COMPLETED
            initialized = true
        }
    }

    override fun episodeListParse(response: Response): List<SEpisode> = listOf(
        SEpisode.create().apply {
            setUrlWithoutDomain(response.request.url.toString())
            name = "Full Video"
            episode_number = 1f
        },
    )

    // ----------------------------------------------------------------- video

    override fun videoListParse(response: Response): List<Video> {
        val html = response.asJsoup().html()
        val videos = mutableListOf<Video>()

        Regex("""https://[^"'\\ ]+\.m3u8[^"'\\ ]*""")
            .findAll(html)
            .map { it.value.replace("\\/", "/") }
            .distinct()
            .forEach { url -> videos += MediaVideo.create(url, "HLS", url) }

        Regex("""https://[^"'\\ ]+\.mp4[^"'\\ ]*""")
            .findAll(html)
            .map { it.value.replace("\\/", "/") }
            .filter { !it.contains("/trailer", ignoreCase = true) }
            .distinct()
            .forEach { url ->
                val quality = Regex("""(\d+p)\.mp4""").find(url)?.groupValues?.get(1)?.uppercase()
                videos += MediaVideo.create(url, quality ?: "MP4", url)
            }

        return videos.distinctBy { it.url }
            .sortedByDescending { Regex("""^(\d+)""").find(it.quality)?.groupValues?.get(1)?.toIntOrNull() ?: 0 }
    }
}
