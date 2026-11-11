package com.aiohentai.cs.compat

import com.lagradost.cloudstream3.network.CloudflareKiller
import okhttp3.OkHttpClient

private val cloudflareClient: OkHttpClient by lazy {
    httpClient.newBuilder().addInterceptor(CloudflareKiller()).build()
}

/**
 * In-app client that can solve a Cloudflare managed challenge with CloudStream's WebView.
 * On a plain JVM (AppContext is null) the normal client is used.
 */
fun appHttpClient(): OkHttpClient =
    if (AppContext.value != null) cloudflareClient else httpClient
