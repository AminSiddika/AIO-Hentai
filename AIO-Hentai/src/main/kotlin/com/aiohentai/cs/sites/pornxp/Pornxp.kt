package com.aiohentai.cs.sites.pornxp

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.MediaVideo
import com.aiohentai.cs.compat.MirrorPool
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.asJsoup
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

/**
 * PornXP. The brand rotates domains (pxp.pink, xp.com.se, pxp.farm, porn-xp.eu) and the
 * media sits on separate `*.pornxp.sh` hosts, so browse URLs use the mirror pool while
 * playback URLs are taken verbatim from the `<source>` tags.
 */
class Pornxp : AnimeHttpSource() {

    override val name = "@JalebiBae - PornXP"
    override val id: Long = 2905639814208153907L
    private val mirrors = MirrorPool(
        primary = "https://pxp.pink",
        alternates = listOf("https://xp.com.se", "https://pxp.farm"),
        marker = "PornXP",
    )
    override val baseUrl: String
        get() = mirrors.active
    override val lang = "en"
    override val supportsLatest = true

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    // ---------------------------------------------------------------- browse

    override fun popularAnimeRequest(page: Int): Request {
        ensureBase()
        return GET(if (page <= 1) "$baseUrl/best/" else "$baseUrl/best/?page=$page", headers)
    }

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request {
        ensureBase()
        return GET(if (page <= 1) "$baseUrl/released/" else "$baseUrl/released/?page=$page", headers)
    }

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        ensureBase()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/?q=$encoded" else "$baseUrl/?q=$encoded&page=$page"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        ensureBase()
        val base = if (path.startsWith("/")) baseUrl.trimEnd('/') + path else path
        return if (page <= 1) base else "$base&page=$page"
    }

    private fun ensureBase() {
        mirrors.base(client, headers)
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div.item_cont")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href^=/videos/]") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("/videos/")) return@mapNotNull null
                val img = card.selectFirst("img.item_img") ?: card.selectFirst("img")
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst(".item_title")?.text()?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?: img?.attr("alt")?.trim().orEmpty()
                    thumbnail_url = img?.absUrl("data-src")?.takeIf { it.startsWith("http") }
                        ?: img?.absUrl("src")?.takeIf { it.startsWith("http") }
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.select("a[href]").any { it.attr("href").contains("page=") } || items.size >= 20
        return AnimesPage(items, hasNext)
    }

    // --------------------------------------------------------------- details

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst(".player_details h1")?.text()?.trim()
                ?: doc.selectFirst("h1")?.text()?.trim()
                ?: doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim().orEmpty()
            thumbnail_url = doc.selectFirst("video[poster]")?.absUrl("poster")
                ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
            genre = doc.select("a[href*='/tags/']")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
            description = doc.selectFirst("#desc")?.text()?.trim()
                ?: doc.selectFirst("meta[name=description]")?.attr("content")
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
        val videos = mutableListOf<Video>()
        doc.select("video#player source, video source").forEach { source ->
            val raw = source.attr("src").ifBlank { source.attr("data-src") }
            if (raw.isBlank()) return@forEach
            val url = when {
                raw.startsWith("//") -> "https:$raw"
                raw.startsWith("http") -> raw
                raw.startsWith("/") -> baseUrl.trimEnd('/') + raw
                else -> return@forEach
            }
            val quality = source.attr("title").trim().ifBlank {
                Regex("""/(\d+)p?\.mp4""").find(url)?.groupValues?.get(1)?.plus("p") ?: "HD"
            }
            videos += MediaVideo.create(url, quality, url)
        }
        return videos.distinctBy { it.url }
    }
}
