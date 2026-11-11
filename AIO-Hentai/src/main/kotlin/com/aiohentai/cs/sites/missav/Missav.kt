package com.aiohentai.cs.sites.missav

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.MediaVideo
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.asJsoup
import com.aiohentai.cs.compat.appHttpClient
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

/**
 * MissAV. The origin is behind Cloudflare; [headers] replays the WebView cookie jar and
 * user agent after the user solves the challenge in CloudStream's browser.
 *
 * The player is a JavaScript packer blob (`eval(function(p,a,c,k,e,d){...})`) that expands
 * to `source`, `source842` and `source1280` m3u8 URLs on the stream host.
 */
class Missav : AnimeHttpSource() {

    override val name = "@JalebiBae - MissAV"
    override val id: Long = 6127726159505648113L
    override val baseUrl = "https://missav.ws"
    override val lang = "en"
    override val supportsLatest = true

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    /**
     * In the app this routes through CloudStream's CloudflareKiller; on a plain JVM it
     * falls back to the normal client.
     */
    override val client: OkHttpClient
        get() = appHttpClient()

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request = GET(pageUrl("today-hot", page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request = GET(pageUrl("new", page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val base = "$baseUrl/en/search/$encoded"
        return GET(if (page <= 1) base else "$base?page=$page", headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        val base = when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/") -> baseUrl.trimEnd('/') + path
            else -> baseUrl.trimEnd('/') + "/" + path
        }
        return if (page <= 1) base else "$base?page=$page"
    }

    private fun pageUrl(section: String, page: Int): String =
        if (page <= 1) "$baseUrl/en/$section" else "$baseUrl/en/$section?page=$page"

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div.thumbnail.group")
            .mapNotNull { card ->
                val link = card.selectFirst("a[alt][href]")
                    ?: card.selectFirst("a[href]")
                    ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!isDetailUrl(href)) return@mapNotNull null

                val image = card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst("div.my-2 a")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: link.attr("alt").trim()
                    thumbnail_url = image?.absUrl("data-src")?.takeIf { it.isNotBlank() }
                        ?: image?.absUrl("src")
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.select("a[href]").any { it.text().trim().equals("Next", true) }
        return AnimesPage(items, hasNext)
    }

    private fun isDetailUrl(url: String): Boolean {
        if (url.isBlank() || !url.startsWith("http")) return false
        val path = url.substringAfter("://").substringAfter('/', "").trimEnd('/')
        val slug = path.substringAfterLast('/')
        return slug.isNotBlank() && !slug.contains('.') && slug !in SECTION_SLUGS
    }

    // --------------------------------------------------------------- details

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        val genres = doc.select("a[href*='/genres/']")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() && !it.equals("VR", true) }
            .distinct()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?: doc.selectFirst("img[data-src*='/cover-']")?.absUrl("data-src")
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
        val doc = response.asJsoup()
        val urls = linkedSetOf<String>()

        for (script in doc.select("script")) {
            val data = script.data()
            if (!data.contains("eval(function(p,a,c,k,e,d)")) continue
            urls += decodePacked(data)
        }

        if (urls.isEmpty()) {
            M3U8.findAll(doc.html()).forEach { urls += it.value }
        }

        val videoHeaders = Headers.Builder()
            .add("Referer", "$baseUrl/")
            .add("Origin", baseUrl)
            .build()

        return urls.mapNotNull { raw ->
            val url = raw.replace("\\/", "/").replace("&amp;", "&").trim()
            if (!url.startsWith("http")) return@mapNotNull null
            val quality = when {
                url.contains("/1080p/") -> "1080p"
                url.contains("/720p/") -> "720p"
                url.contains("/480p/") -> "480p"
                url.contains("/360p/") -> "360p"
                else -> "HLS"
            }
            MediaVideo.create(url, quality, url, headers = videoHeaders)
        }.distinctBy { it.url }
    }

    /** Expands the `eval(function(p,a,c,k,e,d){...})` blob and returns its m3u8 URLs. */
    private fun decodePacked(script: String): List<String> {
        val match = PACKED.find(script) ?: return emptyList()
        val payload = match.groupValues[1]
        val base = match.groupValues[2].toIntOrNull() ?: return emptyList()
        val words = match.groupValues[4].split('|')

        val decoded = TOKEN.replace(payload) { token ->
            val index = token.value.toIntOrNull(base)
            if (index != null && index in words.indices) words[index] else token.value
        }
        return M3U8.findAll(decoded).map { it.value }.toList()
    }

    companion object {
        private val PACKED = Regex(
            """\}\('(.*?)',(\d+),(\d+),'(.*?)'\.split\('\|'\)""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val TOKEN = Regex("""\b[0-9a-z]+\b""")
        private val M3U8 = Regex("""https?://[^'"\\\s]+\.m3u8[^'"\\\s]*""")

        private val SECTION_SLUGS = setOf(
            "new", "release", "today-hot", "weekly-hot", "monthly-hot",
            "search", "genres", "actresses", "makers", "vip", "saved",
            "playlists", "history", "klive", "clive", "english-subtitle",
            "uncensored-leak", "siro", "luxu", "gana", "maan", "scute", "ara",
            "fc2", "heyzo", "tokyohot", "1pondo", "caribbeancom", "caribbeancompr",
            "10musume", "pacopacomama", "gachinco", "xxxav", "marriedslash",
            "naughty4610", "naughty0930", "madou", "twav", "furuke", "ranking",
        )
    }
}
