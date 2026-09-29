package com.neverreader.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.neverreader.ui.util.LazyBitmapDrawable.SupportsPlaceholder

/**
 * A variant of [LazyBitmapDrawable] when you need the drawable to have
 * intrinsic size that some view is basing its measuring on, for example when
 * being used as a image in an ImageView with wrap_content in one or both directions.
 *
 *
 * This will initially have no intrinsic size, but after it loads the image it will have one
 * and attempt to update the view for you.
 */
class LazyInstrinicBitmapDrawable(
    private val context: Context,
    lazy: LazyBitmap,
    private val updater: ViewSizeUpdater
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var bitmap: Bitmap? = null
    private var width = 0
    private var height = 0

    init {
        paint.setFilterBitmap(false)
        lazy.fill(0, 0, LazyBitmap.Loaded { bitmap: Bitmap? -> this.setBitmap(bitmap) }, null)
    }

    private fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
        if (bitmap != null) {
            val dpi = context.getResources().getDisplayMetrics().densityDpi
            width = bitmap.getScaledWidth(dpi)
            height = bitmap.getScaledHeight(dpi)
        } else {
            width = 0
            height = 0
        }
        invalidateSelf()
        updater.onDrawableSizeChanged(this)
    }

    override fun getIntrinsicWidth(): Int {
        return if (width > 0) width else -1
    }

    override fun getIntrinsicHeight(): Int {
        return if (height > 0) height else -1
    }

    override fun draw(canvas: Canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap!!, null, getBounds(), paint)
        } else if (getCallback() is SupportsPlaceholder) {
            (getCallback() as SupportsPlaceholder).drawPlaceholder(canvas, getBounds(), getState())
        }
    }

    override fun setAlpha(alpha: Int) {
        paint.setAlpha(alpha)
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.setColorFilter(colorFilter)
    }

    /** The base declaration is deprecated and has done nothing since API 3. */
    @Deprecated("Deprecated in the Drawable base class")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun isStateful(): Boolean {
        return true
    }

    override fun onStateChange(state: IntArray): Boolean {
        super.onStateChange(state)
        // Always return true, assuming the placeholder is stateful, could optimize to check with the placeholder
        return true
    }

    fun interface ViewSizeUpdater {
        fun onDrawableSizeChanged(drawable: LazyInstrinicBitmapDrawable?)

        companion object {
            /**
             * A [ViewSizeUpdater] for when a [LazyInstrinicBitmapDrawable] is an ImageView's drawable.
             */
            val IMAGE_VIEW: ViewSizeUpdater =
                ViewSizeUpdater { drawable: LazyInstrinicBitmapDrawable? ->
                    val view = drawable!!.getCallback() as ImageView?
                    if (view != null) {
                        // Need to force it to update its internal mDrawableWidth and height, most reliable way is to just clear and reset the drawable.
                        view.setImageDrawable(null)
                        view.setImageDrawable(drawable)
                    }
                }
        }
    }
}
