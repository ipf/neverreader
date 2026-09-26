package com.neverreader.ui.view.menu

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedConstraintLayout
import com.squareup.phrase.Phrase.Companion.from

class SettingIncrementor : ThemedConstraintLayout {
    private val binder: Binder = Binder()

    private var up: View? = null
    private var down: View? = null
    private var icon: ImageView? = null

    constructor(context: Context) : super(context!!) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
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
        LayoutInflater.from(getContext()).inflate(R.layout.view_setting_incrementor, this, true)
        up = findViewById<View>(R.id.setting_up)
        down = findViewById<View>(R.id.setting_down)
        icon = findViewById<ImageView>(R.id.setting_icon)

        bind().clear()
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            upListener(null)
            downListener(null)
            icon(0)
            upEnabled(true)
            downEnabled(true)
            label(0)
            return this
        }

        fun upListener(listener: OnClickListener?): Binder {
            up!!.setOnClickListener(listener)
            return this
        }

        fun downListener(listener: OnClickListener?): Binder {
            down!!.setOnClickListener(listener)
            return this
        }

        fun icon(@DrawableRes drawableRes: Int): Binder {
            icon!!.setImageResource(drawableRes)
            return this
        }

        fun upEnabled(enabled: Boolean): Binder {
            up!!.setEnabled(enabled)
            return this
        }

        fun downEnabled(enabled: Boolean): Binder {
            down!!.setEnabled(enabled)
            return this
        }

        fun label(@StringRes settingName: Int): Binder {
            if (settingName != 0) {
                val setting: CharSequence = getResources().getString(settingName)
                up!!.setContentDescription(
                    from(getResources(), R.string.setting_incrementor)
                        .put("setting", setting)
                        .format()
                )
                down!!.setContentDescription(
                    from(getResources(), R.string.setting_decrementor)
                        .put("setting", setting)
                        .format()
                )
            } else {
                up!!.setContentDescription(null)
                down!!.setContentDescription(null)
            }
            return this
        }
    }
}
