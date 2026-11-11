package com.animetail.cs.compat

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

const val BROWSER_UA =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

private object MemoryCookieJar : CookieJar {
    private val store = ConcurrentHashMap<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val list = store.getOrPut(url.host) { mutableListOf() }
        synchronized(list) {
            for (cookie in cookies) {
                list.removeAll { it.name == cookie.name && it.path == cookie.path }
                list.add(cookie)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val list = store[url.host] ?: return emptyList()
        val now = System.currentTimeMillis()
        synchronized(list) {
            list.removeAll { it.expiresAt < now }
            return list.filter { it.matches(url) }
        }
    }
}

val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .cookieJar(MemoryCookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
}

val defaultHeaders: Headers by lazy {
    Headers.Builder()
        .add("User-Agent", BROWSER_UA)
        .add(
            "Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9," +
                "image/avif,image/webp,*/*;q=0.8",
        )
        .add("Accept-Language", "en-US,en;q=0.9")
        .build()
}

fun GET(url: String, headers: Headers? = null): Request {
    val builder = Request.Builder().url(url)
    if (headers != null) builder.headers(headers)
    return builder.build()
}

fun Response.asJsoup(): Document {
    val body = this.body
    val url = this.request.url.toString()
    if (body == null) return Jsoup.parse("", url)

    val bytes = body.bytes()
    val charset = body.contentType()?.charset(Charsets.UTF_8) ?: Charsets.UTF_8
    var html = bytes.toString(charset)
    if (html.startsWith("\uFEFF")) html = html.removePrefix("\uFEFF")
    return Jsoup.parse(html, url)
}

/** One fetched page, buffered so several parse functions can read it independently. */
class FetchedPage internal constructor(
    val url: String,
    private val bytes: ByteArray,
    private val mediaType: MediaType?,
    private val template: Response,
) {
    fun response(): Response = template.newBuilder()
        .body(bytes.toResponseBody(mediaType))
        .build()

    fun doc(): Document = response().asJsoup()
}
