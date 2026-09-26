package com.neverreader.app.settings.rotation.interf

/**
 * A View that displays a "rotation lock" toggle which allows the user
 * to lock the current display orientation within the NeverReader App.
 */
interface RotationLockView {
    fun interface OnClick {
        fun onClick(checked: Boolean)
    }

    fun setOnToggleClick(onclick: OnClick?)

    fun show(checked: Boolean)

    fun hide()
}
