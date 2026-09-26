package com.neverreader.ui.view.item

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import com.neverreader.ui.R
import com.neverreader.ui.util.EnabledUtil.setChildrenEnabled
import com.neverreader.ui.view.themed.ThemedTextView
import com.neverreader.ui.view.visualmargin.VisualMarginConstraintLayout

/**
 * Displays various aspects of an Item's meta data:
 *
 *  * Title
 *  * Domain
 *  * Time Estimate
 *  * Indicator (optional)
 *  * Excerpt (optional, and can control max lines)
 *  * Badges (Groups, Favorite and Tags) (optional)
 *  * Shared By (optional)
 *
 */
class ItemMetaView : VisualMarginConstraintLayout {
    private val binder: Binder = Binder()
    private var title: ThemedTextView? = null
    private var domain: TextView? = null
    private var time: TextView? = null
    private var indicator: ImageView? = null

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context!!, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_item_meta, this, true)
        title = findViewById<ThemedTextView>(R.id.title)
        domain = findViewById<TextView>(R.id.domain)
        time = findViewById<TextView>(R.id.time_estimate)
        indicator = findViewById<ImageView>(R.id.indicator)
        title!!.setEllipsize(TextUtils.TruncateAt.END)

        bind().clear()
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        setChildrenEnabled(this, enabled, true)
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            title(null)
            titleMaxLines(10)
            domain(null)
            timeEstimate(null)
            indicator(null)
            return this
        }

        fun title(value: CharSequence?): Binder {
            title!!.setText(value)
            return this
        }

        fun titleMaxLines(maxLines: Int): Binder {
            title!!.setMaxLines(maxLines)
            return this
        }

        fun domain(value: CharSequence?): Binder {
            domain!!.setText(value)
            return this
        }

        fun timeEstimate(value: CharSequence?): Binder {
            if (TextUtils.isEmpty(value)) {
                time!!.setText(null)
                time!!.setVisibility(GONE)
            } else {
                time!!.setText(TextUtils.concat(" · ", value))
                time!!.setVisibility(VISIBLE)
            }
            return this
        }

        fun indicator(drawable: Drawable?): Binder {
            indicator!!.setImageDrawable(drawable)
            indicator!!.setVisibility(if (drawable != null) VISIBLE else GONE)
            return this
        }
    }
}
