package com.aiohentai.cs.sites.muchohentai

import com.aiohentai.cs.compat.MediaVideo

import com.aiohentai.cs.compat.AnimesPage
import com.aiohentai.cs.compat.AnimeFilterList
import com.aiohentai.cs.compat.SAnime
import com.aiohentai.cs.compat.SEpisode
import com.aiohentai.cs.compat.Video
import com.aiohentai.cs.compat.AnimeHttpSource
import com.aiohentai.cs.compat.GET
import com.aiohentai.cs.compat.asJsoup
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray

class MuchoHentai : AnimeHttpSource() {

    override val name = "@JalebiBae - MuchoHentai"
    override val id: Long = 564716588487801074L
    override val baseUrl = "https://muchohentai.com"
    init {
        MediaVideo.referer = "$baseUrl/"
    }
    override val lang = "en"
    override val supportsLatest = true

    override fun popularAnimeRequest(page: Int): Request =
        GET(listUrl(page), headers)

    override fun popularAnimeParse(response: Response): AnimesPage = parseList(response)

    override fun latestUpdatesRequest(page: Int): Request =
        GET(listUrl(page), headers)

    override fun latestUpdatesParse(response: Response): AnimesPage = parseList(response)

    override fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        GET("$baseUrl/?s=${query.replace(" ", "+")}", headers)

    override fun searchAnimeParse(response: Response): AnimesPage = parseList(response)

    private fun listUrl(page: Int): String =
        if (page <= 1) {
            "$baseUrl/latest-hentai-posts/"
        } else {
            "$baseUrl/latest-hentai-posts/page/$page"
        }

    private fun parseList(response: Response): AnimesPage {
        val doc = response.asJsoup()
        val items = doc.select("div.item.item-video")
            .mapNotNull { item ->
                val link = item.selectFirst("a.clip-link[href]")
                    ?: item.selectFirst("h2.entry-title a[href]")
                    ?: return@mapNotNull null
                val href = link.absUrl("href").ifBlank { link.attr("href") }
                if (href.isBlank()) return@mapNotNull null

                SAnime.create().apply {
                    setUrlWithoutDomain(href)
                    title = link.attr("title").ifBlank { link.text() }
                    thumbnail_url = item.selectFirst("img")?.absUrl("src")
                }
            }
            .distinctBy { it.url }

        return AnimesPage(items, hasNextPage(doc))
    }

    private fun hasNextPage(doc: org.jsoup.nodes.Document): Boolean {
        val status = doc.selectFirst(".wp-pagenavi .pages")?.text() ?: return false
        val match = PAGE_STATUS.find(status) ?: return false
        val current = match.groupValues[1].toIntOrNull() ?: return false
        val total = match.groupValues[2].toIntOrNull() ?: return false
        return current < total
    }

    override fun animeDetailsParse(response: Response): SAnime {
        val doc = response.asJsoup()
        return SAnime.create().apply {
            title = doc.selectFirst("h1.entry-title")?.text().orEmpty()
            thumbnail_url = doc.selectFirst("meta[property=og:image]")?.attr("content")
            genre = doc.select("a[href*='/genre/']")
                .map { it.text() }
                .distinct()
                .joinToString(", ")
            description = doc.selectFirst("meta[name=description]")?.attr("content")
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
        val server = SERVER.find(html)?.groupValues?.get(1) ?: return emptyList()
        val filesJson = FILES.find(html)?.groupValues?.get(1) ?: return emptyList()

        val videos = mutableListOf<Video>()
        runCatching {
            val files = JSONArray(filesJson)
            for (i in 0 until files.length()) {
                val file = files.optJSONObject(i)?.optString("file").orEmpty()
                if (file.isBlank()) continue

                val url = if (file.startsWith("http")) {
                    file
                } else {
                    "https://$server.edge.tmncdn.io$file"
                }
                val quality = if (url.contains(".m3u8")) "HLS" else "MP4"
                videos += MediaVideo.create(url, quality, url)
            }
        }

        return videos.distinctBy { it.videoUrl }
    }

    companion object {
        private val PAGE_STATUS = Regex("""Page\s+(\d+)\s+of\s+(\d+)""")
        private val SERVER = Regex("""var\s+servers\s*=\s*\[\s*['"]([^'"]+)['"]""")
        private val FILES = Regex("""var\s+files\s*=\s*(\[[^\]]*\])\s*;""")
    }
}
