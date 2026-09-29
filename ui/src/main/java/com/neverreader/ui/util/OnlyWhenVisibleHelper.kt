package com.neverreader.ui.util

import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.view.ViewTreeObserver.OnGlobalLayoutListener

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
    private val onVisible: Runnable = onVisible ?: Runnable {}
    private val onHidden: Runnable = onHidden ?: Runnable {}

    /** Null means it hasn't been initialized yet.  */
    private var isVisible: Boolean? = null

    init {

        if (view.isAttachedToWindow) {
            onViewAttachedToWindow(view)
        }
        view.addOnAttachStateChangeListener(this)
    }

    override fun onViewAttachedToWindow(p0: View) {
        if (view.viewTreeObserver != null && view.viewTreeObserver.isAlive) {
            view.viewTreeObserver.addOnGlobalLayoutListener(this)
        }
        update()
    }

    override fun onViewDetachedFromWindow(p0: View) {
        update()
        if (view.viewTreeObserver != null && view.viewTreeObserver.isAlive) {
            view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        }
    }

    override fun onGlobalLayout() {
        update()
    }

    private fun update() {
        val first = isVisible == null
        val was = isVisible == true
        val now =
            view.isAttachedToWindow && view.isShown && view.width > 0 && view.height > 0
        isVisible = now
        if (first || was != now) {
            if (isVisible == true) {
                onVisible.run()
            } else {
                onHidden.run()
            }
        }
    }
}
