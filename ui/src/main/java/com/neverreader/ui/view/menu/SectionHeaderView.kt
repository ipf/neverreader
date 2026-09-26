package com.neverreader.ui.view.menu

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedConstraintLayout
import com.neverreader.util.android.setTextOrHide

/**
 * The default style of a section header. Includes a bottom divider by default.
 *
 *
 * You can control dividers with xml attrs or methods:
 *
 *
 * `app:showDividerTop` or `bind().showTopDivider()` turns on and off a thick top divider
 *
 *
 * `app:showDividerBottom` or `bind().showBottomDivider()` turns on and off a thin bottom divider
 *
 *
 * Supports the standard android:text attribute for setting the header text via xml
 * TODO consider enforcing upper case
 */
class SectionHeaderView : ThemedConstraintLayout {
    private val binder: Binder = Binder()
    private var label: TextView? = null
    private var button: TextView? = null
    private var top: View? = null
    private var bottom: View? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init(attrs)
    }

    constructor(context: Context) : super(context!!) {
        init(null)
    }

    private fun init(attrs: AttributeSet?) {
        LayoutInflater.from(getContext()).inflate(R.layout.view_section_header, this, true)
        label = findViewById<TextView>(R.id.label)
        button = findViewById<TextView>(R.id.button)
        top = findViewById<View>(R.id.top_divider)
        bottom = findViewById<View>(R.id.bottom_divider)

        bind().clear()

        if (attrs != null) {
            val a = getContext().obtainStyledAttributes(attrs, R.styleable.SectionHeaderView)
            bind().showTopDivider(a.getBoolean(R.styleable.SectionHeaderView_showDividerTop, false))
            bind().showBottomDivider(
                a.getBoolean(
                    R.styleable.SectionHeaderView_showDividerBottom,
                    true
                )
            )
            bind().label(a.getText(R.styleable.SectionHeaderView_android_text))
            a.recycle()
        }
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            label(null)
            showTopDivider(false)
            showBottomDivider(true)
            button(null, null)
            textAllCaps(false)
            return this
        }

        fun label(value: CharSequence?): Binder {
            label!!.setText(value)
            return this
        }

        fun label(stringResId: Int): Binder {
            label!!.setText(stringResId)
            return this
        }

        fun showTopDivider(show: Boolean): Binder {
            top!!.setVisibility(if (show) VISIBLE else GONE)
            return this
        }

        fun showBottomDivider(show: Boolean): Binder {
            bottom!!.setVisibility(if (show) VISIBLE else GONE)
            return this
        }

        fun button(text: Int, onClick: OnClickListener?): Binder {
            return button(button!!.getResources().getText(text), onClick)
        }

        fun button(text: CharSequence?, onClick: OnClickListener?): Binder {
            button!!.setTextOrHide(text)
            button!!.setOnClickListener(onClick)
            return this
        }

        fun textAllCaps(caps: Boolean): Binder {
            label!!.setAllCaps(caps)
            return this
        }
    }
}
