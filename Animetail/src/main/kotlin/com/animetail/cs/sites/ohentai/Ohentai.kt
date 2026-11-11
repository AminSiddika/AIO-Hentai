package com.animetail.cs.sites.ohentai

import com.animetail.cs.compat.MediaVideo

import com.animetail.cs.compat.AnimesPage
import com.animetail.cs.compat.AnimeFilterList
import com.animetail.cs.compat.SAnime
import com.animetail.cs.compat.SEpisode
import com.animetail.cs.compat.Video
import com.animetail.cs.compat.AnimeHttpSource
import com.animetail.cs.compat.GET
import com.animetail.cs.compat.asJsoup
import okhttp3.Request
import okhttp3.Response
import java.net.URLEncoder

class Ohentai : AnimeHttpSource() {

    override val name = "@JalebiBae - Ohentai"
    override val id: Long = 7247837649602913976L
    override val baseUrl = "https://ohentai.org"
    init {
        MediaVideo.referer = "$baseUrl/"
    }
    override val lang = "en"
    override val supportsLatest = true

    override fun popularAnimeRequest(page: Int): Request =
        GET("$baseUrl/index.php?sort=most_viewed&p=$page", headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl/index.php?p=$page", headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request {
        val encoded = URLEncoder.encode(query, "UTF-8")
        return GET("$baseUrl/search.php?k=$encoded&p=$page", headers)
    }

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div.videobrick")
            .mapNotNull { brick ->
                val link = brick.selectFirst("a.interlink[href]") ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (href.isBlank()) return@mapNotNull null

                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = brick.selectFirst(".videotitle a")?.text()
                        ?.ifBlank { link.attr("title") }
                        ?: link.attr("title")
                    thumbnail_url = brick.selectFirst("img")?.absUrl("src")
                }
            }
            .distinctBy { it.url }

        val current = items.size
        val hasNext = current >= PAGE_SIZE
        return AnimesPage(items, hasNext)
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1.title")?.text().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            genre = doc.select(".subtag, .detailtag")
                .map { it.text().trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .joinToString(", ")
            description = doc.selectFirst("meta[property=og:description]")?.attr("content")
            status = SAnime.COMPLETED
            initialized = true
        }
    }

    override fun episodeListParse(response: Response): List<SEpisode> {
        val url = response.request.url.toString()
        return listOf(
            SEpisode.create().apply {
                setUrlWithoutDomain(url)
                name = "Full Video"
                episode_number = 1f
                date_upload = 0L
            },
        )
    }

    override fun videoListParse(response: Response): List<Video> {
        val html = response.body?.string().orEmpty()
        return FILE_URL.findAll(html)
            .map { it.groupValues[1].replace("\\/", "/") }
            .filter { it.contains(".mp4") }
            .distinct()
            .map { MediaVideo.create(it, "MP4", it) }
            .toList()
    }

    companion object {
        private const val PAGE_SIZE = 24
        private val FILE_URL = Regex(""""file"\s*:\s*"([^"]+)"""")
    }
}
