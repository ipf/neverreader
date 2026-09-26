package com.neverreader.ui.view.edittext

import android.content.Context
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.ThemedLinearLayout

class CharCountEditText : ThemedLinearLayout {
    private val binder: Binder = Binder()

    private var editText: EditText? = null
    private var charCount: CharCounter? = null

    constructor(context: Context, attrs: AttributeSet?) : super(context!!, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context)
    }

    constructor(context: Context) : super(context!!) {
        init(context)
    }

    private fun init(context: Context?) {
        inflate(context, R.layout.view_char_count_edittext, this)
        setOrientation(VERTICAL)
        editText = findViewById<EditText>(R.id.edit_text)
        charCount = findViewById<CharCounter>(R.id.char_count)
    }

    fun showKeyboard() {
        requestFocus()
        (getContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
            editText,
            InputMethodManager.SHOW_IMPLICIT
        )
    }

    fun hideKeyboard() {
        (getContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).hideSoftInputFromWindow(
            editText!!.getWindowToken(),
            0
        )
    }

    fun getEditText(): EditText {
        return editText!!
    }

    fun bind(): Binder {
        return binder
    }

    inner class Binder {
        private var watcher: TextWatcher? = null

        fun clear(): Binder {
            editText!!.setText(null)
            charCount!!.bind().clear()
            textChanged(null)
            return this
        }

        fun maxLength(length: Int): Binder {
            charCount!!.bind().watchText(editText, length)
            return this
        }

        fun textChanged(w: TextWatcher?): Binder {
            editText!!.removeTextChangedListener(watcher)
            watcher = w
            editText!!.addTextChangedListener(watcher)
            return this
        }
    }
}
