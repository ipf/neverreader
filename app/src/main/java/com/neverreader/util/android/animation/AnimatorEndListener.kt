package com.neverreader.util.android.animation

import android.animation.Animator

/**
 * An [Animator.AnimatorListener] who's only abstract method is [.onAnimationEnd] to keep
 * code cleaner when you only need that method. The other methods are no-ops.
 *
 * @author max
 */
abstract class AnimatorEndListener : Animator.AnimatorListener {
    private var wasCanceled = false

    override fun onAnimationCancel(p0: Animator) {
        wasCanceled = true
    }

    override fun onAnimationRepeat(p0: Animator) {
    }

    override fun onAnimationStart(p0: Animator) {
        wasCanceled = false
    }

}
