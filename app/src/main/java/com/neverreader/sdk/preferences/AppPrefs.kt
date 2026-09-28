package com.neverreader.sdk.preferences

import android.content.Context
import com.neverreader.util.android.FormFactor.isTablet
import com.neverreader.util.prefs.BooleanPreference
import com.neverreader.util.prefs.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's [com.neverreader.util.prefs.Preference]s.
 *
 *
 * Historically these were all in one large static class with all the preferences as global static final fields.
 * That was refactored to have these fields be instance fields to help aid in making the app more testable in unit tests.
 *
 *
 * Ideally instead of adding new preferences here, we start passing a [Preferences] instance to components
 * and components create and hold the preferences they need and if another part of the app needs that preference,
 * they get it through that component.  However, it is possible there will be some preferences that are "global"
 * to the app and may still want to live here. We'll have to see how this all plays out.
 *
 */
@Singleton
class AppPrefs @Inject constructor(
    val prefs: Preferences,
    @ApplicationContext context: Context
) {

    /**
     * Whether to load article thumbnails that are served by a third party.
     *
     * Off by default. Readeck proxies thumbnails through your own server, so
     * those always load. Wallabag hands back the article's raw OpenGraph image
     * URL, so loading it would tell every article's image host your IP address
     * just for scrolling past it.
     */
    val LOAD_THIRD_PARTY_IMAGES: BooleanPreference =
        prefs.forUser("loadThirdPartyImages", false)

    fun changes(): Flow<String?>? {
        return prefs.changes()
    }
}
