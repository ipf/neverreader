package com.neverreader.ui.view.item

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.DrawableCompat
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.util.android.drawable.ColorUtil

/**
 * Overlaid on thumbnails to indicate the Item is a video.
 * Ideally we would have used a layer-list here, since It's
 * pretty simple, but doesn't appear that vector compat
 * works for layer-list without having to enable
 * AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
 * which is heavy-handed just to support that.
 *
 * Note: This drawable does not change colors or appearance based on
 * themes, it is always the same colors.
 */
class ItemVideoIndicatorDrawable private constructor(
    context: Context,
    icon: Int,
    circleRadius: Int
) : Drawable() {
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleRadius: Int
    private val icon: Drawable?
    private val iconWidthRadius: Int
    private val iconHeightRadius: Int

    init {
        circlePaint.setStyle(Paint.Style.FILL)
        circlePaint.setColor(
            ColorUtil.setAlpha(
                0.8f,
                context.getResources().getColor(R.color.nr_grey_2)
            )
        )

        this.circleRadius = circleRadius
        this.icon = VectorDrawableCompat.create(context.getResources(), icon, null)
        DrawableCompat.setTint(this.icon!!, Color.WHITE)
        this.iconWidthRadius = this.icon.getIntrinsicWidth() / 2
        this.iconHeightRadius = this.icon.getIntrinsicHeight() / 2
    }

    override fun getIntrinsicHeight(): Int {
        return circleRadius * 2
    }

    override fun getIntrinsicWidth(): Int {
        return circleRadius * 2
    }

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val cx = getBounds().centerX()
        val cy = getBounds().centerY()
        icon!!.setBounds(
            cx - iconWidthRadius, cy - iconHeightRadius,
            cx + iconWidthRadius, cy + iconHeightRadius
        )
    }

    override fun draw(canvas: Canvas) {
        canvas.drawCircle(
            getBounds().centerX().toFloat(),
            getBounds().centerY().toFloat(),
            circleRadius.toFloat(),
            circlePaint
        )
        icon!!.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        circlePaint.alpha = alpha
        icon!!.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        circlePaint.colorFilter = colorFilter
        icon!!.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    companion object {
        fun forItemRow(context: Context): Drawable {
            return ItemVideoIndicatorDrawable(
                context,
                R.drawable.ic_nr_play_mini,
                dpToPxInt(context, (34 / 2).toFloat())
            )
        }

        fun forItemTile(context: Context): Drawable {
            return ItemVideoIndicatorDrawable(
                context,
                R.drawable.ic_nr_play_mini,
                dpToPxInt(context, (48 / 2).toFloat())
            )
        }

        fun forDiscoverTile(context: Context): Drawable {
            return ItemVideoIndicatorDrawable(
                context,
                R.drawable.ic_nr_play_solid,
                dpToPxInt(context, (72 / 2).toFloat())
            )
        }
    }
}
