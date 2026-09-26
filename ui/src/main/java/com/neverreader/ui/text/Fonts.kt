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
 * In xml, use a [com.neverreader.ui.view.themed.ThemedTextView] or [com.neverreader.ui.view.themed.ThemedEditText] and the app:typeface attribute.
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
 * Dev Note: Why not use the font tools in support library? Our main NeverReader app needs the fonts available for use in css and html
 * as well. While there is file:///android_res/ available, in practice it appeared pretty unreliable and could break
 * based on changing package names, using modules, build configs and could be affected by different WebView implementations
 * and versions. For example: https://bugs.chromium.org/p/chromium/issues/detail?id=599869 So having them in assets allows
 * any apps that use this module to access these fonts in code, xml and in html/css without having to duplicate large font files.
 * We also experienced bugs with ResourcesCompat.getFont on some devices.
 */
object Fonts {
    /** Filename of the graphik-lcg font family css file within the assets directory.  */
    private val cache: MutableMap<Font?, Typeface?> = HashMap<Font?, Typeface?>()

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
    @JvmStatic
    fun get(context: Context, font: Font): Typeface? {
        var typeFace = cache.get(font)
        if (typeFace == null) {
            try {
                typeFace = Typeface.createFromAsset(context.getAssets(), font.filename)
            } catch (e: RuntimeException) {
                // Custom fonts are licensed and may not be present; fall back to the system default.
                typeFace = Typeface.create(
                    Typeface.SANS_SERIF,
                    if (font.bold()) Typeface.BOLD else Typeface.NORMAL
                )
            }
            cache.put(font, typeFace)
        }
        return typeFace
    }

    enum class Font(val attrValue: Int, val filename: String) {
        GRAPHIK_LCG_BOLD(10, "graphik_lcg_bold_no_leading.otf"),
        GRAPHIK_LCG_MEDIUM(1, "graphik_lcg_medium_no_leading.otf"),
        GRAPHIK_LCG_MEDIUM_ITALIC(2, "graphik_lcg_medium_italic_no_leading.otf"),
        GRAPHIK_LCG_REGULAR(3, "graphik_lcg_regular_no_leading.otf"),
        GRAPHIK_LCG_REGULAR_ITALIC(4, "graphik_lcg_regular_italic_no_leading.otf"),

        BLANCO_REGULAR(5, "blanco_osf_regular.otf"),
        BLANCO_BOLD(6, "blanco_osf_bold.otf"),
        BLANCO_ITALIC(7, "blanco_osf_italic.otf"),
        BLANCO_BOLD_ITALIC(8, "blanco_osf_bold_italic.otf"),

        DOYLE_MEDIUM(9, "doyle_medium.otf"),

        /**
         * NeverReader icons as a typeface.  This allows certain images to be easily added to text Strings while still
         * properly scaling with the user's text size settings.
         */
        ;

        fun bold(): Boolean {
            return this == Font.GRAPHIK_LCG_BOLD || this == Font.BLANCO_BOLD || this == Font.BLANCO_BOLD_ITALIC || this == Font.DOYLE_MEDIUM
        }
    }
}
