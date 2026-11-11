package com.aiohentai.cs.sites.sextb

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
import android.util.Base64
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

/**
 * SexTB. Detail pages embed an f4scdn.com player whose `data-api` returns a signed HLS
 * `src`. The API is fetched with `ab=0&n=1` so it returns the direct playlist instead of
 * refusing on the ad-blocker check.
 */
class Sextb : AnimeHttpSource() {

    override val name = "@JalebiBae - SexTB"
    override val id: Long = 8330751562263699013L
    override val baseUrl = "https://sextb.net"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request =
        GET(withPage("$baseUrl/jav-uncensored", page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(withPage("$baseUrl/jav-censored", page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val slug = URLEncoder.encode(query.trim().replace(Regex("""\s+"""), "-"), "UTF-8")
            .replace("+", "-")
        return GET(withPage("$baseUrl/search/$slug", page), headers)
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

    private fun withPage(base: String, page: Int): String =
        if (page <= 1) base else "${base.trimEnd('/')}/pg-$page"

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select(".movie-card")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href]") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!isDetailUrl(href)) return@mapNotNull null

                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst(".tray-item-title")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: link.attr("title").trim()
                    thumbnail_url = card.selectFirst("img.tray-item-thumbnail")?.absUrl("src")
                        ?: card.selectFirst("img")?.absUrl("src")
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.select("a[href]").any { it.text().contains("Next", true) }
        return AnimesPage(items, hasNext)
    }

    private fun isDetailUrl(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        val path = parsed.encodedPath.trim('/')
        if (path.contains('/')) return false
        if (path.isBlank() || path.contains('.')) return false
        return path !in SECTION_SLUGS && !path.startsWith("jav-") && !path.startsWith("pg-")
    }

    // --------------------------------------------------------------- details

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val genres = doc.select("a[href*='/genre/']")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
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

        // The player is injected by /ajax/player, so the raw page has no iframe yet.
        resolveAjaxPlayer(doc, pageUrl).takeIf { it.isNotEmpty() }?.let { return it }

        // Some entries expose a direct MP4 fallback before the f4scdn iframe.
        val direct = directVideos(doc)
        if (direct.isNotEmpty()) return direct

        val iframe = doc.selectFirst("iframe[src*='f4scdn.com/e/']")?.let {
            it.absUrl("src").ifBlank { it.attr("src") }
        } ?: return emptyList()
        return resolveF4s(iframe, pageUrl)
    }

    private fun resolveAjaxPlayer(doc: org.jsoup.nodes.Document, pageUrl: String): List<Video> {
        val html = doc.html()
        val pt = Regex("""window\.__pt\s*=\s*"([^"]+)"""").find(html)?.groupValues?.get(1)
            ?: return emptyList()
        val pk = Regex("""window\.__pk\s*=\s*"([^"]+)"""").find(html)?.groupValues?.get(1)
            ?: return emptyList()
        val filmId = Regex("""var\s+filmId\s*=\s*(\d+)""").find(html)?.groupValues?.get(1)
            ?: return emptyList()
        val episode = doc.selectFirst("button.btn-player[data-id]")?.attr("data-id").orEmpty()
            .ifBlank { "0" }
        val token = doc.selectFirst("meta#token")?.attr("value")
            ?: doc.selectFirst("meta[name=_token]")?.attr("value")
            ?: return emptyList()
        val socket = doc.selectFirst("meta#socket")?.attr("value")
            ?: doc.selectFirst("meta[name=_socket]")?.attr("value")
            ?: return emptyList()

        val authorization = "Basic " + Base64.encodeToString(
            "$token:$socket".toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP,
        )
        val body = FormBody.Builder()
            .add("episode", episode)
            .add("filmId", filmId)
            .add("pt", pt)
            .build()
        val request = Request.Builder()
            .url("$baseUrl/ajax/player")
            .post(body)
            .header("User-Agent", DESKTOP_UA)
            .header("Referer", pageUrl)
            .header("Origin", baseUrl)
            .header("Authorization", authorization)
            .header("X-Requested-With", "XMLHttpRequest")
            .build()

        val raw = runCatching {
            client.newCall(request).execute().use { it.body?.string() }
        }.getOrNull() ?: return emptyList()

        val encrypted = runCatching { JSONObject(raw).optString("player_enc") }.getOrNull()
            ?.takeIf { it.isNotBlank() } ?: return emptyList()
        val playerHtml = xorDecrypt(encrypted, pk)
        if (playerHtml.isBlank()) return emptyList()

        val playerDoc = Jsoup.parse(playerHtml, pageUrl)
        val direct = directVideos(playerDoc)
        if (direct.isNotEmpty()) return direct

        val iframe = playerDoc.selectFirst("iframe[src*='f4scdn.com/e/']")?.let {
            it.absUrl("src").ifBlank { it.attr("src") }
        } ?: return emptyList()
        return resolveF4s(iframe, pageUrl)
    }

    private fun resolveF4s(iframe: String, pageUrl: String): List<Video> {
        val embed = fetchPage(iframe, pageUrl) ?: return emptyList()
        val apiPath = Jsoup.parse(embed.first).selectFirst("[data-api]")?.attr("data-api")
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()

        val apiBase = if (apiPath.startsWith("http")) apiPath else "https://f4scdn.com$apiPath"
        val apiUrl = apiBase.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("ab", "0")
            ?.addQueryParameter("n", "1")
            ?.addQueryParameter("r", pageUrl)
            ?.build()?.toString() ?: return emptyList()

        val body = fetchText(apiUrl, "https://f4scdn.com/") ?: return emptyList()
        val src = runCatching { JSONObject(body).optString("src") }.getOrNull().orEmpty()
        val stream = src.takeIf { it.startsWith("http") } ?: return emptyList()

        val headers = Headers.Builder()
            .add("Referer", "https://f4scdn.com/")
            .add("Origin", "https://f4scdn.com")
            .build()
        return listOf(MediaVideo.create(stream, "HLS", stream, headers = headers))
    }

    private fun directVideos(doc: org.jsoup.nodes.Document): List<Video> =
        doc.select("video[src], video source[src], source[src]")
            .mapNotNull { it.absUrl("src").takeIf { url -> url.startsWith("http") } }
            .distinct()
            .map { MediaVideo.create(it, "MP4", it) }

    private fun xorDecrypt(encoded: String, key: String): String {
        if (encoded.isBlank() || key.isBlank()) return ""
        val bytes = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
            ?: return ""
        val out = CharArray(bytes.size) { i ->
            ((bytes[i].toInt() and 0xFF) xor key[i % key.length].code).toChar()
        }
        return String(out)
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

    private fun fetchText(url: String, referer: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", DESKTOP_UA)
            .header("Referer", referer)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { it.body?.string() }
    }.getOrNull()

    companion object {
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        private val SECTION_SLUGS = setOf(
            "genre", "genres", "search", "actress", "actresses", "studio", "studios",
            "private", "advertise", "login", "register", "logout", "favorites", "history",
            "contact", "dmca", "terms", "privacy", "page", "new", "latest", "recent",
        )
    }
}
