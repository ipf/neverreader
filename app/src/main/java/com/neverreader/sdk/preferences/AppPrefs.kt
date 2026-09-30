package com.neverreader.sdk.preferences

import com.neverreader.util.prefs.BooleanPreference
import com.neverreader.util.prefs.Prefs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Preferences that belong to the app rather than to one component, so that the
 * places that need them do not each have to be handed a [Preferences] and
 * re-declare the same key.
 *
 * This is the third and probably last one. When adding a new preference, prefer
 * holding it in the component that uses it, and add it here only if more than
 * one component needs the same key.
 */
@Singleton
class AppPrefs @Inject constructor(prefs: Prefs) {

    /**
     * Whether to load article thumbnails that are served by a third party.
     *
     * Off by default. Readeck proxies thumbnails through your own server, so
     * those always load. Wallabag hands back the article's raw OpenGraph image
     * URL, so loading it would tell every article's image host your IP address
     * just for scrolling past it.
     *
     * Note this covers the list thumbnails only. The reader renders the
     * backend's HTML in a WebView, where images are subresources of the article
     * itself and are not host-filtered.
     */
    val LOAD_THIRD_PARTY_IMAGES: BooleanPreference =
        prefs.forUser("loadThirdPartyImages", false)
}
