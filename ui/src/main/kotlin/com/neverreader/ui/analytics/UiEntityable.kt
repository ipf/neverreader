package com.neverreader.ui.analytics

import android.content.Context
import android.util.AttributeSet

/** A UI component that a tracker could capture. Kept as a no-op: NeverReader has no analytics. */
interface UiEntityable {
    var uiEntityIdentifier: String?
    val uiEntityType: Type?
    var uiEntityComponentDetail: String?
    val uiEntityLabel: String?
    val uiEntityValue: String?
        get() = null

    enum class Type {
        BUTTON, DIALOG, MENU, CARD, LIST, SCREEN, PAGE, READER;
    }
}

open class UiEntityableHelper : UiEntityable {
    override var uiEntityIdentifier: String? = null
    override var uiEntityType: UiEntityable.Type? = null
    override var uiEntityComponentDetail: String? = null
    override var uiEntityLabel: String? = null

    fun obtainStyledAttributes(context: Context, attrs: AttributeSet?) = Unit

    fun updateEnUsLabel(context: Context, resId: Int) = Unit

    fun updateEnUsLabel(label: String?) {
        uiEntityLabel = label
    }
}
