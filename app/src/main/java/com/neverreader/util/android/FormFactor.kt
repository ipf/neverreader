package com.neverreader.util.android

import android.content.Context
import android.content.res.Configuration
import android.util.DisplayMetrics
import android.util.TypedValue
import com.neverreader.app.App
import com.neverreader.app.App.Companion.getContext as appContextFn

/**
 * Device form factor, measured once from the application context.
 *
 * Only what the app still uses is here: a cached classification and dp
 * conversion. The window-width helpers, the Kindle detection, the CSS class
 * names and the human-readable label had no callers, and get(Context) measured
 * the window rather than the device, which is the mistake [get] exists to
 * avoid.
 */
object FormFactor {
    private const val SCREENLAYOUT_SIZE_XLARGE: Int = 0x04

    private const val UNKNOWN = 0
    private const val PHONE = 1
    private const val LARGE_PHONE = 2 // SCREENLAYOUT_SIZE_LARGE like the Galaxy Note
    private const val SMALL_TABLET = 3 // SCREENLAYOUT_SIZE_LARGE like the Kindle Fire
    private const val MICRO_TABLET =
        4 // smaller than SMALL_TABLET but not quite a phablet. The Kindle Fire HD 7" is like this.
    private const val TABLET = 5

    private const val SMALL_TABLET_SMALLEST_WIDTH: Int = 590 // Kindle Fire is 600
    private const val MICRO_TABLET_SMALLEST_WIDTH: Int = 525 // Kindle Fire HD is 533

    private var mFormFactor: Int = 0

    private var mMetrics: DisplayMetrics? = null

    fun get(): Int {
        if (mFormFactor == UNKNOWN) {
            // It is very important to use the Application based Context/Resources for this check!
            // Within an Activity's Context/Resources the Configuration.screenLayout will be for its window, not the device itself
            // and if the window is resized (split screen, multi window, etc), then you won't be looking at the device screen size, but the window size.
            mFormFactor = determine(appContextFn())
        }

        if (mFormFactor == UNKNOWN) {
            // default to phone properites
            return PHONE
        }

        return mFormFactor
    }

    private fun determine(context: Context): Int {
        val screenLayout = context.getResources()
            .getConfiguration().screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK

        when (screenLayout) {
            SCREENLAYOUT_SIZE_XLARGE -> return TABLET

            Configuration.SCREENLAYOUT_SIZE_LARGE -> return calculateSize(context)

            Configuration.SCREENLAYOUT_SIZE_NORMAL, Configuration.SCREENLAYOUT_SIZE_SMALL -> return PHONE

            else -> return PHONE

        }
    }

    /**
     * Manually determine the size. Helpful if the size is not provided by the system or if the device is SCREENLAYOUT_SIZE_LARGE which
     * is a weird inbetween state.  Both the Galaxy Note and Kindle Fire fall into this category but the Note should be given the
     * phone layout and the Fire the tablet.
     *
     * OPT in the future if 3.0 is ever the minimum api, we can start to use the smallestWidth identifier to solve this issue.
     *
     * Relevant: http://developer.android.com/guide/practices/screens_support.html#range
     *
     * @return
     */
    private fun calculateSize(context: Context): Int {
        val smallestDp = context.getResources().getConfiguration().smallestScreenWidthDp.toFloat()

        if (smallestDp >= SMALL_TABLET_SMALLEST_WIDTH) {
            return SMALL_TABLET
        } else if (smallestDp >= MICRO_TABLET_SMALLEST_WIDTH) {
            return MICRO_TABLET
        } else {
            return LARGE_PHONE
        }
    }

    fun init() {
        get()
    }

    /**
     * Convert a Density Independant Pixel size to actual pixels based on the density of the device.  Example: 10dp on a xhdpi device will be converted to 20px
     * @param dp The density indendant size to convert
     * @return The px value
     */
    fun dpToPx(dp: Float): Int {
        return dpToPxF(dp).toInt()
    }

    private fun dpToPxF(dp: Float): Float {
        if (mMetrics == null) {
            mMetrics = appContextFn().resources.displayMetrics
        }
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mMetrics)
    }
}
