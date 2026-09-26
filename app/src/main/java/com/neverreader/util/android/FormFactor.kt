package com.neverreader.util.android

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import com.neverreader.app.App
import com.neverreader.app.App.Companion.getContext as appContextFn
import com.neverreader.app.App.Companion.from

object FormFactor {
    const val SCREENLAYOUT_SIZE_XLARGE: Int = 0x04

    private const val UNKNOWN = 0
    private const val PHONE = 1
    private const val LARGE_PHONE = 2 // SCREENLAYOUT_SIZE_LARGE like the Galaxy Note
    private const val SMALL_TABLET = 3 // SCREENLAYOUT_SIZE_LARGE like the Kindle Fire
    private const val MICRO_TABLET =
        4 // smaller than SMALL_TABLET but not quite a phablet. The Kindle Fire HD 7" is like this.
    private const val TABLET = 5

    const val SMALL_TABLET_SMALLEST_WIDTH: Int = 590 // Kindle Fire is 600
    const val MICRO_TABLET_SMALLEST_WIDTH: Int = 525 // Kindle Fire HD is 533

    var mFormFactor: Int = 0

    private var mMetrics: DisplayMetrics? = null

    fun get(): Int {
        if (mFormFactor == UNKNOWN) {
            // It is very important to use the Application based Context/Resources for this check!
            // Within an Activity's Context/Resources the Configuration.screenLayout will be for its window, not the device itself
            // and if the window is resized (split screen, multi window, etc), then you won't be looking at the device screen size, but the window size.
            mFormFactor = FormFactor.determine(appContextFn()!!)
        }

        if (mFormFactor == UNKNOWN) {
            // default to phone properites
            return PHONE
        }

        return mFormFactor
    }

    fun get(context: Context): Int {
        if (mFormFactor == UNKNOWN) {
            // It is very important to use the Application based Context/Resources for this check!
            // Within an Activity's Context/Resources the Configuration.screenLayout will be for its window, not the device itself
            // and if the window is resized (split screen, multi window, etc), then you won't be looking at the device screen size, but the window size.
            mFormFactor = determine(context)
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

    @get:Deprecated("")
    val isTablet: Boolean
        /**
         * Is this device a Tablet? Includes small and micro tablets.  Opposite of isPhone
         * **Becareful not to use this for layout/ui decisions, since windows can be resized for multi window and split screen.**
         *
         * @return
         */
        get() = isTablet(get())

    fun isTablet(context: Context): Boolean {
        return isTablet(get(context))
    }

    private fun isTablet(formFactor: Int): Boolean {
        return formFactor == TABLET || formFactor == SMALL_TABLET || formFactor == MICRO_TABLET
    }

    val isTabletLarge: Boolean
        /**
         * Is this device a Tablet?  Tablet must be SCREENLAYOUT_SIZE_XLARGE, SCREENLAYOUT_SIZE_LARGE sized devices are returned as false.
         *
         * A device like the Xoom
         *
         * **Becareful not to use this for layout/ui decisions, since windows can be resized for multi window and split screen.**
         *
         * @return
         */
        get() = get() == TABLET

    val isTabletSmall: Boolean
        /**
         * Is this device a small tablet?  Device will be on the larger end of SCREENLAYOUT_SIZE_LARGE Like a Nexus 7.
         * **Becareful not to use this for layout/ui decisions, since windows can be resized for multi window and split screen.**
         *
         * @return
         */
        get() = get() == SMALL_TABLET || get() == MICRO_TABLET


    val isPhone: Boolean
        /**
         * Is this device a handset sized device? This will also include phablet devices like the Galaxy Note.
         * **Becareful not to use this for layout/ui decisions, since windows can be resized for multi window and split screen.**
         *
         * @return
         */
        get() = get() == PHONE || get() == LARGE_PHONE


    /**
     * Should a subscreen be shown as a pop up dialog or a full screen activity?
     * @param context The Activity that is considering how to show a UI, or if no Activity is available, the current context.
     * It will handle null by defaulting to application context, but you should try to pass an activity since this is a ui decision.
     * @return true if should be opened as a popup
     */
    fun showSecondaryScreensInDialogs(context: Context): Boolean {
        val type = determine(findWindowContext(context))
        return type == TABLET || type == SMALL_TABLET
    }

    /**
     * Tries to find a context that best reflects the current window size.
     * For best results, pass in an Activity.
     * @param context
     * @return
     */
    private fun findWindowContext(context: Context): Context {
        var context = context
        var activity = ContextUtil.getActivity(context)
        if (activity == null) {
            // Let's try to grab what we know is the currently visible activity.
            activity = from(context)!!.activities().visible
        }
        if (activity != null) {
            context = activity
        }
        if (context == null) {
            // Not really expecting this case, but fallback to the application context.
            context = appContextFn()!!
        }
        return context
    }

    /**
     * Converts dp to px, if on phone, will use the phone value, if on tablet it will use the tablet value.
     */
    fun dpToPx(phone: Float, tablet: Float): Int {
        if (isTablet) {
            return dpToPx(tablet)
        } else {
            return dpToPx(phone)
        }
    }

    /**
     * Convert a Density Independant Pixel size to actual pixels based on the density of the device.  Example: 10dp on a xhdpi device will be converted to 20px
     * @param dp The density indendant size to convert
     * @return The px value
     */
    fun dpToPx(dp: Float): Int {
        return dpToPxF(dp).toInt()
    }

    fun dpToPxF(dp: Float): Float {
        if (mMetrics == null) {
            mMetrics = appContextFn()!!.getResources().getDisplayMetrics()
        }
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, mMetrics)
    }

    /**
     * Convert pixels to a density independant pixel value for the device.  For example: 20px on a xhdpi device will be converted to 10dp.
     * @param px The pixel value to convert
     * @return The dp value
     */
    fun pxToDp(px: Int): Float {
        if (mMetrics == null) {
            mMetrics = appContextFn()!!.getResources().getDisplayMetrics()
        }
        return px / mMetrics!!.density
    }

    /**
     * OPT NO NO NO NO bad developer... don't ever only target one device.  Are you really really sure you want to do this?
     * @return
     */
    fun isKindleFire(only7Inch: Boolean): Boolean {
        if (!"Amazon".equals(Build.MANUFACTURER, ignoreCase = true)) return false

        if (!"Kindle Fire".equals(
                Build.PRODUCT,
                ignoreCase = true
            ) && !"Kindle Fire".equals(Build.MODEL, ignoreCase = true)
        ) return false

        if (!only7Inch) return true

        if ("Kindle Fire".equals(Build.MODEL, ignoreCase = true) ||  // First Gen 7"
            "KFOT".equals(Build.MODEL, ignoreCase = true) ||  // Second Gen 7"
            "KFTT".equals(Build.MODEL, ignoreCase = true)
        )  // Second Gen HD 7"
            return true

        return false
    }

    val label: String
        /**
         * Get a human readable description of the kind of device this is.
         * @return
         */
        get() {
            val form = get()
            when (form) {
                PHONE -> return "Handset"
                LARGE_PHONE -> return "Phablet"
                SMALL_TABLET -> return "Small Tablet"
                MICRO_TABLET -> return "Micro Tablet"
                TABLET -> return "Tablet"
                UNKNOWN -> return "Unknown"
                else -> return "Unknown"
            }
        }

    /**
     * Gets the CSS class name for this form factor. Either "tablet", "smalltablet", or ("phone" or "null" depending on nullPhone).
     *
     * @param nullPhone true if a phone should return null, false if "phone"
     * @return
     */
    fun getClassKey(nullPhone: Boolean): String? {
        if (isTabletLarge) {
            return "tablet"
        } else if (isTabletSmall) {
            return "smalltablet"
        } else {
            return if (nullPhone) null else "phone"
        }
    }

    /**
     * Returns the current width of the window in dp. Note: this allocates
     * objects, so don't use this in repetitive or performance needed areas
     * like drawing code. Instead, grab the value and hold it until size
     * change events occur.
     *
     * @param activity measure this Activity's window
     */
    fun getWindowWidthDp(activity: Activity): Float {
        return pxToDp(getWindowWidthPx(activity))
    }

    /**
     * Returns the current width of the window in px. Note: this allocates
     * objects, so don't use this in repetitive or performance needed areas
     * like drawing code. Instead, grab the value and hold it until size
     * change events occur.
     *
     * @param activity measure this Activity's window
     */
    fun getWindowWidthPx(activity: Activity): Int {
        val metrics = DisplayMetrics()
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics)
        return metrics.widthPixels
    }
}
