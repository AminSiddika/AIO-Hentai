package com.aiohentai.cs.sites.javtsunami

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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.net.URLEncoder

class Javtsunami : AnimeHttpSource() {

    override val name = "@JalebiBae - JAVTsunami"
    override val id: Long = 6721546895648746113L
    override val baseUrl = "https://javtsunami.com"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    override fun popularAnimeRequest(page: Int): Request = GET(withPage(page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request = GET(withPage(page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/?s=$encoded" else "$baseUrl/page/$page/?s=$encoded"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    private fun withPage(page: Int): String =
        if (page <= 1) "$baseUrl/" else "$baseUrl/page/$page/"

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("article.video-card")
            .mapNotNull { card ->
                val link = card.selectFirst("a.thumb-link[href]")
                    ?: card.selectFirst(".card-title a[href]")
                    ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (href.isBlank()) return@mapNotNull null
                val image = card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst(".card-title a")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: image?.attr("alt")?.trim().orEmpty()
                    thumbnail_url = image?.absUrl("data-src")?.takeIf { it.isNotBlank() }
                        ?: image?.absUrl("src")
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
            genre = doc.select("a[href*='/tag/']")
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
        val resolveUrl = doc.selectFirst(".jw-player-shell[data-resolve-url]")
            ?.attr("data-resolve-url")
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        val tokens = doc.select("iframe[data-server-token]")
            .map { it.attr("data-server-token") }
            .filter { it.isNotBlank() }
            .distinct()

        for (token in tokens.take(3)) {
            val playerUrl = resolve(resolveUrl, token, pageUrl) ?: continue
            val player = fetchPage(playerUrl, pageUrl) ?: continue
            val urls = hlsUrls(player.first, player.second)
            if (urls.isNotEmpty()) return urls
        }
        return emptyList()
    }

    private fun resolve(resolveUrl: String, token: String, referer: String): String? {
        val request = Request.Builder()
            .url(resolveUrl)
            .post("{\"token\":\"$token\"}".toRequestBody(JSON_MEDIA))
            .header("User-Agent", DESKTOP_UA)
            .header("Referer", referer)
            .header("Origin", baseUrl)
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                JSONObject(body).optString("url").takeIf { it.startsWith("http") }
            }
        }.getOrNull()
    }

    private fun hlsUrls(html: String, base: String): List<Video> {
        val decoded = unpackAll(html)
        val map = HLS_PAIR.findAll(decoded)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val chosen = listOf("hls4", "hls3", "hls2")
            .firstNotNullOfOrNull { key -> map[key] }
            ?: M3U8.find(decoded)?.value
            ?: return emptyList()
        val stream = absolute(chosen, base)
        val origin = base.toHttpUrlOrNull()?.let { "${it.scheme}://${it.host}" } ?: baseUrl
        val headers = Headers.Builder()
            .add("Referer", base)
            .add("Origin", origin)
            .build()
        return listOf(MediaVideo.create(stream, "HLS", stream, headers = headers))
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

    companion object {
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        private val JSON_MEDIA = "application/json".toMediaType()
        private val HLS_PAIR = Regex("""["'](hls\d+)["']\s*:\s*["']([^"']+)["']""")
        private val PACKED = Regex(
            """\}\('(.*?)',(\d+),(\d+),'(.*?)'\.split\('\|'\)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val TOKEN = Regex("""\b[0-9a-z]+\b""")
        private val M3U8 = Regex("""https?://[^'"\\\s]+\.m3u8[^'"\\\s]*""")
    }
}
