package com.aiohentai.cs.compat

import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newSearchResponseList
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Glue between a ported Aniyomi source and CloudStream's provider contract.
 *
 * Every source is either "one video per page" (the page URL is the only episode) or a
 * series page that exposes several episodes. That is decided at load time from the
 * episode list, so no per-source configuration is needed.
 */
open class AniyomiProvider(
    val src: AnimeHttpSource,
) : MainAPI() {

    override var name: String = src.name
    override var mainUrl: String = src.baseUrl
    override var lang: String = src.lang
    override var canBeOverridden: Boolean = true

    override val hasMainPage = true
    override val hasQuickSearch = false
    override val hasDownloadSupport = true
    // Most sources hand out localhost proxies or hotlink-protected URLs.
    override val hasChromecastSupport = false

    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.AsianDrama,
    )

    override val mainPage = mainPageOf(
        "popular" to "Popular",
        "latest" to "Latest",
    )

    // ------------------------------------------------------------------ home

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest,
    ): HomePageResponse = withContext(Dispatchers.IO) {
        prepare()
        val result = if (request.data == "latest") {
            src.latestUpdates(page)
        } else {
            src.popularAnime(page)
        }
        newHomePageResponse(
            request.name,
            result.animes.mapNotNull { it.toSearchResult() },
            result.hasNextPage,
        )
    }

    // ---------------------------------------------------------------- search

    override suspend fun search(query: String, page: Int): SearchResponseList =
        withContext(Dispatchers.IO) {
            prepare()
            val result = src.searchAnime(page, query, AnimeFilterList())
            newSearchResponseList(
                result.animes.mapNotNull { it.toSearchResult() },
                result.hasNextPage,
            )
        }

    // ------------------------------------------------------------------ load

    override suspend fun load(url: String): LoadResponse = withContext(Dispatchers.IO) {
        prepare()
        val page = src.fetch(url)
        val details = src.details(page)
        val episodes = runCatching { src.episodes(page) }.getOrDefault(emptyList())

        val title = details.title.ifBlank { "Untitled" }
        val poster = abs(details.thumbnail_url)
        val plot = details.description
        val tags = details.genre
            ?.split(',', '/')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.takeIf { it.isNotEmpty() }

        if (episodes.size <= 1) {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = plot
                this.tags = tags
            }
        } else {
            val list = episodes.map { ep ->
                newEpisode(abs(ep.url) ?: url) {
                    this.name = ep.name.ifBlank { "Episode" }
                    this.season = 1
                    this.episode = ep.episode_number.toInt().takeIf { it > 0 }
                    if (ep.date_upload > 0L) this.date = ep.date_upload
                }
            }
            newTvSeriesLoadResponse(title, url, TvType.Anime, list) {
                this.posterUrl = poster
                this.plot = plot
                this.tags = tags
            }
        }
    }

    // ----------------------------------------------------------------- links

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        prepare()
        val page = src.fetch(data)
        val videos = runCatching { src.videos(page) }.getOrDefault(emptyList())

        val seen = HashSet<String>()
        for (video in videos) {
            val link = video.videoUrl.ifBlank { video.url }
            if (link.isBlank() || !seen.add(link)) continue

            callback.invoke(
                newExtractorLink(src.name, video.quality, link) {
                    this.referer = src.baseUrl.trimEnd('/') + "/"
                    this.quality = qualityOf(video.quality)
                    val headers = video.headers?.toMap()
                    if (headers != null) this.headers = headers
                },
            )
        }
        videos.isNotEmpty()
    }

    // --------------------------------------------------------------- helpers

    /** Restores the per-provider globals the Aniyomi sources used to set in their init. */
    private fun prepare() {
        MediaVideo.referer = src.baseUrl.trimEnd('/') + "/"
        MediaVideo.useProxy = src.mediaUseProxy
    }

    private fun SAnime.toSearchResult(): SearchResponse? {
        val href = abs(url) ?: return null
        return newMovieSearchResponse(title.ifBlank { "Untitled" }, href, TvType.Movie) {
            this.posterUrl = abs(thumbnail_url)
        }
    }

    private fun abs(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim()
        return when {
            value.startsWith("http://") || value.startsWith("https://") -> value
            value.startsWith("//") -> "https:$value"
            value.startsWith("/") -> src.baseUrl.trimEnd('/') + value
            else -> src.baseUrl.trimEnd('/') + "/" + value
        }
    }

    private fun qualityOf(label: String): Int {
        val named = getQualityFromName(label)
        if (named != Qualities.Unknown.value) return named
        val lower = label.lowercase()
        return when {
            lower.contains("2160") || lower.contains("4k") -> Qualities.P2160.value
            lower.contains("1440") -> Qualities.P1440.value
            lower.contains("1080") || lower.contains("fhd") -> Qualities.P1080.value
            lower.contains("720") || lower.contains(" hd") -> Qualities.P720.value
            lower.contains("480") -> Qualities.P480.value
            lower.contains("360") -> Qualities.P360.value
            lower.contains("240") -> Qualities.P240.value
            else -> Qualities.Unknown.value
        }
    }
}
