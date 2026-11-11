package com.aiohentai.cs.sites.p91

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.MediaVideo
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.appHttpClient
import com.aiohentai.cs.compat.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

class P91 : AnimeHttpSource() {

    override val name = "@JalebiBae - 91Porn"
    override val id: Long = 7317846975511526037L
    override val baseUrl = "https://www.91porn.com"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    override fun popularAnimeRequest(page: Int): Request =
        GET(withPage("$baseUrl/v.php?category=hot&viewtype=basic", page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(withPage("$baseUrl/v.php?next=watch", page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val base = "$baseUrl/search_result.php?search_id=$encoded&search_type=search_videos"
        return GET(if (page <= 1) base else "$base&page=$page", headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    private fun withPage(base: String, page: Int): String =
        if (page <= 1) base else "$base&page=$page"

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div.well")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href*='view_video.php']") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (href.isBlank()) return@mapNotNull null
                val image = card.selectFirst("img.img-responsive")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst("span.video-title")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: link.attr("title").trim()
                    thumbnail_url = image?.absUrl("src")?.takeIf { it.isNotBlank() }
                        ?: image?.absUrl("data-src")
                }
            }
            .distinctBy { it.url }

        return AnimesPage(items, items.size >= 20)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val title = doc.title().substringBefore(" - 91porn").trim()
            .ifBlank { doc.selectFirst("h1")?.text()?.trim().orEmpty() }
        val poster = doc.selectFirst("video[poster]")?.absUrl("poster")
        val genres = doc.select("a[href*='v.php?category=']")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return SAnime.create().apply {
            this.title = title
            thumbnail_url = poster
            description = doc.selectFirst("meta[name=description]")?.attr("content")
            genre = genres.joinToString(", ")
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
        val doc = response.asJsoup()
        val urls = linkedSetOf<String>()

        // The real <source> is inside a comment and written by strencode2(...).
        for (match in STRENCODE.findAll(doc.html())) {
            val decoded = runCatching {
                java.net.URLDecoder.decode(match.groupValues[1], "UTF-8")
            }.getOrNull().orEmpty()
            SOURCE_URL.find(decoded)?.groupValues?.get(1)?.let { urls += it }
        }

        doc.select("video[src], video source[src], source[src]")
            .mapNotNullTo(urls) { it.absUrl("src").takeIf { url -> url.startsWith("http") } }

        return urls.filter { it.startsWith("http") }
            .map { MediaVideo.create(it, "MP4", it) }
    }

    companion object {
        private val STRENCODE = Regex("""strencode2\("([^"]+)"\)""")
        private val SOURCE_URL = Regex("""src=['"]([^'"]+)['"]""")
    }
}
