package com.neverreader.ui.view.settings

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.CompoundButton
import android.widget.TextView
import androidx.core.view.isVisible
import com.neverreader.ui.R
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.EnabledUtil
import com.neverreader.ui.view.menu.ThemedSwitch
import com.neverreader.ui.view.themed.ThemedTextView
import com.neverreader.ui.view.visualmargin.VisualMarginConstraintLayout
import com.neverreader.util.android.setTextOrHide

/**
 * A "settings" styled View for use as an action button or optionally a toggle switch.
 */
class SettingsSwitchView : VisualMarginConstraintLayout {
    private val binder: Binder = Binder()

    private var title: ThemedTextView? = null
    private var subtitle: TextView? = null
    private var toggleSwitch: ThemedSwitch? = null

    constructor(context: Context) : super(context!!) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        LayoutInflater.from(context).inflate(R.layout.view_settings_switch, this, true)
        title = findViewById<ThemedTextView>(R.id.title)
        subtitle = findViewById<TextView>(R.id.subtitle)
        toggleSwitch = findViewById<ThemedSwitch>(R.id.toggleSwitch)
		setOnClickListener { toggleSwitch!!.toggle() }

        setMinimumHeight(dpToPxInt(context, 72f))

        subtitle!!.setVisibility(GONE)

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.SettingsSwitchView)
            title!!.setText(ta.getText(R.styleable.SettingsSwitchView_android_title))
            val sub = ta.getText(R.styleable.SettingsSwitchView_android_text)
            if (sub != null && sub.length > 0) {
                subtitle!!.setText(ta.getText(R.styleable.SettingsSwitchView_android_text))
                subtitle!!.setVisibility(VISIBLE)
            }

            setEnabled(ta.getBoolean(R.styleable.SettingsSwitchView_android_enabled, true))

            binder.isToggle(ta.getBoolean(R.styleable.SettingsSwitchView_isToggle, true))

            ta.recycle()
        } else {
            setLayoutParams(ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT, ConstraintLayout.LayoutParams.WRAP_CONTENT))
            binder.clear()
        }

        setBackground(getResources().getDrawable(R.drawable.cl_nr_touchable_area))

        engageable.uiEntityType = UiEntityable.Type.BUTTON
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        EnabledUtil.setChildrenEnabled(this@SettingsSwitchView, enabled, false)
    }

    override val uiEntityLabel: String?
        get() = title!!.uiEntityLabel

    override val uiEntityValue: String?
        get() = if (toggleSwitch!!.isVisible) toggleSwitch!!.uiEntityValue else null

    override val engagementValue: String?
        get() = if (toggleSwitch!!.isVisible) toggleSwitch!!.engagementValue else null

    var isChecked: Boolean
        get() = toggleSwitch!!.isChecked()
        set(checked) {
            toggleSwitch!!.setChecked(checked)
        }

    fun toggle() {
        toggleSwitch!!.toggle()
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            isToggle(true)
            title(null)
            subtitle(null)
            checked(false)
            enabled(true)
            onCheckedListener(null)
            return this
        }

        fun isToggle(`val`: Boolean): Binder {
            if (!`val`) {
                toggleSwitch!!.setVisibility(INVISIBLE)
            } else {
                toggleSwitch!!.setVisibility(VISIBLE)
            }
            return this
        }

        fun title(`val`: CharSequence?): Binder {
            title!!.setTextAndUpdateEnUsLabel(`val`, if (`val` != null) `val`.toString() else null)
            return this
        }

        fun subtitle(`val`: CharSequence?): Binder {
            subtitle!!.setTextOrHide(`val`)
            return this
        }

        fun checked(`val`: Boolean): Binder {
            this@SettingsSwitchView.isChecked = `val`
            return this
        }

        fun enabled(`val`: Boolean): Binder {
            setEnabled(`val`)
            return this
        }

        fun onCheckedListener(listener: CompoundButton.OnCheckedChangeListener?): Binder {
            toggleSwitch!!.setOnCheckedChangeListener(listener)
            return this
        }

        fun onClickListener(listener: OnClickListener?): Binder {
            setOnClickListener(listener)
            return this
        }
    }
}
