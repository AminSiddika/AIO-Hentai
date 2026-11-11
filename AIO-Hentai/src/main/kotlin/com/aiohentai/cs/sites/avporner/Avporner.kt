package com.aiohentai.cs.sites.avporner

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
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

class Avporner : AnimeHttpSource() {

    override val name = "@JalebiBae - AVPorner"
    override val id: Long = 5027116735701840957L
    override val baseUrl = "https://avporner.com"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    override fun popularAnimeRequest(page: Int): Request =
        GET(withPage("$baseUrl/videos/category/Japanese", page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request = GET(withPage("$baseUrl/", page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/search?keyword=$encoded"
        else "$baseUrl/search?keyword=$encoded&page=$page"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    private fun withPage(base: String, page: Int): String {
        if (page <= 1) return base
        return if (base.contains('?')) "$base&page=$page" else "$base?page=$page"
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select(".video-list")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href*='/watch/']") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (href.isBlank()) return@mapNotNull null
                val image = card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst(".video-list-title h4")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: image?.attr("alt")?.trim().orEmpty()
                    thumbnail_url = image?.absUrl("data-src")?.takeIf { it.isNotBlank() }
                        ?: image?.absUrl("src")
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.select("a[href]").any { it.text().trim() == "»" || it.text().contains("Next", true) }
        return AnimesPage(items, hasNext)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            description = doc.selectFirst("meta[name=description]")?.attr("content")
            genre = doc.select("a[href*='/videos/category/']")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
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
        val html = doc.html()
        val headers = Headers.Builder()
            .add("Referer", "$baseUrl/")
            .add("Origin", baseUrl)
            .build()

        val pairs = SOURCE.findAll(html)
            .map { it.groupValues[1] to it.groupValues[2] }
            .filter { (url, _) -> url.startsWith("http") }
            .distinctBy { it.first }
            .toList()

        if (pairs.isNotEmpty()) {
            return pairs.map { (url, label) -> MediaVideo.create(url, label, url, headers = headers) }
        }
        return CONTENT_URL.findAll(html)
            .map { it.groupValues[1] }
            .filter { it.startsWith("http") }
            .distinct()
            .map { MediaVideo.create(it, qualityOf(it), it, headers = headers) }
            .toList()
    }

    private fun qualityOf(url: String): String =
        QUALITY.find(url)?.groupValues?.get(1)?.let { "${it}p" } ?: "MP4"

    companion object {
        private val SOURCE = Regex(
            """"file"\s*:\s*"([^"]+)"[\s\S]{0,300}?"label"\s*:\s*"([^"]+)"""",
        )
        private val CONTENT_URL = Regex(""""contentUrl"\s*:\s*"([^"]+\.mp4[^"]*)"""")
        private val QUALITY = Regex("""[/_](\d{3,4})p?\.mp4""")
    }
}
