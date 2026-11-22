package com.aiohentai.cs.sites.spankbang

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.MediaVideo
import com.aiohentai.cs.compat.MirrorPool
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.appHttpClient
import com.aiohentai.cs.compat.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

/**
 * SpankBang. The origin is behind Cloudflare; [client] routes through CloudStream's
 * WebView challenge solver in the app. Cards are `div[data-testid=video-item]` and the
 * player page embeds HLS masters/variants on `hls-*.sb-cd.com`.
 */
class Spankbang : AnimeHttpSource() {

    override val name = "@JalebiBae - SpankBang"
    override val id: Long = 1866139741386437001L
    private val mirrors = MirrorPool(
        primary = "https://spankbang.com",
        alternates = listOf("https://spankbang.party"),
        marker = "SpankBang",
    )
    override val baseUrl: String
        get() = mirrors.active
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request {
        ensureBase()
        return GET(if (page <= 1) "$baseUrl/trending_videos/" else "$baseUrl/trending_videos/$page/", headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request {
        ensureBase()
        return GET(if (page <= 1) "$baseUrl/new_videos/" else "$baseUrl/new_videos/$page/", headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        ensureBase()
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val url = if (page <= 1) "$baseUrl/s/$encoded/" else "$baseUrl/s/$encoded/$page/"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        ensureBase()
        val base = if (path.startsWith("/")) baseUrl.trimEnd('/') + path else path
        return if (page <= 1) "${base.trimEnd('/')}/1/" else "${base.trimEnd('/')}/$page/"
    }

    private fun ensureBase() {
        mirrors.base(client, headers)
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div[data-testid=video-item], div.js-video-item")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href*='/video/']") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("/video/")) return@mapNotNull null
                val img = card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = img?.attr("alt")?.trim()?.takeIf { it.isNotBlank() }
                        ?: link.attr("title").trim().takeIf { it.isNotBlank() }
                        ?: link.text().trim()
                    thumbnail_url = img?.absUrl("src")?.takeIf { it.startsWith("http") }
                        ?: img?.absUrl("data-src")?.takeIf { it.startsWith("http") }
                }
            }
            .distinctBy { it.url }

        val url = response.request.url.toString()
        val current = Regex("/(?:new_videos|trending_videos|s/[^/]+)/(\\d+)/").find(url)
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val hasNext = doc.select("a[href]").any {
            it.attr("href").contains("/${current + 1}/")
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
            genre = doc.select("a[href*='/category/'], a[href*='/pornstar/']")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(12)
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
        val urls = Regex("""https://[^"'\\ ]+\.m3u8[^"'\\ ]*""")
            .findAll(html)
            .map { it.value.replace("\\/", "/") }
            .filter { it.contains("sb-cd.com") }
            .distinct()
            .toList()

        if (urls.isEmpty()) return emptyList()

        val master = urls.firstOrNull { it.contains("master.m3u8") && it.contains(",480p,") }
            ?: urls.first()
        val videos = mutableListOf(MediaVideo.create(master, "Auto", master))

        urls.forEach { url ->
            val quality = Regex("""-,(240p|360p|480p|720p|1080p|2160p),""")
                .find(url)?.groupValues?.get(1) ?: return@forEach
            videos += MediaVideo.create(url, quality, url)
        }

        return videos.distinctBy { it.url }
    }
}
