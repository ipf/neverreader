package com.neverreader.ui.util

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.core.view.ViewCompat

/**
 * Helper for releasing resources or stopping animations/listeners etc for a [View] when no longer visible or off screen.
 * This isn't always clear how to accomplish within a [View] or requires a bunch of boilerplate. This helps you set it up
 * for any view with one call: [.install]
 */
class OnlyWhenVisibleHelper private constructor(
    private val view: View,
    onVisible: Runnable?,
    onHidden: Runnable?
) : OnAttachStateChangeListener, OnGlobalLayoutListener {
    private val onVisible: Runnable
    private val onHidden: Runnable

    /** Null means it hasn't been initialized yet.  */
    private var isVisible: Boolean? = null

    init {
        this.onVisible = if (onVisible != null) onVisible else Runnable {}
        this.onHidden = if (onHidden != null) onHidden else Runnable {}

        if (ViewCompat.isAttachedToWindow(view)) {
            onViewAttachedToWindow(view)
        }
        view.addOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(p0: View) {
        if (view.getViewTreeObserver() != null && view.getViewTreeObserver().isAlive()) {
            view.getViewTreeObserver().addOnGlobalLayoutListener(this)
        }
        update()
    }

    override fun onViewDetachedFromWindow(p0: View) {
        update()
        if (view.getViewTreeObserver() != null && view.getViewTreeObserver().isAlive()) {
            view.getViewTreeObserver().removeOnGlobalLayoutListener(this)
        }
    }

    override fun onGlobalLayout() {
        update()
    }

    private fun update() {
        val first = isVisible == null
        val was = isVisible == true
        val now =
            ViewCompat.isAttachedToWindow(view) && view.isShown && view.width > 0 && view.height > 0
        isVisible = now
        if (first || was != now) {
            if (isVisible == true) {
                onVisible.run()
            } else {
                onHidden.run()
            }
        }
    }

    companion object {
        /**
         * Runs `onVisible` if already visible, or 'onHidden' if already hidden and then any time it
         * changes visibility state runs the appropriate method.
         *
         *
         * Visible means attached to a window, has a non-zero size and it and its parents are [View.VISIBLE].
         */
        fun install(view: View, onVisible: Runnable?, onHidden: Runnable?): OnlyWhenVisibleHelper {
            return OnlyWhenVisibleHelper(view, onVisible, onHidden)
        }
    }
}
