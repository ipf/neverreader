package com.neverreader.util.android

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassifier
import com.neverreader.app.AppMode
import com.neverreader.util.java.UrlFinder
import com.neverreader.util.prefs.IntPreference
import com.neverreader.util.prefs.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads urls off the clipboard so they can be offered for saving.
 *
 * Write-only: the app has no Copy Link action, so there is nothing to suppress
 * the offer for, and [lastUrlHash] is only a "do not offer the same url twice"
 * guard.
 */
@Singleton
class Clipboard @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mode: AppMode,
    prefs: Preferences
) {

    private val lastUrlHash: IntPreference = prefs.forUser("lastClipUrlHash", 0)
    private val manager: ClipboardManager =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    /**
     * Records the current clipboard url as already seen.
     *
     * Called once the account exists, so a url the user copied in order to sign
     * in is not offered back to them as something to save. This used to be an
     * `AppLifecycle.onLoggedIn` hook, which never ran: the dispatcher that
     * delivered it was never itself called from anywhere.
     */
    fun markCurrentUrlAsSeen() {
        getUrl()
    }

    /**
     * Get URL from clipboard or null if there is none or this URL was returned previously already.
     */
    fun getUrl(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.primaryClipDescription?.let { clipDescription ->
                if (clipDescription.classificationStatus == ClipDescription.CLASSIFICATION_COMPLETE &&
                    clipDescription.getConfidenceScore(TextClassifier.TYPE_URL) < 0.5) {
                    return null
                }
            }
        }
        return UrlFinder.getFirstUrlOrNull(getText())?.let { url ->
            // if the url is the same as the last one, return null
            if (url.hashCode() == lastUrlHash.get()) {
                null
            } else {
                lastUrlHash.set(url.hashCode())
                url
            }
        }
    }

    fun getText(): String? {
        var clipData: ClipData? = null
        try {
            clipData = manager.primaryClip
        } catch (t: Throwable) {
            // Looks like just checking clipboard contents can crash the app on some devices.
            if (mode.isForInternalCompanyOnly) {
                throw t
            }
        }

        if (clipData?.description == null
            || clipData.description?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) == false
            || clipData.itemCount == 0
            || clipData.getItemAt(0) == null
        ) {
            return null
        }

        val item = clipData.getItemAt(0)
        return item.text?.toString() ?: item.uri?.toString()
    }

}