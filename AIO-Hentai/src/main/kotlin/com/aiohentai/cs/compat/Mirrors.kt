package com.aiohentai.cs.compat

import okhttp3.Headers
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Picks the first reachable mirror for tube sites that run several domains
 * (xvideos.com / xvideos2.com / xvideos3.com, xnxx.com / xnxx2.com / ..., ...).
 *
 * The probe runs once per process, on the first browse request, and the chosen domain is
 * then used for every generated URL and for the playback Referer.
 */
class MirrorPool(
    private val primary: String,
    private val alternates: List<String>,
    private val marker: String,
) {

    @Volatile
    var active: String = primary
        private set

    @Volatile
    private var probed: Boolean = false

    @Synchronized
    fun base(client: OkHttpClient, headers: Headers): String {
        if (probed) return active
        probed = true

        val probeClient = client.newBuilder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .callTimeout(12, TimeUnit.SECONDS)
            .build()

        for (candidate in listOf(primary) + alternates) {
            val reachable = runCatching {
                probeClient.newCall(GET("$candidate/", headers)).execute().use { response ->
                    response.isSuccessful &&
                        response.body?.string()?.contains(marker, ignoreCase = true) == true
                }
            }.getOrDefault(false)
            if (reachable) {
                active = candidate
                break
            }
        }
        return active
    }
}
