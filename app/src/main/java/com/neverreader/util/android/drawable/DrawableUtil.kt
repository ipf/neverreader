package com.neverreader.util.android.drawable

import android.graphics.drawable.Drawable
import android.view.MotionEvent

object DrawableUtil {
    /**
     * Applies the motion events x and y to [Drawable.setHotspot].
     *
     * @param drawable ok as null, it just do nothing.
     */
    fun setHotspot(drawable: Drawable?, event: MotionEvent) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && drawable != null) {
            drawable.setHotspot(event.getX(), event.getY())
        }
    }
}
