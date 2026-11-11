package com.aiohentai.cs.smoke

import com.aiohentai.cs.compat.AniyomiProvider
import com.aiohentai.cs.compat.AnimeHttpSource
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.TvSeriesLoadResponse
import com.lagradost.cloudstream3.MovieLoadResponse
import kotlinx.coroutines.runBlocking

/**
 * Provider-level smoke test: runs the exact CloudStream entry points (getMainPage, search,
 * load, loadLinks) through the AniyomiProvider adapter, so a mapping bug shows up here
 * instead of on a phone.
 *
 * Usage: ProviderSmokeKt [sourceName ...] [--url <pageUrl>] [--episode <n>]
 */
fun main(args: Array<String>) {
    val urlIndex = args.indexOf("--url")
    val fixedUrl = if (urlIndex >= 0) args.getOrNull(urlIndex + 1) else null
    val genreMode = args.contains("--genres")
    val episodeIndex = (args.indexOf("--episode") + 1).takeIf { it > 0 }?.let { args.getOrNull(it)?.toIntOrNull() } ?: 0
    val filters = args.filterIndexed { i, a ->
        !a.startsWith("--") && !(urlIndex >= 0 && i == urlIndex + 1) && !(args.indexOf("--episode") >= 0 && i == args.indexOf("--episode") + 1)
    }.toSet()

    val sites = smokeSites().filter { filters.isEmpty() || it.first in filters }
    var ok = 0
    var failed = 0

    for ((name, factory) in sites) {
        val src: AnimeHttpSource = factory()
        val provider = AniyomiProvider(src)

        if (genreMode) {
            if (src.genres.isEmpty()) continue
            val result = runBlocking {
                runCatching {
                    for ((genreName, path) in src.genres) {
                        val page = provider.getMainPage(
                            1,
                            MainPageRequest(genreName, "genre|$path", false),
                        )
                        val count = page.items.sumOf { it.list.size }
                        println("[$name] genre '$genreName' ($path): $count")
                    }
                }
            }
            if (result.isSuccess) ok++ else {
                failed++
                println("[$name] GENRE FAIL ${result.exceptionOrNull()?.message?.take(120)}")
            }
            continue
        }

        val result = runBlocking {
            runCatching {
                val target = fixedUrl ?: run {
                    val home = provider.getMainPage(1, MainPageRequest("Popular", "popular", false))
                    val first = home.items.firstOrNull()?.list?.firstOrNull() ?: error("empty home page")
                    println("[$name] home: ${home.items.sumOf { it.list.size }} items, first='${first.name?.take(60)}'")
                    val search = provider.search(first.name?.split(' ')?.firstOrNull() ?: "a", 1)
                    println("[$name] search: ${search.items.size} items")
                    first.url
                }

                println("[$name] url: $target")
                val loaded = provider.load(target) ?: error("load returned null")
                val episodes = (loaded as? TvSeriesLoadResponse)?.episodes.orEmpty()
                val episodeData = when (loaded) {
                    is TvSeriesLoadResponse -> episodes.getOrNull(episodeIndex)?.data
                    is MovieLoadResponse -> loaded.dataUrl
                    else -> null
                } ?: error("load produced no data source: ${loaded::class.simpleName}")
                println("[$name] load: ${loaded::class.simpleName} '${loaded.name?.take(60)}' episodes=${episodes.size}")

                val links = mutableListOf<String>()
                val handled = provider.loadLinks(episodeData, false, {}, { link ->
                    links.add("${link.name ?: "?"} q=${link.quality} ${link.type} ${link.url.take(70)}")
                })
                println("[$name] links: handled=$handled count=${links.size}")
                links.take(6).forEach { println("[$name]   $it") }
                check(links.isNotEmpty()) { "no links emitted" }
            }
        }
        if (result.isSuccess) ok++ else {
            failed++
            println("[$name] FAIL ${result.exceptionOrNull()?.message?.replace('\n', ' ')?.take(160)}")
        }
    }

    println()
    println("provider summary: ok=$ok failed=$failed total=${sites.size}")
    if (failed > 0) kotlin.system.exitProcess(1)
}
