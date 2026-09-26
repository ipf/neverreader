package com.neverreader.sdk.util

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.util.Property
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.transition.Transition
import androidx.transition.TransitionValues
import com.neverreader.util.android.animation.AnimatorEndListener
import com.neverreader.util.android.animation.WebViewArgbEvaluator

internal class ThemeChange : Transition() {
    override fun createAnimator(
        sceneRoot: ViewGroup,
        startValues: TransitionValues?,
        endValues: TransitionValues?
    ): Animator? {
        if (startValues == null || endValues == null) return null

        val animators: MutableCollection<Animator?> = ArrayList()

        val startColor = getColor(startValues.values)
        val endColor = getColor(endValues.values)
        if (startColor != null && endColor != null) {
            val background =
                ObjectAnimator.ofInt<View?>(
                    startValues.view,
                    BACKGROUND_COLOR,
                    startColor,
                    endColor
                )
            background.setEvaluator(ARGB_EVALUATOR)
            background.addListener(object : AnimatorEndListener() {
                override fun onAnimationEnd(animator: Animator) {
                    background.removeAllListeners()
                    startValues.view.background = startValues.values[KEY_BACKGROUND] as Drawable?
                }
            })
            animators.add(background)
        }

        if (startValues.view is TextView) {
            val textView = startValues.view as TextView
            val textColor = ObjectAnimator.ofInt<TextView?>(
                textView,
                TEXT_COLOR,
                ((startValues.values.get(ThemeChange.Companion.KEY_CURRENT_TEXT_COLOR) as kotlin.Int?)!!),
                ((endValues.values.get(ThemeChange.Companion.KEY_CURRENT_TEXT_COLOR) as kotlin.Int?)!!)
            )
            textColor.setEvaluator(ARGB_EVALUATOR)
            textColor.addListener(object : AnimatorEndListener() {
                override fun onAnimationEnd(animator: Animator) {
                    textColor.removeAllListeners()
                    textView.setTextColor((startValues.values[KEY_TEXT_COLOR] as ColorStateList?))
                }
            })
            animators.add(textColor)
        }

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(animators)
        animatorSet.duration = DURATION.toLong()
        animatorSet.interpolator = LinearInterpolator()
        return animatorSet
    }

    @ColorInt
    private fun getColor(values: MutableMap<String?, Any?>): Int? {
        val background = values.get(KEY_BACKGROUND)
        val backgroundForCurrentState = values[KEY_BACKGROUND_FOR_CURRENT_STATE]

        if (background is ColorDrawable) {
            return background.color
        } else if (backgroundForCurrentState is ColorDrawable) {
            return backgroundForCurrentState.color
        }

        return null
    }

    override fun captureStartValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    override fun captureEndValues(transitionValues: TransitionValues) {
        captureValues(transitionValues)
    }

    private fun captureValues(transitionValues: TransitionValues) {
        // Capture background to try and animate it.
        val background = transitionValues.view.background
        transitionValues.values[KEY_BACKGROUND] = background


        // In our case background is usually stateful (that's how we change themes),
        // so let's capture drawable for current state.
        if (background is StateListDrawable) {
            var stateListDrawable = background
            while (stateListDrawable.current is StateListDrawable) {
                stateListDrawable = stateListDrawable.current as StateListDrawable
            }
            transitionValues.values[KEY_BACKGROUND_FOR_CURRENT_STATE] = stateListDrawable.current
        }


        // Also capture text color if there is text
        if (transitionValues.view is TextView) {
            val textView = transitionValues.view as TextView
            transitionValues.values[KEY_TEXT_COLOR] = textView.textColors
            transitionValues.values[KEY_CURRENT_TEXT_COLOR] = textView.currentTextColor
        }
    }

    private class BackgroundColorProperty :
        Property<View, Int>(Int::class.java, "backgroundColor") {
        override fun set(`object`: View, value: Int) {
            `object`.setBackgroundColor(value)
        }

        override fun get(`object`: View): Int? {
            val background = `object`.background

            if (background is ColorDrawable) {
                return background.color
            }

            return null
        }
    }

    private class TextColorProperty : Property<TextView, Int>(Int::class.java, "textColor") {
        override fun set(`object`: TextView, value: Int) {
            `object`.setTextColor(value)
        }

        override fun get(`object`: TextView): Int {
            return `object`.currentTextColor
        }
    }

    companion object {
        const val DURATION: Int = 300
        val ARGB_EVALUATOR: TypeEvaluator<*> = WebViewArgbEvaluator()

        private const val KEY_BACKGROUND = "neverreader:themeChange:background"
        private const val KEY_BACKGROUND_FOR_CURRENT_STATE =
            "neverreader:themeChange:backgroundDrawableForCurrentState"
        private const val KEY_TEXT_COLOR = "neverreader:themeChange:textColor"
        private const val KEY_CURRENT_TEXT_COLOR = "neverreader:themeChange:currentTextColor"

        private val BACKGROUND_COLOR: Property<View, Int> = BackgroundColorProperty()
        private val TEXT_COLOR: Property<TextView, Int> = TextColorProperty()
    }
}
