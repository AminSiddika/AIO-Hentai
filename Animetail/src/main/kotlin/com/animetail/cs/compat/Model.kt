package com.animetail.cs.compat

/**
 * Minimal stand-ins for the Aniyomi extension model classes the scrapers were written
 * against. Keeping the same field names lets the original parse code move over with
 * import-only changes, so the site selectors and decoders stay byte-for-byte intact.
 */

class SAnime {
    var url: String = ""
    var title: String = ""
    var artist: String? = null
    var author: String? = null
    var description: String? = null
    var genre: String? = null
    var status: Int = UNKNOWN
    var thumbnail_url: String? = null
    var background_url: String? = null
    var initialized: Boolean = false

    fun setUrlWithoutDomain(url: String) {
        this.url = toSiteRelative(url)
    }

    fun getUrlWithoutDomain(): String = url

    companion object {
        const val UNKNOWN = 0
        const val ONGOING = 1
        const val COMPLETED = 2
        const val LICENSED = 3
        const val PUBLISHING_FINISHED = 4
        const val CANCELLED = 5
        const val ON_HIATUS = 6

        fun create(): SAnime = SAnime()
    }
}

class SEpisode {
    var url: String = ""
    var name: String = ""
    var date_upload: Long = 0L
    var episode_number: Float = 0f
    var scanlator: String? = null
    var filler: Boolean = false

    fun setUrlWithoutDomain(url: String) {
        this.url = toSiteRelative(url)
    }

    fun getUrlWithoutDomain(): String = url

    companion object {
        fun create(): SEpisode = SEpisode()
    }
}

class Video(
    var url: String,
    var quality: String,
    var videoUrl: String = url,
    var headers: okhttp3.Headers? = null,
)

class AnimesPage(
    val animes: List<SAnime>,
    val hasNextPage: Boolean,
)

open class AnimeFilter {

    open class Select<T>(
        val name: String,
        val values: Array<T>,
    ) : AnimeFilter() {
        var state: Int = 0
        var selections: List<T>? = null
    }

    open class CheckBox(
        val name: String,
        var state: Boolean = false,
    ) : AnimeFilter()

    open class TriState(
        val name: String,
        var state: State = State.IGNORE,
    ) : AnimeFilter() {
        enum class State { IGNORE, INCLUDE, EXCLUDE }
    }

    open class Text(
        val name: String,
        var state: String = "",
    ) : AnimeFilter()

    open class Group(
        val name: String,
        val state: List<AnimeFilter> = emptyList(),
    ) : AnimeFilter()

    open class Separator(val name: String) : AnimeFilter()

    open class Header(val name: String) : AnimeFilter()

    open class Sort(
        val name: String,
        val values: Array<String> = emptyArray(),
        var state: Int? = null,
    ) : AnimeFilter()
}

class AnimeFilterList(vararg filters: AnimeFilter) : ArrayList<AnimeFilter>(filters.toList())

/**
 * Aniyomi stores a site-relative path on models. The plugin keeps that representation:
 * an absolute URL on the source's own host becomes "/path", anything else is left alone.
 */
internal fun toSiteRelative(url: String): String {
    if (url.startsWith("http://") || url.startsWith("https://")) {
        val afterScheme = url.substringAfter("://")
        val path = afterScheme.substringAfter('/', "")
        return if (path.isEmpty()) "/" else "/$path"
    }
    return if (url.startsWith("/")) url else "/$url"
}
