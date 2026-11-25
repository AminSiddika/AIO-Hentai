package com.aiohentai.cs.sites.javbangers

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
 * JavBangers (KVS). Cards are KVS `div.video-item`; detail MP4 renditions live on
 * `javbangers.com/get_file/.../{id}(_720p).mp4`.
 */
class Javbangers : AnimeHttpSource() {

    override val name = "@JalebiBae - JavBangers"
    override val id: Long = 7018265931703284591L
    override val baseUrl = "https://www.javbangers.com"
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
                    title = link.attr("title").trim().takeIf { it.isNotBlank() }
                        ?: img?.attr("alt")?.trim().orEmpty()
                    thumbnail_url = img?.absUrl("data-original")?.takeIf { it.startsWith("http") }
                        ?: img?.absUrl("data-src")?.takeIf { it.startsWith("http") }
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
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
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
        val pageUrl = response.request.url.toString()
        val html = response.asJsoup().html()
        val id = Regex("/video/(\\d+)").find(pageUrl)?.groupValues?.get(1)
            ?: return emptyList()
        val pattern = Regex("""https://[^"'\s]*?/get_file/[^"'\s]*?/$id/[^"'\s]*?\.mp4[^"'\s]*""")
        return pattern.findAll(html)
            .map { it.value }
            .filter { !it.contains("_preview", ignoreCase = true) }
            .distinct()
            .map { url ->
                val quality = Regex("""_(\d+)p\.mp4""").find(url)?.groupValues?.get(1)?.plus("p") ?: "480p"
                MediaVideo.create(url, quality, url)
            }
            .toList()
    }
}
