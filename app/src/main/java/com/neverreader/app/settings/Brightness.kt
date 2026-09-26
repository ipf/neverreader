package com.neverreader.app.settings

import com.neverreader.sdk.util.AbsNeverReaderActivity
import kotlin.math.max
import kotlin.math.min

object Brightness {
    // OPT does software dimming this slow down drawing? what about hardware acceleration? is this worth the extra dimness?
    // OPT i disabled software dimming for now as it seemed to be too dark in many cases and possibly? causes extra drawing calls.
    //     if you turn it back on, uncomment it NeverReaderActivityRootView
    // OPT if we stick with only hardware, clean up this code to remove the uneeded software dimming code.
    private var mUsingCustomBrightness = false
    private var mHardware = 0f // 0 to 1 // 0 is dim, 1 is bright
    private const val mSoftware =
        0 // SOFTWARE_RANGE to 0 // SOFTWARE_RANGE is dim, 0 is bright, used as overlay alpha

    fun applyBrightnessIfSet(activity: AbsNeverReaderActivity) {
        if (!mUsingCustomBrightness) return

        applyBrightnessToActivity(activity, mHardware, mSoftware)
    }

    private fun applyBrightnessToActivity(
        activity: AbsNeverReaderActivity,
        hardware: Float,
        software: Int
    ) {
        // Hardware
        val window = activity.getWindow()
        val lp = window.getAttributes()
        lp.screenBrightness = mHardware
        window.setAttributes(lp)


        //float nb = window.getAttributes().screenBrightness;

        // Software
        activity.setBrightnessOverlay(software)
    }

    var brightness: Float
        /**
         * @return Returns the hardware brightness last set in [.setBrightness]
         */
        get() = mHardware
        /**
         * Set the screen brightness
         *
         * @param hardware percent of hardware brightness. range of 0 to 1.0f. 1.0f is full brightness.
         */
        set(hardware) {
            var hardware = hardware
            mUsingCustomBrightness = true


            // Set Hardware Brightness
            hardware = min(hardware, 1.0f)
            hardware = max(
                hardware,
                0.02f
            ) // QUESTION what is the min value we can use here? // create a fail-safe in case it does turn off the screen?
            mHardware = hardware
        }
}
