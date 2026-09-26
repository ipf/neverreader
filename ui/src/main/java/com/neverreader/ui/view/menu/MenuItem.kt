package com.neverreader.ui.view.menu

import android.view.View
import androidx.annotation.StringRes

class MenuItem @JvmOverloads constructor(
    @field:StringRes val label: Int,
    val icon: Int,
    val onClick: View.OnClickListener?,
    val uiEntityIdentifier: String? = null
) {
    val groupId: Int = 1

    /**
     * Invoked each time, right before the menu is displayed.
     *
     * @return whether or not this option should be visible in the menu
     */
    var isVisible: Boolean = true

    /**
     * Invoked each time, right before the menu is displayed.
     *
     * @return whether or not this option should be enabled in the menu
     */
    var isEnabled: Boolean = true

    fun onClick(v: View?) {
        if (onClick != null) {
            onClick.onClick(v)
        }
    }
}
