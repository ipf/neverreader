package com.neverreader.app.settings.rotation

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.neverreader.app.R
import com.neverreader.app.settings.rotation.interf.RotationLockView
import com.neverreader.app.settings.rotation.interf.RotationLockView.OnClick
import com.neverreader.ui.util.CheckableHelper
import com.neverreader.ui.util.NestedColorStateList.get
import com.neverreader.ui.view.button.ButtonBoxDrawable
import com.neverreader.ui.view.checkable.CheckableImageView

class AppRotationLockView : FrameLayout, RotationLockView {
    private var fadeOut: FadeOut? = null
    private var toggle: CheckableImageView? = null
    private var fading = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.view_rotation_lock, this, true)
        setOnTouchListener(OnTouchListener { v: View?, event: MotionEvent? ->
            scheduleFadeOut(0)
            false
        })
        toggle = findViewById<CheckableImageView>(R.id.rotation_lock_toggle)
        toggle!!.setCheckable(true)

        val bg: Drawable = ButtonBoxDrawable(context, R.color.nr_rotation_lock_bg, 0, 4f)
        bg.setAlpha((0.8 * 255).toInt())
        toggle!!.setBackgroundDrawable(bg)

        val lockimage = AppCompatResources.getDrawable(context, R.drawable.ic_rotation)
        DrawableCompat.setTintList(lockimage!!, get(context, R.color.nr_rotation_lock))
        toggle!!.setImageDrawable(lockimage)
    }

    private fun scheduleFadeOut(delay: Long) {
        if (!fading) {
            if (fadeOut == null) {
                fadeOut = FadeOut()
            }
            removeCallbacks(fadeOut)
            postDelayed(fadeOut, delay)
        }
    }

    override fun setOnToggleClick(onclick: OnClick?) {
        toggle!!.setOnClickListener(OnClickListener { v: View? ->
            onclick!!.onClick(toggle!!.isChecked)
        })
    }

    override fun show(checked: Boolean) {
        toggle!!.clearAnimation()
        visibility = VISIBLE
        toggle!!.isEnabled = true
        toggle!!.isChecked = checked
        toggle!!.visibility = VISIBLE
        scheduleFadeOut(LOCK_SHOW_DURATION_MS.toLong())
    }

    override fun hide() {
        visibility = GONE
    }

    private inner class FadeOut : Runnable {
        override fun run() {
            val outAlpha: Animation = AlphaAnimation(1f, 0f)
            outAlpha.interpolator = AccelerateDecelerateInterpolator()
            outAlpha.duration = LOCK_FADEOUT_DURATION_MS.toLong()
            outAlpha.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {
                    fading = true
                }

                override fun onAnimationRepeat(animation: Animation?) {
                }

                override fun onAnimationEnd(animation: Animation?) {
                    toggle!!.isEnabled = false
                    hide()
                    fading = false
                }
            })
            toggle!!.startAnimation(outAlpha)
        }
    }

    companion object {
        private const val LOCK_SHOW_DURATION_MS = 4000
        private const val LOCK_FADEOUT_DURATION_MS = 620
    }
}
