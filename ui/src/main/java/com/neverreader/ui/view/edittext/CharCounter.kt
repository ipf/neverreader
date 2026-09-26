package com.neverreader.ui.view.edittext

import android.content.Context
import android.text.Editable
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.TextView
import com.neverreader.ui.R
import com.neverreader.ui.util.NestedColorStateList
import com.neverreader.ui.view.themed.ThemedTextView

class CharCounter : ThemedTextView {
    private val binder: Binder = Binder()

    private var maxLength = 0
    private var watchedText: TextView? = null

    private val watcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            //
        }

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            //
        }

        override fun afterTextChanged(s: Editable?) {
            val length = if (watchedText == null) 0 else watchedText!!.getText().length
            setText(getResources().getString(R.string.quantity_count, length, maxLength))
            setTextColor(
                NestedColorStateList.get(
                    getContext(),
                    if (length == maxLength) R.color.nr_themed_apricot_1 else R.color.nr_themed_grey_3
                )
            )
        }
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init(context)
    }

    constructor(context: Context) : super(context!!) {
        init(context)
    }

    private fun init(context: Context?) {
        setTextAppearance(context, R.style.App_Text_Teeny_Tiny_Light)
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            setText(null)
            watchText(null, 0)
            return this
        }

        fun watchText(tv: TextView?, max: Int): Binder {
            // unbind the last view
            if (watchedText != null) {
                watchedText!!.removeTextChangedListener(watcher)
                watchedText = null
            }
            // bind this one
            if (tv != null) {
                watchedText = tv
                watchedText!!.addTextChangedListener(watcher)
                tv.setFilters(arrayOf<InputFilter>(LengthFilter(max)))
            }
            maxLength = max
            watcher.afterTextChanged(null)
            return this
        }
    }
}
