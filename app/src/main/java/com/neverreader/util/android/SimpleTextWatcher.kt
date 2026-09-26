package com.neverreader.util.android

import android.text.Editable
import android.text.TextWatcher

/**
 * A no op implementation so you can override only the methods you will use. No need to call super methods as they do nothing (unless your class is a sub-sub-class).
 */
abstract class SimpleTextWatcher : TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {}
}
