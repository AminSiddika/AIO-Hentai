package com.aiohentai.cs.sites.xhamster

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
 * xHamster. The listing is server-rendered (`div.thumb-list__item`) with absolute cover
 * URLs. The player page preloads an HLS master playlist and usually a 480p H.264 MP4.
 */
class Xhamster : AnimeHttpSource() {

    override val name = "@JalebiBae - xHamster"
    override val id: Long = 3382749061755028109L
    override val baseUrl = "https://xhamster.com"
    override val lang = "en"
    override val supportsLatest = true

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request =
        GET(if (page <= 1) "$baseUrl/best" else "$baseUrl/best/$page", headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(if (page <= 1) "$baseUrl/newest" else "$baseUrl/newest/$page", headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val url = if (page <= 1) "$baseUrl/search/$encoded" else "$baseUrl/search/$encoded?page=$page"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        val base = if (path.startsWith("/")) baseUrl.trimEnd('/') + path else path
        return if (page <= 1) base else "$base/$page"
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        // Only the first screen of cards is server-rendered; the rest are skeletons.
        // The inline JSON payload still carries every video's id -> cover URL.
        val posterById = Regex(
            """"id":(\d+),.{0,2500}?"thumbURL":"([^"]+)"""",
            RegexOption.DOT_MATCHES_ALL,
        ).findAll(doc.html())
            .associate { it.groupValues[1] to it.groupValues[2].replace("\\/", "/") }

        val items = doc.select("div.thumb-list__item")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href*='/videos/']") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("/videos/")) return@mapNotNull null
                val img = card.selectFirst("img")
                val videoId = card.attr("data-video-id")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = link.attr("aria-label").trim().takeIf { it.isNotBlank() }
                        ?: link.attr("title").trim().takeIf { it.isNotBlank() }
                        ?: img?.attr("alt")?.trim().orEmpty()
                    thumbnail_url = img?.absUrl("data-src")?.takeIf { it.startsWith("http") }
                        ?: img?.absUrl("src")?.takeIf { it.startsWith("http") }
                        ?: posterById[videoId]
                }
            }
            .distinctBy { it.url }

        val url = response.request.url.toString()
        val current = Regex("/(?:newest|best)/(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: Regex("[?&]page=(\\d+)").find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: 1
        val hasNext = doc.select("a[href]").any {
            it.attr("href").contains("/${current + 1}") || it.attr("href").contains("page=${current + 1}")
        } || items.size >= 20
        return AnimesPage(items, hasNext)
    }

    // --------------------------------------------------------------- details

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            genre = doc.select("a[href*='/categories/'], a[href*='/tags/']")
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
        val hls = Regex("""https://[^"'\\ ]+\.m3u8[^"'\\ ]*""")
            .findAll(html)
            .map { it.value.replace("\\/", "/") }
            .filter { it.contains("hls4") || it.contains(".h264.") }
            .distinct()
            .toList()

        val videos = mutableListOf<Video>()
        val preferred = hls.firstOrNull { it.contains(".h264.") } ?: hls.firstOrNull()
        if (preferred != null) videos += MediaVideo.create(preferred, "HLS", preferred)

        Regex("""https://video[^"'\\ ]+/(\d+)p\.h264\.mp4[^"'\\ ]*""")
            .findAll(html)
            .map { it.value.replace("\\/", "/") }
            .distinct()
            .forEach { url ->
                val quality = Regex("""/(\d+)p\.h264\.mp4""").find(url)?.groupValues?.get(1)
                videos += MediaVideo.create(url, quality?.let { "${it}p" } ?: "SD", url)
            }

        return videos.distinctBy { it.url }
    }
}
