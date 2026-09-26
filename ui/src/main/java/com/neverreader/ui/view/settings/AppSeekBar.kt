package com.neverreader.ui.view.settings

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.Gravity
import com.neverreader.ui.R
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.view.themed.ThemedSeekBar

class AppSeekBar : ThemedSeekBar {
    private var thumbRadius = 0
    private var thumbShadowLength = 0
    private var thumbStroke = 0
    private var trackHeight = 0

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context)
    }

    private fun init(context: Context) {
        thumbRadius = dpToPxInt(context, 11f)
        thumbShadowLength = dpToPxInt(context, 6.5f)
        thumbStroke = dpToPxInt(context, 1.1f)
        trackHeight = dpToPxInt(context, 5f)

        setThumb(Handle(getResources()))
        setBackgroundDrawable(null) // Removes the ripple on L

        val progressColorRes = R.color.nr_themed_grey_4

        val progress: Drawable = ClipDrawable(
            Progress(getResources(), progressColorRes),
            Gravity.LEFT,
            ClipDrawable.HORIZONTAL
        )
        val track: Drawable = Progress(getResources(), R.color.nr_themed_grey_5)

        setProgressDrawable(LayerDrawable(arrayOf<Drawable>(track, progress)))

        // Adjust the thumb offset so that it ends at edge of the track rather than going beyond it.
        val offset = dpToPxInt(context, 9f)
        setThumbOffset(offset)
        setPadding(offset, 0, offset, 0)
    }

    private val realProgressDrawable: Drawable?
        /**
         * A Progress Drawable is normally a layer drawable with the track and the progress bar. getProgressDrawable returns the LayerDrawable.
         * This method pulls out the progress drawable inside of the layer drawable.  If getProgressDrawable is null or not a layer drawable it will return null
         *
         *
         * Also if this is called before the constructors are completed, it will also return null.
         */
        get() {
            val drawable = getProgressDrawable()
            if (drawable != null && drawable is LayerDrawable) {
                return drawable.findDrawableByLayerId(android.R.id.progress)
            } else {
                return null
            }
        }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        val progressDrawable = this.realProgressDrawable
        if (progressDrawable != null) {
            progressDrawable.setBounds(getProgressDrawable().getBounds())
            progressDrawable.setState(getDrawableState())
        }
        super.onDraw(canvas!!)
    }

    inner class Progress(res: Resources, colorResource: Int) : ThemeDrawable(res) {
        private val paint = Paint()
        private val colors: ColorStateList
        private val path = PillPath()

        init {
            registerPaint(paint)
            paint.setStyle(Paint.Style.FILL)
            colors = res.getColorStateList(colorResource)
        }

        override fun updatePaints(newState: IntArray?) {
            paint.setColor(colors.getColorForState(newState, Color.TRANSPARENT))
        }

        override fun onBoundsChange(bounds: Rect) {
            super.onBoundsChange(bounds)

            val r = trackHeight / 2f
            path.setBounds(
                bounds.left.toFloat(),
                bounds.exactCenterY() - r,
                bounds.right.toFloat(),
                bounds.exactCenterY() + r
            )
        }

        override fun draw(canvas: Canvas) {
            path.draw(canvas, paint)
        }
    }

    inner class Handle(res: Resources) : ThemeDrawable(res) {
        private val strokePaint = Paint()
        private val bgPaint = Paint()
        private val strokeColors: ColorStateList
        private val bgColors: ColorStateList
        private val radius = thumbRadius + thumbShadowLength

        init {
            registerPaint(bgPaint)
            registerPaint(strokePaint)

            bgPaint.setStyle(Paint.Style.FILL)
            strokePaint.setStyle(Paint.Style.FILL)

            bgColors = res.getColorStateList(R.color.nr_seekbar_handle)
            strokeColors = res.getColorStateList(R.color.nr_themed_grey_1)
        }

        override fun updatePaints(newState: IntArray?) {
            strokePaint.setColor(strokeColors.getColorForState(newState, Color.TRANSPARENT))
            bgPaint.setColor(bgColors.getColorForState(newState, Color.TRANSPARENT))
        }

        override fun draw(canvas: Canvas) {
            val b = getBounds()
            canvas.drawCircle(
                b.exactCenterX(),
                b.exactCenterY(),
                thumbRadius.toFloat(),
                strokePaint
            )
            canvas.drawCircle(
                b.exactCenterX(),
                b.exactCenterY(),
                (thumbRadius - thumbStroke).toFloat(),
                bgPaint
            )
        }

        override fun getIntrinsicWidth(): Int {
            return radius * 2
        }

        override fun getIntrinsicHeight(): Int {
            return radius * 2
        }
    }

    /** Handles the theme state changing of the Drawable. Make sure you [.registerPaint] for each Paint you create  */
    abstract inner class ThemeDrawable(res: Resources?) : Drawable() {
        private val paints = ArrayList<Paint>()

        protected fun registerPaint(paint: Paint) {
            paints.add(paint)
            paint.setAntiAlias(true)
        }

        override fun onStateChange(state: IntArray): Boolean {
            super.onStateChange(state)
            updatePaints(state)
            return true
        }

        /** The theme state has changed, you should update your paints to match the current state  */
        protected abstract fun updatePaints(newState: IntArray?)

        override fun setAlpha(alpha: Int) {
            for (paint in paints) {
                paint.setAlpha(alpha)
            }
        }

        override fun setColorFilter(cf: ColorFilter?) {
            for (paint in paints) {
                paint.setColorFilter(cf)
            }
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSLUCENT
        }

        override fun isStateful(): Boolean {
            return true
        }
    }

    /**
     * Creates a [Path] shaped like a pill where the left and right sides are half circles and the center
     * is a rectangle.
     *
     *
     * Invoke setBounds to define the area and [.draw] to draw.
     *
     *
     * If the bounds width and height is equal, a circle will be drawn.
     */
    private inner class PillPath {
        private val mPath = Path()
        private val mReuse = RectF()

        var width: Float = 0f
            private set

        /**
         * @see .setBounds
         */
        fun setBounds(bounds: RectF) {
            setBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)
        }

        /**
         * Set the area of the pill.
         *
         *
         * Imagine a circle drawn in the center of the bounds with the diameter equal to the height so it fills
         * the height. Then if there is remaining width on the left and right sides, the circle is cut down the center and each half circle
         * moved horizontally to the far left and right. Then a rectangle fills the space between them.
         * @param left
         * @param top
         * @param right
         * @param bottom
         */
        fun setBounds(left: Float, top: Float, right: Float, bottom: Float) {
            val width = right - left
            val height = bottom - top
            val r = height / 2f
            val centerX = left + (width / 2f)
            val centerY = top + (height / 2f)
            mPath.rewind()

            if (width == height) {
                mPath.addCircle(centerX, centerY, r, Path.Direction.CW)
            } else {
                val tlX = left + r // top left
                val tlY = top
                val trX = right - r // top right
                val trY = top
                val blX = left + r // bottom left
                val blY = bottom
                val brX = right - r // bottom right
                val brY = bottom

                // Top straight line
                mPath.moveTo(tlX, tlY)
                mPath.lineTo(trX, trY)

                // Right side half circle cap
                mReuse.set(trX - r, trY, brX + r, brY)
                mPath.arcTo(mReuse, 270f, 180f)

                // Bottom straight line
                mPath.lineTo(blX, blY)

                // Left side half circle cap
                mReuse.set(tlX - r, tlY, blX + r, blY)
                mPath.arcTo(mReuse, 90f, 180f)
            }

            mPath.close()
            this.width = right - left
        }

        fun draw(canvas: Canvas, paint: Paint) {
            canvas.drawPath(mPath, paint)
        }
    }
}
