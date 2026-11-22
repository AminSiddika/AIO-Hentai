package com.aiohentai.cs.sites.xnxx

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.MediaVideo
import com.aiohentai.cs.compat.MirrorPool
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.asJsoup
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

/**
 * XNXX. Same layout and player as XVideos: covers live in `data-src`, playback URLs in
 * `html5player.setVideoHLS(...)` / `setVideoUrlHigh(...)`.
 *
 * The public homepage is only a category hub, so both home rows use the month's
 * `/best` listing. Its redirect (`/best/YYYY-MM`) is remembered for paging.
 */
class Xnxx : AnimeHttpSource() {

    override val name = "@JalebiBae - XNXX"
    override val id: Long = 7215409826173985413L
    private val mirrors = MirrorPool(
        primary = "https://www.xnxx.com",
        alternates = listOf(
            "https://www.xnxx2.com",
            "https://www.xnxx3.com",
            "https://www.xnxx.tv",
            "https://www.xnxx.xxx",
        ),
        marker = "XNXX.COM",
    )
    override val baseUrl: String
        get() = mirrors.active
    override val lang = "en"
    override val supportsLatest = true

    @Volatile
    private var bestMonth: String? = null

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request {
        ensureBase()
        return GET(bestUrl(page), headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request {
        ensureBase()
        return GET(bestUrl(page), headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    private fun bestUrl(page: Int): String {
        if (page <= 1) return "$baseUrl/best"
        val month = bestMonth
        return if (month != null) "$baseUrl/best/$month/${page - 1}" else "$baseUrl/best/${page - 1}"
    }

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        ensureBase()
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val url = if (page <= 1) "$baseUrl/search/$encoded" else "$baseUrl/search/$encoded/${page - 1}"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        ensureBase()
        val base = if (path.startsWith("/")) baseUrl.trimEnd('/') + path else path
        return if (page <= 1) "$base?top" else "$base/${page - 1}"
    }

    private fun ensureBase() {
        mirrors.base(client, headers)
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        Regex("/best/(\\d{4}-\\d{2})").find(response.request.url.toString())
            ?.groupValues?.get(1)
            ?.let { bestMonth = it }

        val items = doc.select("div.thumb-block")
            .mapNotNull { card ->
                val link = card.selectFirst("p.title a[href]")
                    ?: card.selectFirst("a[href^=/video]")
                    ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("/video")) return@mapNotNull null
                val img = card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = link.attr("title").ifBlank { link.text() }.trim()
                        .ifBlank { img?.attr("alt")?.trim().orEmpty() }
                    thumbnail_url = img?.absUrl("data-src")?.takeIf { it.startsWith("http") }
                        ?: img?.absUrl("src")?.takeIf { it.startsWith("http") }
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.selectFirst("a.next-page[href], li.next-page a[href]") != null ||
            items.size >= 20
        return AnimesPage(items, hasNext)
    }

    // --------------------------------------------------------------- details

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            genre = doc.select("a[href*='/tags/']")
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
        val urls = linkedSetOf<String>()
        Regex("""setVideoHLS\(['"]([^'"]+)['"]\)""").findAll(html)
            .forEach { urls += it.groupValues[1] }
        Regex("""setVideoUrlHigh\(['"]([^'"]+)['"]\)""").findAll(html)
            .forEach { urls += it.groupValues[1] }

        return urls.filter { it.startsWith("http") }.map { url ->
            val quality = if (url.contains(".m3u8", true)) "HLS" else "HD"
            MediaVideo.create(url, quality, url)
        }
    }
}
