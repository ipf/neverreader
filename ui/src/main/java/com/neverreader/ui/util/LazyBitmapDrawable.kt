package com.neverreader.ui.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import com.neverreader.ui.util.LazyBitmap.Canceller.Companion.cancelAndRenew

/**
 * A drawable that lazily and asynchronously loads a bitmap from an external source.
 * It fills whatever the bounds of this drawable are, so it means your view must declare
 * explict bounds, it can not depend on the intrinsic bounds of this drawable.
 * See [LazyInstrinicBitmapDrawable] for that use case.
 *
 *
 * Will reload any time the bounds change.
 *
 *
 * If your view implements [SupportsPlaceholder] you will receive a callback
 * when a placeholder view should be drawn, such as when it is loading.
 */
class LazyBitmapDrawable(private val lazy: LazyBitmap) : Drawable() {
    private val onLoaded: LazyBitmap.Loaded
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var bitmap: Bitmap? = null
    private var canceller: LazyBitmap.Canceller? = null

    init {
        this.onLoaded = LazyBitmap.Loaded { bitmap: Bitmap? -> this.setBitmap(bitmap) }
        paint.setFilterBitmap(false)
    }

    interface SupportsPlaceholder {
        fun drawPlaceholder(canvas: Canvas?, bounds: Rect?, state: IntArray?)
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        bitmap = null
        canceller = cancelAndRenew(canceller)
        lazy.fill(bounds.width(), bounds.height(), onLoaded, canceller)
    }

    private fun setBitmap(bitmap: Bitmap?) {
        this.bitmap = bitmap
        invalidateSelf()
    }

    override fun getIntrinsicWidth(): Int {
        return -1
    }

    override fun getIntrinsicHeight(): Int {
        return -1
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

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun isStateful(): Boolean {
        return true
    }

    override fun onStateChange(state: IntArray): Boolean {
        super.onStateChange(state)
        // Always return true, assuming the placeholder is stateful, could optimize to check with the placeholder
        return true
    }
}
