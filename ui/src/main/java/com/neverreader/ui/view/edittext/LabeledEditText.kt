package com.neverreader.ui.view.edittext

import android.content.Context
import android.graphics.Typeface
import android.text.InputType
import android.util.AttributeSet
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.ViewCompat
import com.neverreader.ui.R
import com.neverreader.ui.text.Fonts
import com.neverreader.ui.text.Fonts.get
import com.neverreader.ui.text.TextViewUtil
import com.neverreader.ui.util.DimenUtil.dpToPxInt
import com.neverreader.ui.util.NestedColorStateList
import com.neverreader.ui.view.themed.ThemedEditText
import com.neverreader.ui.view.themed.ThemedTextInputLayout
import com.neverreader.ui.view.visualmargin.VisualMargin
import kotlin.math.ceil

class LabeledEditText : ThemedTextInputLayout, VisualMargin {
    private val binder: Binder = Binder()

    private var line: View? = null
    private var editText: EditText? = null

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
        setHintTextAppearance(R.style.App_TextFloatingLabelAppearance)

        editText =
            ThemedEditText(ContextThemeWrapper(context, R.style.App_EditTextAppearance), attrs)

        // The editText inherits the id of its parent LabeledEditText via passing attrs in its creation, which can cause duplicate id crashes.
        // Here we set it back to its default id-less state.
        editText!!.setId(NO_ID)

        addView(editText)

        // set the typeface of the floating label
        setTypeface(get(context, Fonts.Font.GRAPHIK_LCG_MEDIUM))

        // add our own horizontal line
        setOrientation(VERTICAL)
        line = View(context)
        line!!.setBackgroundColor(getResources().getColor(R.color.nr_themed_grey_5))
        ViewCompat.setBackgroundTintList(
            line!!,
            NestedColorStateList.get(context, R.color.nr_edittext_underline)
        )
        line!!.setLayoutParams(LayoutParams(LayoutParams.MATCH_PARENT, dpToPxInt(context, 1f)))
        addView(line)

        if (attrs != null) {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.LabeledEditText)

            setHintLabel(ta.getText(R.styleable.LabeledEditText_android_hint))

            val inputType = ta.getInt(
                R.styleable.LabeledEditText_android_inputType,
                EditorInfo.TYPE_TEXT_VARIATION_NORMAL
            )
            editText!!.setInputType(inputType)
            // fix inputType:password changing font to monospace
            if (inputType == (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)) {
                editText!!.setTypeface(Typeface.DEFAULT)
            }
            binder.underline(ta.getBoolean(R.styleable.LabeledEditText_underLine, true))

            editText!!.setCompoundDrawablesWithIntrinsicBounds(
                null,
                null,
                ta.getDrawable(R.styleable.LabeledEditText_android_drawableRight),
                null
            )
            ta.recycle()
        }
    }

    private fun setHintLabel(hint: CharSequence?) {
        setHint(hint)
        editText!!.setHint(null) // remove the hint that gets propagated to the EditText
    }

    private fun setErrorColors(error: Boolean) {
        line!!.setActivated(error)
        editText!!.setActivated(error)
        invalidate()
    }

    val isErrorState: Boolean
        get() = editText!!.isActivated()

    override fun setOnFocusChangeListener(l: OnFocusChangeListener?) {
        // Respond to the focus change of the internal edit text, and when returning the view, return this view not, the internal edit text.
        editText!!.setOnFocusChangeListener(if (l != null) OnFocusChangeListener { v: View?, hasFocus: Boolean ->
            l.onFocusChange(
                this@LabeledEditText,
                hasFocus
            )
        } else null)
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        fun clear(): Binder {
            label(null)
            text(null)
            underline(true)
            setErrorColors(false)
            return this
        }

        fun label(label: CharSequence?): Binder {
            setHintLabel(label)
            return this
        }

        fun text(text: CharSequence?): Binder {
            editText!!.setText(text)
            return this
        }

        fun underline(show: Boolean): Binder {
            line!!.setVisibility(if (show) VISIBLE else GONE)
            return this
        }

        fun errorState(error: Boolean): Binder {
            setErrorColors(error)
            return this
        }
    }

    override fun visualAscent(): Int {
        return ceil(TextViewUtil.ascent(editText!!).toDouble()).toInt()
    }

    override fun visualDescent(): Int {
        return 0
    }

    override fun prepareVisualAscent(): Boolean {
        return false
    }

    override fun prepareVisualDescent(): Boolean {
        return false
    }
}
