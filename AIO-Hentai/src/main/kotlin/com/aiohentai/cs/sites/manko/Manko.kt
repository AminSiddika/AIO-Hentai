package com.aiohentai.cs.sites.manko

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.MediaVideo
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.appHttpClient
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Manko.fun. The SPA reads an API on healertanker.com whose `data` field is encoded with
 * a custom base-91-style packer (ported from the site's build.js). Decoded list/detail
 * JSON exposes `wasabi_url_1` (HLS) and `missav_video`.
 */
class Manko : AnimeHttpSource() {

    override val name = "@JalebiBae - Manko"
    override val id: Long = 1207731096531845053L
    override val baseUrl = "https://manko.fun"
    override val lang = "en"
    override val supportsLatest = true

    override val client: OkHttpClient
        get() = appHttpClient()

    private val apiBase = "https://healertanker.com"

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    override fun popularAnimeRequest(page: Int): Request =
        GET("$apiBase/swx/movie/search?page=$page&size=24&most_popular=true&can_manko=true", headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseMovies(response, 1)

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$apiBase/swx/movie/search?page=$page&size=24&can_manko=true", headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseMovies(response, 1)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return GET("$apiBase/swx/movie/search?page=$page&size=24&can_manko=true&keyword=$encoded", headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseMovies(response, 1)

    private fun parseMovies(response: Response, fallbackPage: Int): AnimesPage {
        val body = response.body?.string().orEmpty()
        val root = runCatching { JSONObject(body) }.getOrNull()
            ?: return AnimesPage(emptyList(), false)
        val encoded = root.optString("data")
        val array = runCatching { JSONArray(decode(encoded)) }.getOrNull()
            ?: return AnimesPage(emptyList(), false)

        val items = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("_id")
                if (id.isBlank()) continue
                add(
                    SAnime.create().apply {
                        url = "/movie-info/$id?series=false"
                        title = titleOf(item)
                        thumbnail_url = item.optString("thumbnail_image").takeIf { it.startsWith("http") }
                            ?: item.optString("wide_thumbnail").takeIf { it.startsWith("http") }
                    },
                )
            }
        }.distinctBy { it.url }

        val page = Regex("""[?&]page=(\d+)""").find(response.request.url.toString())
            ?.groupValues?.get(1)?.toIntOrNull() ?: fallbackPage
        val total = root.optInt("total", 0)
        return AnimesPage(items, total > page * 24)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val detail = detail(response.request.url.toString())
        return SAnime.create().apply {
            title = detail.let { titleOf(it) }
            thumbnail_url = detail.optString("cover_image").takeIf { it.startsWith("http") }
            description = descriptionOf(detail)
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
        val detail = detail(response.request.url.toString())
        val urls = listOf("wasabi_url_1", "missav_video")
            .mapNotNull { key -> detail.optString(key).takeIf { it.startsWith("http") } }
            .distinct()
        val headers = Headers.Builder()
            .add("Referer", "$baseUrl/")
            .add("Origin", baseUrl)
            .build()
        return urls.map { MediaVideo.create(it, "HLS", it, headers = headers) }
    }

    private fun detail(pageUrl: String): JSONObject {
        val id = Regex("""/movie-info/([0-9a-fA-F]+)""").find(pageUrl)?.groupValues?.get(1)
            ?: return JSONObject()
        val body = runCatching {
            client.newCall(GET("$apiBase/swx/movie/detail/$id", headers)).execute()
                .use { it.body?.string().orEmpty() }
        }.getOrNull().orEmpty()
        val root = runCatching { JSONObject(body) }.getOrNull() ?: return JSONObject()
        val encoded = root.optString("data")
        return runCatching { JSONObject(decode(encoded)) }.getOrNull() ?: JSONObject()
    }

    private fun titleOf(item: JSONObject): String {
        val titles = item.optJSONArray("title_display_name")
        if (titles != null) {
            for (i in 0 until titles.length()) {
                val title = titles.optJSONObject(i) ?: continue
                if (title.optString("language").equals("en", true)) {
                    title.optString("title").takeIf { it.isNotBlank() }?.let { return it }
                }
            }
            titles.optJSONObject(0)?.optString("title")?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return item.optString("title")
    }

    private fun descriptionOf(item: JSONObject): String? {
        val descriptions = item.optJSONArray("discription") ?: return null
        for (i in 0 until descriptions.length()) {
            val description = descriptions.optJSONObject(i) ?: continue
            if (description.optString("language").equals("en", true)) {
                description.optString("overview").takeIf { it.isNotBlank() }?.let { return it }
            }
        }
        return descriptions.optJSONObject(0)?.optString("overview")?.takeIf { it.isNotBlank() }
    }

    /** Port of the site's `xn` decoder from build.js. */
    private fun decode(input: String): String {
        val out = java.io.ByteArrayOutputStream()
        var accumulator = 0
        var bits = 0
        var pending = -1

        for (ch in input) {
            val value = ALPHABET.indexOf(ch)
            if (value < 0) continue
            if (pending < 0) {
                pending = value
                continue
            }
            pending += 91 * value
            accumulator = accumulator or (pending shl bits)
            bits += if ((pending and 8191) > 88) 13 else 14
            do {
                out.write(accumulator and 0xFF)
                accumulator = accumulator shr 8
                bits -= 8
            } while (bits > 7)
            pending = -1
        }
        if (pending > -1) out.write((accumulator or (pending shl bits)) and 0xFF)
        return out.toByteArray().toString(Charsets.UTF_8)
    }

    companion object {
        private const val ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
                "!#$%&()*+,./:;<=>?@[]^_`{|}~\""
    }
}
