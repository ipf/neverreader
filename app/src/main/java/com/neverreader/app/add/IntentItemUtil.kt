package com.neverreader.app.add

import android.content.Intent
import com.neverreader.util.java.Logs.printStackTrace
import com.neverreader.util.java.UrlFinder.getUrlsFromText

class IntentItemUtil private constructor() {
    init {
        throw AssertionError("No instances.")
    }

    companion object {
        /**
         * Parses an Intent for [AddActivity] to extract an item to save and various types of meta data.
         */
        fun from(intent: Intent): IntentItem {
            // WARNING: This is used in an exported activity. Extras could come from outside apps and may not be trust worthy.
            // Get a list of urls to choose from from the Intent
            val urls: ArrayList<String?> = findUrlsFromIntent(intent)
            // Determine which url to save
            var url: String? = null
            if (!urls.isEmpty()) {
                url = urls.get(0)
            }

            val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

            return IntentItem(url, title)
        }

        /**
         * Finds the url(s) that are to be saved.
         */
        private fun findUrlsFromIntent(intent: Intent): ArrayList<String?> {
            if (Intent.ACTION_VIEW == intent.action) {
                val urls = ArrayList<String?>(1)
                if (intent.getData() != null) {
                    val saveUrl: String?
                    try {
                        saveUrl = intent.getData()!!.getQueryParameter("url")
                        urls.add(saveUrl)
                    } catch (t: Throwable) {
                        // Not matching the format we are expecting
                        printStackTrace(t)
                    }
                }
                return urls
            } else {
                // SEND Action or other, search the extras
                val urls =
                    getUrlsFromText(intent.getStringExtra(Intent.EXTRA_TEXT))
                return if (urls != null) urls else ArrayList<String?>()
            }
        }
    }
}
