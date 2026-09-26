package com.neverreader.ui.view.menu

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import androidx.annotation.IdRes
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedLinearLayout

/**
 * A picker for for app theme choices
 */
class ThemeToggle : ThemedLinearLayout {
    enum class ThemeChoice(@param:IdRes val resId: Int) {
        LIGHT(R.id.theme_light),
        DARK(R.id.theme_dark),
        AUTO(R.id.theme_auto)
    }

    private val binder: Binder = Binder()

    private var onThemeSelectedListener: OnThemeSelectedListener? = null
    private var themeChoiceViews: SparseArray<View?>? = null
    private var selector: Drawable? = null
    private var currentSelection: View? = null

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context) : super(context!!) {
        init()
    }

    override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        val `val` = super.drawChild(canvas, child, drawingTime)

        if (child === currentSelection) {
            selector!!.setBounds(
                child.getLeft(),
                child.getTop(),
                child.getRight(),
                child.getBottom()
            )
            selector!!.draw(canvas)
            selector!!.setState(child.getDrawableState())
        }

        return `val`
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_theme_toggle, this, true)

        setOrientation(HORIZONTAL)

        selector = ThemeToggleSelectionDrawable(getContext())

        themeChoiceViews = SparseArray<View?>()

        for (choice in ThemeChoice.entries) {
            val v = findViewById<View>(choice.resId)
            v.setOnClickListener(OnClickListener { v1: View? ->
                if (onThemeSelectedListener != null) {
                    onThemeSelectedListener!!.onThemeSelected(v1, choice)
                }
                setCurrentSelection(v1)
            })
            themeChoiceViews!!.put(choice.resId, v)
        }

        bind().clear()
    }

    private fun setCurrentSelection(v1: View?) {
        currentSelection = v1
        invalidate()
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            listener(null)
            availableThemes(*ThemeChoice.entries.toTypedArray())
            theme(null)
            setCurrentSelection(themeChoiceViews!!.valueAt(0))
            return this
        }

        fun availableThemes(vararg choices: ThemeChoice): Binder {
            // clear all views
            for (i in 0..<getChildCount()) {
                getChildAt(i).setVisibility(GONE)
            }
            // set available views visible
            for (choice in choices) {
                themeChoiceViews!!.get(choice.resId)!!.setVisibility(VISIBLE)
            }
            return this
        }

        fun theme(value: ThemeChoice?): Binder {
            if (value != null) {
                setCurrentSelection(themeChoiceViews!!.get(value.resId))
            }
            return this
        }

        fun listener(listener: OnThemeSelectedListener?): Binder {
            onThemeSelectedListener = listener
            return this
        }
    }

    interface OnThemeSelectedListener {
        fun onThemeSelected(view: View?, value: ThemeChoice?)
    }
}
