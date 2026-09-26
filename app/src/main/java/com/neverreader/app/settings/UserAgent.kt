package com.neverreader.app.settings

import android.content.Context
import android.webkit.WebView
import com.neverreader.sdk.preferences.AppPrefs
import com.neverreader.util.java.Logs
import com.neverreader.util.prefs.BooleanPreference
import com.neverreader.util.prefs.Preferences
import com.neverreader.util.prefs.StringPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages the user agent for downloading and the reader. It allows faking a desktop agent when required or requested by user.
 */
@Singleton
class UserAgent @Inject constructor(@ApplicationContext context: Context, prefs: Preferences) {

    private val useMobile: BooleanPreference = prefs.forUser("userAgentMobile", false)
    private var mobile: StringPreference? = prefs.forApp("uamobile", null as String?)
    private var desktop: StringPreference? = prefs.forApp("uadesktop", null as String?)

    init {
        if (mobile!!.get() == null || desktop!!.get() == null) {
            // Only reload if we don't already have something cached.
            // TODO add something that will refresh this every once in a while
            // We don't need to refresh it every app load, as it has some start up cost, but maybe each app update, or OS update?
            // Unfortunately can't make it an async task because it needs the ui thread for a WebView. Maybe a scheduled repeating task?
            reload(context)
        }
    }

    /** @return The default User Agent for this device.
     */
    fun mobile(): String? {
        return mobile!!.get()
    }

    /** @return A fake desktop User Agent.
     */
    fun desktop(): String? {
        return desktop!!.get()
    }

    /** @return One of the two agents, based on the user's [useMobile] preference.
     */
    fun preferred(): String? {
        return if (useMobile.get()) {
            mobile()
        } else {
            desktop()
        }
    }

    private fun saveMobileAgent(agent: String?) {
        mobile!!.set(agent)
    }

    private fun saveDesktopAgent(agent: String?) {
        desktop!!.set(agent)
    }

    /**
     * Attempts to load the current user agent from a WebView
     * and updates the known user agents.
     * If you need to have the absolute latest, invoke this first.
     * On a Nexus 5x this takes around 40/50ms
     */
    fun reload(context: Context): UserAgent {
        // First attempt to get the real device user agent.
        try {
            val webView = WebView(context)
            var m = webView.getSettings().getUserAgentString()
            m = StringUtils.trimToNull(m)
            if (m != null) {
                saveMobileAgent(m)

                // Success
            } else {
                saveMobileAgent(FAIL_SAFE_MOBILE)
                saveDesktopAgent(FAIL_SAFE_DESKTOP)
                return this
            }
        } catch (t: Throwable) {
            // Not really expected, but let's not blow up the app over this.
            Logs.printStackTrace(t)
            saveMobileAgent(FAIL_SAFE_MOBILE)
            saveDesktopAgent(FAIL_SAFE_DESKTOP)
            return this
        }


        // If successfully obtained the mobile agent...
        // Second, tweak the user agent to fake a desktop agent, but using the correct version numbers.
        try {
            /*
			 * A default WebView agent looks something like this 			// Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/47.0.2526.100 Mobile Safari/537.36
			 * Chrome's "Request Desktop Agent" looks something like this:	// Mozilla/5.0 (X11; Linux x86_64) 								AppleWebKit/537.36 (KHTML, like Gecko) 			   Chrome/48.0.2564.95 	Safari/537.36
			 */

            // Remove "Mobile" from Safari

            var d = StringUtils.replace(mobile!!.get(), "Mobile Safari", "Safari")


            // Replace OS info
            val open = d!!.indexOf("(")
            val android = StringUtils.indexOfIgnoreCase(d, "Android")
            val close = d.indexOf(")")
            if (open in 1..<close && android > open && android < close) {
                d = StringUtils.substring(d, 0, open + 1) + FAKE_DESKTOP_OS + StringUtils.substring(
                    d,
                    close
                )
            }
            d = StringUtils.trimToNull(d)

            saveDesktopAgent(d ?: FAIL_SAFE_DESKTOP)
        } catch (t: Throwable) {
            // Not really expected, but let's not blow up the app over this.
            Logs.printStackTrace(t)
            saveDesktopAgent(FAIL_SAFE_DESKTOP)
        }
        return this
    }

    companion object {
        /**
         * REVIEW USER AGENT This should be updated every now and then or we should find a permanent and automatic way. Can we just omit it?
         */
        private const val FAKE_DESKTOP_OS = "X11; Linux x86_64"
        private const val FAIL_SAFE_MOBILE =
            "Mozilla/5.0 (Linux; Android 6.0.1; Nexus 5 Build/MMB29K; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/47.0.2526.100 Mobile Safari/537.36"
        private const val FAIL_SAFE_DESKTOP =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.95 Safari/537.36"
    }
}
