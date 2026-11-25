package com.aiohentai.cs.sites.hsex

import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.MediaVideo
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.asJsoup
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

/**
 * 好色 TV (hsex.icu). Chinese amateur/AV tube. Listing covers are CSS background
 * images; detail pages expose an HLS playlist in `source#video-source`.
 */
class Hsex : AnimeHttpSource() {

    override val name = "@JalebiBae - 好色TV"
    override val id: Long = 1846205573190457623L
    override val baseUrl = "https://hsex.icu"
    override val lang = "zh"
    override val supportsLatest = true

    init {
        MediaVideo.referer = "$baseUrl/"
    }

    override fun popularAnimeRequest(page: Int): Request =
        GET(if (page <= 1) "$baseUrl/top_list-1.htm" else "$baseUrl/top_list-$page.htm", headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(if (page <= 1) "$baseUrl/list-1.htm" else "$baseUrl/list-$page.htm", headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = if (page <= 1) "$baseUrl/search.htm?search=$encoded"
        else "$baseUrl/search.htm?search=$encoded&page=$page"
        return GET(url, headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun genrePageUrl(path: String, page: Int): String {
        val base = if (path.startsWith("/")) baseUrl.trimEnd('/') + path else path
        if (page <= 1) return base
        return if (base.contains("-1.htm")) base.replace("-1.htm", "-$page.htm")
        else "$base?page=$page"
    }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div.thumbnail")
            .mapNotNull { card ->
                val link = card.selectFirst("a[href^=video-]") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (!href.contains("video-")) return@mapNotNull null
                val style = card.selectFirst("div.image")?.attr("style").orEmpty()
                val poster = Regex("""url\(['"]?([^'")]+)['"]?\)""").find(style)
                    ?.groupValues?.get(1)
                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = card.selectFirst("h5 a")?.text()?.trim()
                        ?: link.attr("title").trim().takeIf { it.isNotBlank() }
                        ?: href.substringAfterLast('/')
                    thumbnail_url = poster
                        ?: card.selectFirst("img")?.absUrl("src")?.takeIf { it.startsWith("http") }
                }
            }
            .distinctBy { it.url }

        val hasNext = doc.select("a[href]").any {
            it.attr("href").contains("list-") && !it.attr("href").contains("-1.htm")
        } || items.size >= 20
        return AnimesPage(items, hasNext)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1")?.text()?.trim()
                ?: doc.title().substringBefore('-').trim()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
                ?: doc.selectFirst("video[poster]")?.absUrl("poster")
            genre = doc.select("a[href*=search.htm]")
                .map { it.text().replace("&nbsp;", "").trim() }
                .filter { it.isNotEmpty() && it.length < 20 }
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

    override fun videoListParse(response: Response): List<Video> {
        val doc = response.asJsoup()
        val urls = linkedSetOf<String>()
        doc.select("source[src], source[data-src]").forEach { source ->
            val raw = source.attr("src").ifBlank { source.attr("data-src") }
            if (raw.isBlank()) return@forEach
            urls += when {
                raw.startsWith("//") -> "https:$raw"
                raw.startsWith("http") -> raw
                raw.startsWith("/") -> baseUrl.trimEnd('/') + raw
                else -> raw
            }
        }
        if (urls.isEmpty()) {
            Regex("""https://[^"'\s]+\.m3u8[^"'\s]*""")
                .findAll(doc.html()).forEach { urls += it.value }
        }
        return urls.map { url ->
            val quality = if (url.contains(".m3u8", true)) "HLS" else "HD"
            MediaVideo.create(url, quality, url)
        }
    }
}
