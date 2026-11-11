package com.aiohentai.cs.compat

/**
 * Application context holder.
 *
 * A few sources are fronted by Cloudflare. After the user solves the challenge in
 * CloudStream's built-in browser, the provider can read the WebView cookie jar and
 * user agent and replay them on its own HTTP requests.
 */
object AppContext {
    @Volatile
    var value: android.content.Context? = null
}
