package com.aiohentai.cs.compat

import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Same contract as Aniyomi's `AnimeHttpSource`, but implemented on top of the plugin's
 * own OkHttp client. Every scraper in this plugin only implements the request/parse pair
 * methods, so this is the whole surface that has to exist.
 */
abstract class AnimeHttpSource {

    abstract val name: String
    abstract val baseUrl: String
    open val id: Long = 0L
    open val lang: String = "en"
    open val supportsLatest: Boolean = false

    open val client: OkHttpClient get() = httpClient
    open val headers: Headers get() = defaultHeaders

    /** Opt-in proxy flag carried over from the Aniyomi extensions. */
    var mediaUseProxy: Boolean = false

    /**
     * Extra browse rows for the home screen: display name -> site path.
     * Static because CloudStream reads `mainPage` synchronously.
     */
    open val genres: List<Pair<String, String>>
        get() = Genres.bySite[javaClass.packageName.substringAfterLast('.')].orEmpty()

    open fun genrePageUrl(path: String, page: Int): String {
        val base = when {
            path.startsWith("http://") || path.startsWith("https://") -> path
            path.startsWith("/") -> baseUrl.trimEnd('/') + path
            else -> baseUrl.trimEnd('/') + "/" + path
        }
        if (page <= 1) return base
        return when {
            base.contains("?") -> "$base&page=$page"
            base.endsWith("/") -> "${base}page/$page/"
            else -> "$base/page/$page/"
        }
    }

    open fun popularAnimeRequest(page: Int): Request =
        throw NotImplementedError("popularAnimeRequest")

    open fun popularAnimeParse(response: Response): AnimesPage =
        throw NotImplementedError("popularAnimeParse")

    open fun latestUpdatesRequest(page: Int): Request =
        throw NotImplementedError("latestUpdatesRequest")

    open fun latestUpdatesParse(response: Response): AnimesPage =
        throw NotImplementedError("latestUpdatesParse")

    open fun searchAnimeRequest(page: Int, query: String, filters: AnimeFilterList): Request =
        throw NotImplementedError("searchAnimeRequest")

    open fun searchAnimeParse(response: Response): AnimesPage =
        throw NotImplementedError("searchAnimeParse")

    open fun animeDetailsParse(response: Response): SAnime =
        throw NotImplementedError("animeDetailsParse")

    open fun episodeListParse(response: Response): List<SEpisode> =
        throw NotImplementedError("episodeListParse")

    open fun videoListRequest(episode: SEpisode): Request =
        GET("$baseUrl${episode.url}", headers)

    open fun videoListParse(response: Response): List<Video> =
        throw NotImplementedError("videoListParse")

    open fun getFilterList(): AnimeFilterList = AnimeFilterList()

    // ------------------------------------------------------------------ helpers

    fun fetch(request: Request): FetchedPage {
        client.newCall(request).execute().use { response ->
            val body = response.body
            val bytes = body?.bytes() ?: ByteArray(0)
            val mediaType = body?.contentType()
            return FetchedPage(response.request.url.toString(), bytes, mediaType, response)
        }
    }

    fun fetch(url: String): FetchedPage = fetch(GET(url, headers))

    fun popularAnime(page: Int): AnimesPage =
        parse(popularAnimeRequest(page)) { popularAnimeParse(it) }

    fun latestUpdates(page: Int): AnimesPage =
        parse(latestUpdatesRequest(page)) { latestUpdatesParse(it) }

    fun searchAnime(page: Int, query: String, filters: AnimeFilterList): AnimesPage =
        parse(searchAnimeRequest(page, query, filters)) { searchAnimeParse(it) }

    fun details(page: FetchedPage): SAnime = animeDetailsParse(page.response())

    fun episodes(page: FetchedPage): List<SEpisode> = episodeListParse(page.response())

    fun videos(page: FetchedPage): List<Video> = videoListParse(page.response())

    private fun <T> parse(request: Request, fn: (Response) -> T): T =
        fn(fetch(request).response())
}
