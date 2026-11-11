package com.aiohentai.cs.sites.javgg

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
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLEncoder

/**
 * JAVGG. The detail page embeds an f4jav player (`/api/play/<id>`) and a VidHide-style
 * embed whose packed JS expands to hls2/hls4. Both paths are supported; f4jav is tried
 * first because it returns a direct HLS master.
 */
class Javgg : AnimeHttpSource() {

    override val name = "@JalebiBae - JavGG"
    override val id: Long = 3410668376988175721L
    override val baseUrl = "https://javgg.net"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    @Volatile
    private var searchQuery: String = ""

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request =
        GET(withPage("$baseUrl/trending/", page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(withPage("$baseUrl/new-post/", page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        searchQuery = query
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/?s=$encoded" else "$baseUrl/page/$page/?s=$encoded"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val page = parseList(doc, response.request.url.toString())
        if (page.animes.isNotEmpty() || searchQuery.isBlank()) return page
        return searchViaApi(doc.html())
    }

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
        return "${base.trimEnd('/')}/page/$page/"
    }

    private fun parseList(response: Response): AnimesPage =
        parseList(response.asJsoup(), response.request.url.toString())

    private fun parseList(doc: org.jsoup.nodes.Document, url: String): AnimesPage {
        val items = doc.select("article.item.movies")
            .mapNotNull { card ->
                val link = card.selectFirst("div.data h3 a[href]")
                    ?: card.selectFirst("a[href]")
                    ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("/jav/")) return@mapNotNull null

                val image = card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst(".titlecontent")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: link.text().trim()
                    thumbnail_url = image?.absUrl("data-src")?.takeIf { it.isNotBlank() }
                        ?: image?.absUrl("src")
                }
            }
            .distinctBy { it.url }

        val current = Regex("""/page/(\d+)/""").find(url)
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1
        val hasNext = doc.select("a[href]").any {
            it.attr("href").contains("/page/${current + 1}/")
        }
        return AnimesPage(items, hasNext)
    }

    // --------------------------------------------------------------- details

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val genres = doc.select("a[href*='/tag/']")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        return SAnime.create().apply {
            title = doc.select("h1").firstOrNull { it.text().isNotBlank() }?.text()?.trim()
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
        val iframes = doc.select("iframe[src]")
            .mapNotNull { it.absUrl("src").ifBlank { it.attr("src") } }
            .filter { it.startsWith("http") }
            .distinct()

        iframes.firstOrNull { it.contains("f4jav.xyz/e/") }?.let {
            resolveF4(it, pageUrl).takeIf { videos -> videos.isNotEmpty() }?.let { videos -> return videos }
        }
        iframes.firstOrNull { it.contains("earnvidjavgg.xyz/embed/") }?.let {
            resolveVidhide(it, pageUrl).takeIf { videos -> videos.isNotEmpty() }?.let { videos -> return videos }
        }
        return emptyList()
    }

    private fun resolveF4(iframe: String, pageUrl: String): List<Video> {
        val embed = fetchPage(iframe, pageUrl) ?: return emptyList()
        val apiPath = Jsoup.parse(embed.first).selectFirst("[data-api]")?.attr("data-api")
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()

        val origin = originOf(embed.second)
        val apiBase = if (apiPath.startsWith("http")) apiPath else "$origin$apiPath"
        val apiUrl = apiBase.toHttpUrlOrNull()?.newBuilder()
            ?.addQueryParameter("ab", "0")
            ?.addQueryParameter("n", "1")
            ?.addQueryParameter("r", pageUrl)
            ?.build()?.toString() ?: return emptyList()

        val body = fetchText(apiUrl, "$origin/") ?: return emptyList()
        val src = runCatching { JSONObject(body).optString("src") }.getOrNull().orEmpty()
        val stream = src.takeIf { it.startsWith("http") } ?: return emptyList()

        val headers = Headers.Builder()
            .add("Referer", "$origin/")
            .add("Origin", origin)
            .build()
        return listOf(MediaVideo.create(stream, "HLS", stream, headers = headers))
    }

    private fun resolveVidhide(iframe: String, pageUrl: String): List<Video> {
        val embed = fetchPage(iframe, pageUrl) ?: return emptyList()
        val decoded = unpackAll(embed.first)
        val map = HLS_PAIR.findAll(decoded)
            .associate { it.groupValues[1] to it.groupValues[2] }
        val chosen = listOf("hls4", "hls3", "hls2")
            .firstNotNullOfOrNull { key -> map[key] }
            ?: M3U8.find(decoded)?.value
            ?: return emptyList()
        val stream = absolute(chosen, embed.second)
        val origin = originOf(embed.second)
        val headers = Headers.Builder()
            .add("Referer", embed.second)
            .add("Origin", origin)
            .build()
        return listOf(MediaVideo.create(stream, "HLS", stream, headers = headers))
    }

    private fun searchViaApi(html: String): AnimesPage {
        val nonce = Regex(""""nonce"\s*:\s*"([^"]+)"""").find(html)?.groupValues?.get(1)
            ?: return AnimesPage(emptyList(), false)
        val keyword = URLEncoder.encode(searchQuery, "UTF-8")
        val url = "$baseUrl/wp-json/dooplay/search/?keyword=$keyword&nonce=$nonce"
        val body = runCatching {
            client.newCall(GET(url, headers)).execute().use { it.body?.string() }
        }.getOrNull() ?: return AnimesPage(emptyList(), false)

        val objectJson = runCatching { JSONObject(body) }.getOrNull()
            ?: return AnimesPage(emptyList(), false)
        val items = buildList {
            val keys = objectJson.keys()
            while (keys.hasNext()) {
                val item = objectJson.optJSONObject(keys.next()) ?: continue
                val href = item.optString("url")
                if (!href.startsWith("http")) continue
                add(
                    SAnime.create().apply {
                        setUrlWithoutDomain(href)
                        title = item.optString("title")
                        thumbnail_url = item.optString("img").takeIf { it.startsWith("http") }
                    },
                )
            }
        }.distinctBy { it.url }
        return AnimesPage(items, false)
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

        private val HLS_PAIR = Regex("""["'](hls\d+)["']\s*:\s*["']([^"']+)["']""")
        private val PACKED = Regex(
            """\}\('(.*?)',(\d+),(\d+),'(.*?)'\.split\('\|'\)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val TOKEN = Regex("""\b[0-9a-z]+\b""")
        private val M3U8 = Regex("""https?://[^'"\\\s]+\.m3u8[^'"\\\s]*""")
    }
}
