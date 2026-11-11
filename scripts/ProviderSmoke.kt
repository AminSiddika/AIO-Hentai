package com.aiohentai.cs.smoke

import com.aiohentai.cs.compat.AniyomiProvider
import com.aiohentai.cs.compat.AnimeHttpSource
import com.lagradost.cloudstream3.MainPageRequest
import kotlinx.coroutines.runBlocking

/**
 * Provider-level smoke test: runs the exact CloudStream entry points (getMainPage, search,
 * load, loadLinks) through the AniyomiProvider adapter, so a mapping bug shows up here
 * instead of on a phone.
 *
 * Usage: ProviderSmokeKt [sourceName ...] [--jobs N]
 */
fun main(args: Array<String>) {
    val filters = args.filterNot { it.startsWith("--") }.toSet()
    val sites = smokeSites().filter { filters.isEmpty() || it.first in filters }
    var ok = 0
    var failed = 0

    for ((name, factory) in sites) {
        val src: AnimeHttpSource = factory()
        val provider = AniyomiProvider(src)
        val result = runBlocking {
            runCatching {
                val home = provider.getMainPage(1, MainPageRequest("Popular", "popular", false))
                val first = home.items.firstOrNull()?.list?.firstOrNull()
                    ?: error("empty home page")
                println("[$name] home: ${home.items.sumOf { it.list.size }} items, first='${first.name?.take(60)}'")

                val search = provider.search(first.name?.split(' ')?.firstOrNull() ?: "a", 1)
                println("[$name] search: ${search.items.size} items")

                val loaded = provider.load(first.url)
                    ?: error("load returned null")
                val episodeData = when (loaded) {
                    is com.lagradost.cloudstream3.TvSeriesLoadResponse ->
                        loaded.episodes.firstOrNull()?.data
                    is com.lagradost.cloudstream3.MovieLoadResponse -> loaded.dataUrl
                    else -> null
                } ?: error("load produced no data source: ${loaded::class.simpleName}")
                println("[$name] load: ${loaded::class.simpleName} '${loaded.name?.take(60)}'")

                val links = mutableListOf<String>()
                val handled = provider.loadLinks(episodeData, false, {}, { link ->
                    links.add("${link.name ?: "?"} ${link.quality} ${link.url.take(60)}")
                })
                println("[$name] links: handled=$handled count=${links.size} ${links.take(3)}")
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
