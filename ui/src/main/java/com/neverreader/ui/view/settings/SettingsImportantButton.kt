package com.neverreader.ui.view.settings

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import com.neverreader.ui.R
import com.neverreader.ui.analytics.UiEntityable
import com.neverreader.ui.view.themed.ThemedConstraintLayout

/**
 * This View represents a settings button that contains a large divider and red link text, for use
 * as an "important" settings button (e.g. logging out).
 */
class SettingsImportantButton : ThemedConstraintLayout {
    private val binder: Binder = Binder()

    private var text: TextView? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context) : super(context!!) {
        init()
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_settings_important, this, true)
        text = findViewById<TextView>(R.id.text)
        engageable.uiEntityType = UiEntityable.Type.BUTTON
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            text(null)
            return this
        }

        fun text(`val`: CharSequence?): Binder {
            text!!.setText(`val`)
            return this
        }
    }
}
