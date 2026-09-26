package com.neverreader.ui.view.themed

import android.R
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Paint
import android.text.Selection
import android.text.Spannable
import android.text.method.LinkMovementMethod
import android.text.method.Touch
import android.text.style.ClickableSpan
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import com.neverreader.ui.analytics.Engageable
import com.neverreader.ui.analytics.EngageableHelper
import com.neverreader.ui.analytics.EngagementListener
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.text.Fonts
import com.neverreader.ui.text.PressableSpan
import com.neverreader.ui.text.TextViewUtil
import com.neverreader.ui.util.NestedColorStateList
import com.neverreader.ui.view.visualmargin.VisualMargin
import kotlin.math.ceil

open class ThemedTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr), VisualMargin, Engageable {
    private val engageable = EngageableHelper()

    init {
        setPaintFlags(getPaintFlags() or Paint.SUBPIXEL_TEXT_FLAG)

        val a = getContext().obtainStyledAttributes(
            attrs,
            com.neverreader.ui.R.styleable.ThemedTextView,
            defStyleAttr,
            0
        )
        applyTextAppearanceFromAttributes(a)
        a.recycle()

        if (!isInEditMode()) {
            engageable.obtainStyledAttributes(getContext(), attrs)
            engageable.uiEntityType = UiEntityable.Type.BUTTON
        }
    }

    override fun setTextAppearance(context: Context?, resid: Int) {
        super.setTextAppearance(context, resid)

        val a = getContext().obtainStyledAttributes(
            resid,
            com.neverreader.ui.R.styleable.ThemedTextView
        )
        applyTextAppearanceFromAttributes(a)
        a.recycle()
    }

    private fun applyTextAppearanceFromAttributes(a: TypedArray) {
        if (a.hasValue(com.neverreader.ui.R.styleable.ThemedTextView_typeface) && !isInEditMode()) {
            setTypeface(
                Fonts.get(
                    getContext(),
                    a.getInt(com.neverreader.ui.R.styleable.ThemedTextView_typeface, 0)
                )
            )
        }

        if (a.getBoolean(com.neverreader.ui.R.styleable.ThemedTextView_visualPadding, false)) {
            TextViewUtil.setVisualTextPadding(
                this,
                getPaddingLeft(),
                getPaddingTop(),
                getPaddingRight(),
                getPaddingBottom()
            )
        } else if (a.getBoolean(
                com.neverreader.ui.R.styleable.ThemedTextView_paddingVerticalCenter,
                false
            )
        ) {
            TextViewUtil.verticallyCenterPadding(this)
        }

        val colors =
            a.getResourceId(com.neverreader.ui.R.styleable.ThemedTextView_compatTextColor, 0)
        if (colors != 0) {
            if (!isInEditMode()) {
                val colorStateList = NestedColorStateList.get(getContext(), colors)
                setTextColor(colorStateList)
                setLinkTextColor(colorStateList)
            }
        }
    }

    public override fun onCreateDrawableState(extraSpace: Int): IntArray? {
        val state = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(state, AppThemeUtil.getState(this))
        return state
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        invalidate() // Force paints to update, sometimes with a TextView if the text color didn't change, it skips invalidating the background which might have a different state.
    }

    fun setBold(bold: Boolean) {
        if (bold) {
            setTypeface(Fonts.get(context, Fonts.Font.GRAPHIK_LCG_BOLD))
        } else {
            setTypeface(Fonts.get(context, Fonts.Font.GRAPHIK_LCG_REGULAR))
        }
    }

    fun setMovementMethodForLinks(enabled: Boolean) {
        if (enabled) {
            movementMethod = FixedLinkMethod()
        } else {
            movementMethod = defaultMovementMethod
        }
    }

    @SuppressLint("ClickableViewAccessibility") // Super is always called, so this lint warning is too paranoid.
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (movementMethod is FixedLinkMethod) {
            // Only consume the event if on a link
            val method: FixedLinkMethod = movementMethod as FixedLinkMethod
            super.onTouchEvent(event)
            val isLinkTouch = method.isTouching
            when (event.getAction()) {
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    method.isTouching = false
                    method.clearPressedSpans()
                }
            }
            return isLinkTouch
        } else {
            return super.onTouchEvent(event)
        }
    }

    override fun prepareVisualDescent(): Boolean {
        return false
    }

    override fun prepareVisualAscent(): Boolean {
        return false
    }

    override fun visualAscent(): Int {
        return ceil(TextViewUtil.ascent(this).toDouble()).toInt() + getPaddingTop()
    }

    override fun visualDescent(): Int {
        return ceil(TextViewUtil.descent(this).toDouble()).toInt() + getPaddingBottom()
    }

    override var uiEntityIdentifier: String?
        get() = engageable.uiEntityIdentifier
        set(uiEntityIdentifier) {
            engageable.uiEntityIdentifier = uiEntityIdentifier
        }

    override val uiEntityType: UiEntityable.Type?
        get() = engageable.uiEntityType

    override var uiEntityComponentDetail: String?
        get() = engageable.uiEntityComponentDetail
        set(value) {
            engageable.uiEntityComponentDetail = value
        }

    override val uiEntityLabel: String?
        get() = engageable.uiEntityLabel

    override fun setEngagementListener(listener: EngagementListener?) {
        engageable.setEngagementListener(listener)
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        super.setOnClickListener(engageable.getWrappedClickListener(listener))
    }

    /**
     * This is a workaround to allow touches to pass through to the views behind
     * if they aren't touching a link within the text view. The normal LinkMovementMethod
     * eats all touch events even if it isn't on a ClickableSpan.
     *
     *
     * This also provides support for [PressableSpan].
     */
    inner class FixedLinkMethod : LinkMovementMethod() {
        private val pressing: MutableSet<PressableSpan> = HashSet<PressableSpan>()
        var isTouching = false

        override fun onTouchEvent(
            widget: TextView,
            buffer: Spannable,
            event: MotionEvent
        ): Boolean {
            val action = event.getAction()

            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_CANCEL) {
                var x = event.getX().toInt()
                var y = event.getY().toInt()

                x -= widget.getTotalPaddingLeft()
                y -= widget.getTotalPaddingTop()

                x += widget.getScrollX()
                y += widget.getScrollY()

                val layout = widget.getLayout()
                val line = layout.getLineForVertical(y)
                val off = layout.getOffsetForHorizontal(line, x.toFloat())


                // Support for PressableSpan
                val pressables =
                    buffer.getSpans(off, off, PressableSpan::class.java)
                if (pressables.size != 0) {
                    for (span in pressables) {
                        when (action) {
                            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> clearPressedSpans()
                            MotionEvent.ACTION_DOWN -> {
                                span.setPressed(true)
                                if (pressing.add(span)) {
                                    refreshDrawableState()
                                }
                            }
                        }
                    }
                }

                // ClickableSpan workaround
                val links = buffer.getSpans(off, off, ClickableSpan::class.java)
                if (links.size != 0) {
                    val link = links[0]
                    if (action == MotionEvent.ACTION_UP) {
                        link.onClick(widget)
                    } else if (action == MotionEvent.ACTION_DOWN) {
                        Selection.setSelection(
                            buffer,
                            buffer.getSpanStart(link),
                            buffer.getSpanEnd(link)
                        )
                    }
                    isTouching = true
                    return true
                } else {
                    Selection.removeSelection(buffer)
                    return false
                }
            }

            return Touch.onTouchEvent(widget, buffer, event)
        }

        fun clearPressedSpans() {
            if (!pressing.isEmpty()) {
                for (span in pressing) {
                    span.setPressed(false)
                }
                pressing.clear()
                refreshDrawableState()
            }
        }
    }

    fun setTextAndUpdateEnUsLabel(@StringRes resId: Int) {
        if (resId != 0) {
            setText(resId)
            if (!isInEditMode()) {
                engageable.updateEnUsLabel(getContext(), resId)
            }
        } else {
            setText(null)
            engageable.updateEnUsLabel(null)
        }
    }

    fun setTextAndUpdateEnUsLabel(displayText: CharSequence?, label: String?) {
        setText(displayText)
        engageable.updateEnUsLabel(label)
    }
}
