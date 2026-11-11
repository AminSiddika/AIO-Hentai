package com.aiohentai.cs.sites.asianviral

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
import kotlin.random.Random

class Asianviral : AnimeHttpSource() {

    override val name = "@JalebiBae - AsianViral"
    override val id: Long = 5517157753694810247L
    override val baseUrl = "https://asianviral.net"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    override fun popularAnimeRequest(page: Int): Request = GET(withPage("$baseUrl/", page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request = GET(withPage("$baseUrl/", page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/?s=$encoded" else "$baseUrl/page/$page/?s=$encoded"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    private fun withPage(base: String, page: Int): String =
        if (page <= 1) base else "${base.trimEnd('/')}/page/$page/"

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("article.loop-video")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href]") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.startsWith("http")) return@mapNotNull null
                val image = card.selectFirst("img.video-main-thumb") ?: card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst("header span")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: link.attr("title").trim()
                    thumbnail_url = image?.absUrl("src")?.takeIf { it.isNotBlank() }
                        ?: image?.absUrl("data-src")
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.select("a[href]").any { it.text().contains("Next", true) }
        return AnimesPage(items, hasNext)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            description = doc.selectFirst("meta[name=description]")?.attr("content")
            genre = doc.select("a[href*='/category/']")
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
        val pageUrl = response.request.url.toString()
        val doc = response.asJsoup()
        val iframe = doc.selectFirst("iframe[src*='playmogo.com/e/']")?.let {
            it.absUrl("src").ifBlank { it.attr("src") }
        }

        if (iframe != null) {
            resolvePlaymogo(iframe, pageUrl).takeIf { it.isNotEmpty() }?.let { return it }
        }

        val direct = doc.select("video[src], video source[src]")
            .mapNotNull { it.absUrl("src").takeIf { url -> url.startsWith("http") } }
            .distinct()
        return direct.map { MediaVideo.create(it, "MP4", it) }
    }

    private fun resolvePlaymogo(embedUrl: String, referer: String): List<Video> {
        val html = fetchText(embedUrl, referer) ?: return emptyList()
        val pass = PASS.find(html)?.groupValues?.get(1) ?: return emptyList()
        val token = TOKEN.find(html)?.groupValues?.get(1) ?: return emptyList()
        val base = fetchText("https://playmogo.com/pass_md5/$pass", "https://playmogo.com/")
            ?: return emptyList()
        if (!base.trim().startsWith("http")) return emptyList()

        val suffix = (1..10).map { CHARS.random() }.joinToString("")
        val url = base.trim() + suffix + "?token=$token&expiry=" + System.currentTimeMillis()
        val headers = Headers.Builder().add("Referer", "https://playmogo.com/").build()
        return listOf(MediaVideo.create(url, "MP4", url, headers = headers))
    }

    private fun fetchText(url: String, referer: String?): String? = runCatching {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_UA)
            .header("Accept-Language", "en-US,en;q=0.9")
        referer?.let { builder.header("Referer", it) }
        client.newCall(builder.build()).execute().use { it.body?.string() }
    }.getOrNull()

    companion object {
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private const val CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        private val PASS = Regex("""pass_md5/([^'"]+)""")
        private val TOKEN = Regex("""token=([A-Za-z0-9]+)""")
    }
}
