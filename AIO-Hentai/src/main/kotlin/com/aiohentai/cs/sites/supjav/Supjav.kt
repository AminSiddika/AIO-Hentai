package com.aiohentai.cs.sites.supjav

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
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.net.URLEncoder

/**
 * SupJav. Detail pages expose server buttons whose `data-link` token is passed through
 * lk1.supremejav.com, which redirects to a VidHide-style embed. The embed's packed JS
 * expands to hls2/hls3/hls4 playlist URLs.
 */
class Supjav : AnimeHttpSource() {

    override val name = "@JalebiBae - SupJav"
    override val id: Long = 4488276278055216101L
    override val baseUrl = "https://supjav.com"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request =
        GET(withPage("$baseUrl/popular", page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(withPage("$baseUrl/", page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/?s=$encoded" else "$baseUrl/page/$page?s=$encoded"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        val base = when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/") -> baseUrl.trimEnd('/') + path
            else -> baseUrl.trimEnd('/') + "/" + path
        }
        return withPage(base, page)
    }

    private fun withPage(base: String, page: Int): String {
        if (page <= 1) return base
        return if (base == "$baseUrl/") "$baseUrl/page/$page" else "${base.trimEnd('/')}/page/$page"
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div.post")
            .mapNotNull { post ->
                val link = post.selectFirst("h3 a[href]")
                    ?: post.selectFirst("a.img[href]")
                    ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains(".html")) return@mapNotNull null

                val image = post.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = link.text().trim().ifBlank { link.attr("title").trim() }
                    thumbnail_url = image?.absUrl("data-src")?.takeIf { it.isNotBlank() }
                        ?: image?.absUrl("src")
                }
            }
            .distinctBy { it.url }

        val current = Regex("""/page/(\d+)""").find(response.request.url.toString())
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val hasNext = doc.select("a[href]").any {
            it.text().trim() == ">" ||
                it.text().trim().equals("Next", true) ||
                it.attr("href").contains("/page/${current + 1}")
        }
        return AnimesPage(items, hasNext)
    }

    // --------------------------------------------------------------- details

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val poster = POSTER.find(doc.selectFirst("#player-wrap")?.attr("style").orEmpty())
            ?.groupValues?.get(1)
        val genres = doc.select("a[href*='/tag/']")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim().orEmpty()
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

    // ----------------------------------------------------------------- video

    override fun videoListParse(response: Response): List<Video> {
        val pageUrl = response.request.url.toString()
        val doc = response.asJsoup()
        val bg = doc.selectFirst("#dz_video")?.attr("bg").orEmpty()
        val tokens = doc.select("a.btn-server[data-link]")
            .map { it.attr("data-link") }
            .filter { it.isNotBlank() }
            .distinct()

        val videos = mutableListOf<Video>()
        for (token in tokens.take(4)) {
            videos += resolveServer(token, bg, pageUrl)
            if (videos.isNotEmpty()) break
        }
        return videos.distinctBy { it.url }
    }

    private fun resolveServer(token: String, bg: String, pageUrl: String): List<Video> {
        val jumpUrl = "https://lk1.supremejav.com/supjav.php?l=$token&bg=$bg"
        fetchPage(jumpUrl, pageUrl) ?: return emptyList()

        val playerUrl = "https://lk1.supremejav.com/supjav.php?c=${token.reversed()}"
        val player = fetchPage(playerUrl, jumpUrl) ?: return emptyList()
        val decoded = unpackAll(player.first)

        val urls = HLS_PAIR.findAll(decoded)
            .associate { it.groupValues[1] to it.groupValues[2] }
            .let { map ->
                listOf("hls4", "hls3", "hls2")
                    .firstNotNullOfOrNull { key -> map[key] }
                    ?.let { listOf(absolute(it, player.second)) }
                    ?: emptyList()
            }
            .ifEmpty { M3U8.findAll(decoded + player.first).map { it.value }.take(1).toList() }

        if (urls.isEmpty()) return emptyList()
        val headers = Headers.Builder()
            .add("Referer", player.second)
            .add("Origin", originOf(player.second))
            .build()
        return urls.distinct().map { MediaVideo.create(it, "HLS", it, headers = headers) }
    }

    private fun fetchPage(url: String, referer: String): Pair<String, String>? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_UA)
            .header("Referer", referer)
            .build()
        client.newCall(request).execute().use { response ->
            (response.body?.string().orEmpty()) to response.request.url.toString()
        }
    }.getOrNull()

    private fun unpackAll(html: String): String = buildString {
        for (match in PACKED.findAll(html)) {
            val payload = match.groupValues[1]
            val base = match.groupValues[2].toIntOrNull() ?: continue
            val words = match.groupValues[4].split('|')
            append(TOKEN.replace(payload) { token ->
                val index = token.value.toIntOrNull(base)
                if (index != null && index in words.indices) words[index] else token.value
            })
            append('\n')
        }
    }

    private fun absolute(url: String, base: String): String {
        if (url.startsWith("http")) return url
        return base.toHttpUrlOrNull()?.resolve(url)?.toString() ?: url
    }

    private fun originOf(url: String): String {
        val parsed = url.toHttpUrlOrNull() ?: return baseUrl
        return "${parsed.scheme}://${parsed.host}"
    }

    companion object {
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        private val POSTER = Regex("""url\((https?://[^)]+)\)""")
        private val HLS_PAIR = Regex("""["'](hls\d+)["']\s*:\s*["']([^"']+)["']""")
        private val PACKED = Regex(
            """\}\('(.*?)',(\d+),(\d+),'(.*?)'\.split\('\|'\)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val TOKEN = Regex("""\b[0-9a-z]+\b""")
        private val M3U8 = Regex("""https?://[^'"\\\s]+\.m3u8[^'"\\\s]*""")
    }
}
