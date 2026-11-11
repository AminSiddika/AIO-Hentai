package com.aiohentai.cs.compat

import android.util.Base64
import okhttp3.Headers

/**
 * Single playback wrapper, ported from the Aniyomi extensions.
 *
 * Every video is given a browser User-Agent plus the Referer/Origin of the source site,
 * which is what hotlink-protected media hosts check. [referer] is set per provider right
 * before parsing videos (the original per-APK global would leak between providers when
 * they all live in one plugin).
 */
object MediaVideo {
    private const val WORKER = "https://animextension.mobiusdie.workers.dev"
    private const val TOKEN = "animetail-2026"
    private const val UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0 Safari/537.36"

    @Volatile
    var referer: String = ""

    @Volatile
    var useProxy: Boolean = false

    fun create(
        url: String,
        quality: String,
        videoUrl: String? = url,
        headers: Headers? = null,
    ): Video {
        val target = videoUrl ?: url
        val ref = headers?.get("Referer")
            ?.takeIf { it.isNotBlank() }
            ?: referer.takeIf { it.isNotBlank() }
            ?: ""

        val builder = headers?.newBuilder() ?: Headers.Builder()
        builder.set("User-Agent", UA)
        if (ref.isNotBlank()) {
            builder.set("Referer", ref)
            originOf(ref)?.let { builder.set("Origin", it) }
        }

        if (!useProxy || target.contains(".mpd", ignoreCase = true)) {
            return Video(url, quality, target, builder.build())
        }

        val proxyReferer = ref.ifBlank { originOf(target).orEmpty() }
        val proxied = "$WORKER/api/proxy?token=$TOKEN&u=${b64(target)}&r=${b64(proxyReferer)}"
        return Video(proxied, quality, proxied, builder.build())
    }

    private fun b64(value: String): String = Base64.encodeToString(
        value.toByteArray(Charsets.UTF_8),
        Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
    )

    private fun originOf(url: String): String? {
        val scheme = url.substringBefore("://", "")
        if (scheme != "http" && scheme != "https") return null
        val host = url.substringAfter("://").substringBefore("/")
        if (host.isBlank()) return null
        return "$scheme://$host"
    }
}
