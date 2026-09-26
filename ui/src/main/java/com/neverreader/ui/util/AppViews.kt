package com.neverreader.ui.util

import android.view.View

fun View.updateEnabledAlpha() {
    alpha = if (isEnabled) {
        1f
    } else {
        0.5f
    }
}
