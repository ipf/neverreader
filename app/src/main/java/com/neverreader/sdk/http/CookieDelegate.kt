package com.neverreader.sdk.http

import android.webkit.CookieManager
import com.neverreader.app.AppLifecycle
import com.neverreader.app.AppLifecycle.LogoutPolicy
import com.neverreader.app.AppLifecycleEventDispatcher
import com.neverreader.app.AppThreads
import com.neverreader.sdk.network.eclectic.EclecticHttp
import com.neverreader.sdk.network.eclectic.EclecticHttpRequest
import com.neverreader.sdk.network.eclectic.EclecticHttpUtil
import com.neverreader.util.java.DomainUtils
import java.net.HttpCookie
import java.net.URI
import java.net.URISyntaxException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Cookies for the app, mostly for the site logins feature. See [com.neverreader.sdk.api.generated.thing.Loginlist] and related classes.
 */
@Singleton
class CookieDelegate @Inject constructor(
    http: HttpClientDelegate,
    threads: AppThreads,
    dispatcher: AppLifecycleEventDispatcher
) : AppLifecycle {
    private val http: HttpClientDelegate
    private val threads: AppThreads
    private var cookieSyncManager: CookieSyncManagerCompat? = null

    init {
        dispatcher.registerAppLifecycleObserver<CookieDelegate>(this)
        this.http = http
        this.threads = threads
    }

    private fun init() {
        if (cookieSyncManager == null) {
            cookieSyncManager = CookieSyncManagerCompat()
        }
    }

    fun getCookiesString(url: String?): String? {
        init()
        val cookieManager = CookieManager.getInstance()
        return cookieManager.getCookie(url)
    }

    fun extendCookies(url: String?) {
        init()
        // Hit the url so we get the latest cookies in the store.
        val client = http.getClient()!!
        EclecticHttpUtil.getString(
            http.getClient()!!.buildRequest(url)!!.setHeader("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36"),
            client
        )

        extendCookies(
            url,
            java.net.CookieManager().getCookieStore().getCookies()
        ) // REVIEW these get all cookies... don't we only want the ones for the url? but looking at FileDownloader's getCookiesFromStore method, this seems to be what it did returned.
    }

    fun extendCookies(url: String?, cookies: MutableList<HttpCookie>) {
        init()
        val cookieManager = CookieManager.getInstance()
        val builder = StringBuilder()
        var value: String?
        var domain: String?
        var path: String?
        var isSecure: Boolean
        for (cookie in cookies) {
            builder.setLength(0)

            value = cookie.getValue()
            domain = cookie.getDomain()
            path = cookie.getPath()
            isSecure = cookie.getSecure()

            builder
                .append(cookie.getName())
                .append("=")
                .append(if (value != null) value else "")
                .append(";")
                .append("expires=Fri, 01 Jan 2049 01:01:01 GMT;")

            if (domain != null && domain.length > 0) {
                builder
                    .append("Domain=")
                    .append(domain)
                    .append(";")
            }

            if (path != null && path.length > 0) {
                builder
                    .append("Path=")
                    .append(path)
                    .append(";")
            }

            if (isSecure) {
                builder
                    .append("Secure")
                    .append(";")
            }

            cookieManager.setCookie(url, builder.toString())
        }
    }

    /**
     * Performs a [CookieManager.flush] asynchronously.
     * We used to use [CookieSyncManager.sync], which before Lollipop flushed asynchronously,
     * but since Lollipop, it is a blocking call. To keep it asynchronous, we handle wrapping it
     * in an async call. If a blocking call is needed at some point another method could be created.
     */
    fun sync() {
        threads.async(Runnable {
            init()
            cookieSyncManager!!.sync()
        })
    }

    override fun onLogoutStarted(): LogoutPolicy? {
        return object : LogoutPolicy {
            override fun stopModifyingUserData() {}

            override fun deleteUserData() {
                if (cookieSyncManager != null) {
                    cookieSyncManager!!.removeAllCookies()
                }
            }

            override fun restart() {}

            override fun onLoggedOut() {}
        }
    }

    private class CookieSyncManagerCompat {
        fun sync() {
            CookieManager.getInstance().flush()
        }

        fun removeAllCookies() {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        }
    }

    /**
     * Add cookies to the headers
     */
    fun addCookiesToRequest(request: EclecticHttpRequest, client: EclecticHttp) {
        val url = request.url
        val cookieString = getCookiesString(url)
        if (cookieString != null) {
            val store = java.net.CookieManager().getCookieStore()
            var cookie: HttpCookie?
            var cookieParts: Array<String?>?

            val cookies: Array<String?> =
                cookieString.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val length = cookies.size
            for (i in 0..<length) {
                cookieParts =
                    cookies[i]!!.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                cookie =
                    HttpCookie(cookieParts[0], if (cookieParts.size > 1) cookieParts[1] else null)
                cookie.setDomain(DomainUtils.getBaseDomain(url))
                try {
                    store.add(URI(request.url), cookie)
                } catch (ignore: URISyntaxException) {
                }
            }
        }
    }
}
