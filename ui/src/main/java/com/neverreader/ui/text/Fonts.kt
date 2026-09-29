package com.neverreader.ui.text

import android.content.Context
import android.graphics.Typeface

/**
 * Access to typefaces used by the ui components.
 *
 *
 * At runtime, you can get these fonts with [.get].
 *
 *
 *
 *
 * and then fonts can be referenced in css styles by the font family names declared in that file.
 *
 *
 * Once loaded, typefaces are cached in memory. If we ever find a use case for clearing the cache, we could
 * provide a method to.
 *
 *
 * This class is intended for use only on the UI-Thread. (If we find a reason to enforce this, we can add later)
 *
 *
 * Dev Note: Why not use the font tools in the support library? Our main NeverReader app needs the fonts available for use in css and html
 * as well. While there is file:///android_res/ available, in practice it appeared pretty unreliable and could break
 * based on changing package names, using modules, build configs and could be affected by different WebView implementations
 * and versions. For example: https://bugs.chromium.org/p/chromium/issues/detail?id=599869 So having them in assets allows
 * any apps that use this module to access these fonts in code, XML and in html/css without having to duplicate large font files.
 * We also experienced bugs with ResourcesCompat.getFont on some devices.
 */
object Fonts {
    /** Typefaces are expensive to load, so they are cached for the process lifetime. */
    private val cache: MutableMap<Font, Typeface?> = HashMap()

    /**
     * Get a typeface by attribute.
     * @param attrValue The "value" of a [com.neverreader.ui.R.attr.typeface] to load
     * @return The typeface or null if none matched that value
     */
    fun get(context: Context, attrValue: Int): Typeface? {
        for (font in Font.entries) {
            if (font.attrValue == attrValue) {
                return get(context, font)
            }
        }
        return null
    }

    /**
     * Get a typeface by enum.
     */
    fun get(context: Context, font: Font): Typeface? {
        var typeFace = cache.get(font)
        if (typeFace == null) {
            try {
                typeFace = Typeface.createFromAsset(context.getAssets(), font.filename)
            } catch (e: RuntimeException) {
                // The font should be in assets; fall back to the system default
                // rather than crashing if a build is missing one.
                typeFace = Typeface.create(
                    Typeface.SANS_SERIF,
                    if (font.bold()) Typeface.BOLD else Typeface.NORMAL
                )
            }
            cache.put(font, typeFace)
        }
        return typeFace
    }

    /**
     * The faces the legacy XML views can reach, for `app:typeface`.
     *
     * Compose uses [com.neverreader.ui.theme.AppFontFamily] instead, which loads
     * the same files and is the preferred path.
     */
    enum class Font(val attrValue: Int, val filename: String) {
        INTER_MEDIUM(1, "fonts/Inter-Medium.ttf"),
        INTER_MEDIUM_ITALIC(2, "fonts/Inter-MediumItalic.ttf"),
        INTER_REGULAR(3, "fonts/Inter-Regular.ttf"),
        INTER_REGULAR_ITALIC(4, "fonts/Inter-Italic.ttf"),
        SOURCE_SERIF_4_MEDIUM(9, "fonts/SourceSerif4Display-Semibold.otf"),
        ;

        fun bold(): Boolean = this == INTER_MEDIUM || this == SOURCE_SERIF_4_MEDIUM
    }
}
