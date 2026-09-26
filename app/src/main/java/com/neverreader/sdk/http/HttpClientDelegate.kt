package com.neverreader.sdk.http

import com.neverreader.app.AppLifecycle
import com.neverreader.app.AppLifecycle.LogoutPolicy
import com.neverreader.app.AppLifecycleEventDispatcher
import com.neverreader.app.AppMode
import com.neverreader.sdk.network.eclectic.EclecticHttp
import com.neverreader.sdk.network.toEclecticOkHttpClient
import com.neverreader.util.prefs.Preferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides a singleton http client for network access.
 * On logout, all pending requests are cancelled.
 */
@Singleton
class HttpClientDelegate
@Inject constructor(
    private val mode: AppMode,
    private val status: NetworkStatus,
    prefs: Preferences,
    dispatcher: AppLifecycleEventDispatcher,
) : AppLifecycle {
    private val loggingLevel = prefs.forApp(
        "dcfig_lg_http",
        if (mode.isDevBuild) EclecticHttp.Logging.API.name else EclecticHttp.Logging.NONE.name
    )
    private var client: EclecticHttp? = null

    init {
        dispatcher.registerAppLifecycleObserver(this)
    }

    fun getClient(): EclecticHttp? {
        if (client == null) {
            client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .addLogging()
                .build()
                .toEclecticOkHttpClient()
        }
        return client
    }

    private fun OkHttpClient.Builder.addLogging() = apply {
        if (mode.isForInternalCompanyOnly) {
            addInterceptor(LoggingInterceptor())
        }
    }

    fun getLoggingLevel(): EclecticHttp.Logging {
        return if (mode.isForInternalCompanyOnly) {
            runCatching { EclecticHttp.Logging.valueOf(loggingLevel.get()!!) }.getOrDefault(EclecticHttp.Logging.NONE)
        } else {
            EclecticHttp.Logging.NONE
        }
    }

    fun setLoggingLevel(value: EclecticHttp.Logging) {
        if (!mode.isForInternalCompanyOnly) return
        loggingLevel.set(value.name)
    }

    fun status(): NetworkStatus {
        return status
    }

    override fun onLogoutStarted(): LogoutPolicy {
        return object : LogoutPolicy {
            override fun stopModifyingUserData() {
                try {
                    client?.release()
                } catch (ignore: Throwable) {
                }
            }

            override fun deleteUserData() {}

            override fun restart() {
                client = null
            }

            override fun onLoggedOut() {}
        }
    }

    private inner class LoggingInterceptor : Interceptor {
        private val wrapped = HttpLoggingInterceptor()

        override fun intercept(chain: Interceptor.Chain): Response {
            when (getLoggingLevel()) {
                EclecticHttp.Logging.NONE -> wrapped.setLevel(HttpLoggingInterceptor.Level.NONE)
                EclecticHttp.Logging.API -> wrapped.setLevel(
                    if (true) {
                        HttpLoggingInterceptor.Level.BODY
                    } else {
                        HttpLoggingInterceptor.Level.NONE
                    }
                )
                EclecticHttp.Logging.EVERYTHING -> wrapped.setLevel(HttpLoggingInterceptor.Level.BODY)
            }
            return wrapped.intercept(chain)
        }
    }
}
