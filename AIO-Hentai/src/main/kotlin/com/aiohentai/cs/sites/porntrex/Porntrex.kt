package com.aiohentai.cs.sites.porntrex

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
 * PornTrex (KVS). Listing covers are protocol-relative on `ptx.cdntrex.com`; the detail
 * page's `flashvars` object carries the 480p / 720p / 1080p MP4 renditions.
 */
class Porntrex : AnimeHttpSource() {

    override val name = "@JalebiBae - PornTrex"
    override val id: Long = 5583921017334650827L
    override val baseUrl = "https://www.porntrex.com"
    override val lang = "en"
    override val supportsLatest = true

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    override fun popularAnimeRequest(page: Int): Request =
        GET(if (page <= 1) "$baseUrl/top-rated/" else "$baseUrl/top-rated/$page/", headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(if (page <= 1) "$baseUrl/latest-updates/" else "$baseUrl/latest-updates/$page/", headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val url = if (page <= 1) "$baseUrl/search/$encoded/" else "$baseUrl/search/$encoded/?page=$page"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        val base = if (path.startsWith("/")) baseUrl.trimEnd('/') + path else path
        return if (page <= 1) base else "$base?page=$page"
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div.video-item")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href*='/video/']") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("/video/")) return@mapNotNull null
                val img = card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst(".video-item-title")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: img?.attr("alt")?.trim().orEmpty()
                    thumbnail_url = img?.absUrl("data-src")?.takeIf { it.startsWith("http") }
                        ?: img?.absUrl("src")?.takeIf { it.startsWith("http") }
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.selectFirst("a[rel=next], a.next[href]") != null || items.size >= 20
        return AnimesPage(items, hasNext)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.let { meta ->
                meta.absUrl("content").ifBlank { meta.attr("content") }
            }
            genre = doc.select("a[href*='/categories/']")
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

    override fun videoListParse(response: Response): List<Video> {
        val html = response.asJsoup().html()
        val videos = mutableListOf<Video>()
        val renditions = listOf(
            Triple("video_url", "video_url_text", "480p"),
            Triple("video_alt_url", "video_alt_url_text", "720p"),
            Triple("video_alt_url2", "video_alt_url2_text", "1080p"),
        )
        for ((urlKey, textKey, fallback) in renditions) {
            val url = Regex("""$urlKey:\s*'([^']+)'""").find(html)?.groupValues?.get(1)
                ?.replace("\\/", "/") ?: continue
            if (!url.startsWith("http")) continue
            val label = Regex("""$textKey:\s*'([^']+)'""").find(html)?.groupValues?.get(1)
                ?.trim()?.takeIf { it.isNotBlank() } ?: fallback
            videos += MediaVideo.create(url, label, url)
        }
        return videos.distinctBy { it.url }
    }
}
