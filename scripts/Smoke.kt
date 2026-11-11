package com.animetail.cs.smoke

import com.animetail.cs.compat.AnimeFilterList
import com.animetail.cs.compat.AnimeHttpSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/**
 * Source-level smoke test: exercises the ported scrapers exactly as the provider adapter
 * does (home page -> details -> episodes -> videos) without touching CloudStream classes,
 * so it can run on the JVM.
 *
 * Usage: SmokeKt [sourceName ...] [--links] [--jobs N]
 */
private const val TIMEOUT_MS = 60_000L
private val ok = AtomicInteger()
private val failed = AtomicInteger()

private fun log(site: String, step: String, message: String) {
    println("[$site] $step: $message".replace('\n', ' ').take(300))
}

private sealed class Outcome {
    data object Pass : Outcome()
    data class Fail(val reason: String) : Outcome()
}

private suspend fun testSource(src: AnimeHttpSource, name: String, wantLinks: Boolean): Outcome =
    withTimeoutOrNull(TIMEOUT_MS) {
        runCatching {
            val latest = src.latestUpdates(1)
            log(name, "latest", "${latest.animes.size} items, next=${latest.hasNextPage}")
            check(latest.animes.isNotEmpty()) { "empty home page" }
            val first = latest.animes.first()
            log(name, "item", "${first.title.take(60)} -> ${first.url.take(80)}")

            val query = first.title.split(' ').firstOrNull { it.length > 3 } ?: first.title
            runCatching { src.searchAnime(1, query, AnimeFilterList()) }
                .onSuccess { log(name, "search", "${it.animes.size} for '$query'") }
                .onFailure { log(name, "search", "FAIL ${it.message}") }

            val page = src.fetch(src.baseUrl.trimEnd('/') + first.url.takeIf { it.startsWith("/") }.orEmpty())
            val details = src.details(page)
            log(name, "details", "${details.title.take(60)} | poster=${details.thumbnail_url != null}")
            val episodes = src.episodes(page)
            log(name, "episodes", "${episodes.size}: " + episodes.take(3).joinToString { it.name.take(24) })
            check(episodes.isNotEmpty()) { "no episodes" }

            if (wantLinks) {
                val videoPage = src.fetch(src.baseUrl.trimEnd('/') + episodes.first().url)
                val videos = src.videos(videoPage)
                log(name, "videos", "${videos.size}: " + videos.take(4).joinToString { "${it.quality}=${it.videoUrl.take(40)}" })
                check(videos.isNotEmpty()) { "no videos" }
            }
        }
    }.let { result ->
        when {
            result == null -> Outcome.Fail("timeout ${TIMEOUT_MS}ms")
            result.isSuccess -> Outcome.Pass
            else -> Outcome.Fail(result.exceptionOrNull()?.message ?: "unknown")
        }
    }

fun main(args: Array<String>) {
    val wantLinks = args.contains("--links")
    val jobs = args.indexOf("--jobs").let { i ->
        if (i >= 0) args.getOrNull(i + 1)?.toIntOrNull() ?: 8 else 8
    }
    val filters = args.toSet() - setOf("--links", "--jobs") - (0..99).map { it.toString() }.toSet()
    val sites = smokeSites().filter { filters.isEmpty() || it.first in filters }

    val semaphore = Semaphore(jobs)
    runBlocking(Dispatchers.IO) {
        coroutineScope {
            sites.map { (name, factory) ->
                async {
                    semaphore.withPermit {
                        val src = try {
                            factory()
                        } catch (t: Throwable) {
                            log(name, "init", "FAIL ${t.message}")
                            failed.incrementAndGet()
                            return@withPermit
                        }
                        when (val outcome = testSource(src, name, wantLinks)) {
                            is Outcome.Pass -> ok.incrementAndGet()
                            is Outcome.Fail -> {
                                log(name, "error", "FAIL ${outcome.reason}")
                                failed.incrementAndGet()
                            }
                        }
                    }
                }
            }.awaitAll()
        }
    }

    println()
    println("summary: ok=${ok.get()} failed=${failed.get()} total=${sites.size}")
    if (failed.get() > 0) kotlin.system.exitProcess(1)
}
