package com.neverreader.app.settings.view.preferences

import android.view.View
import com.neverreader.app.settings.AbsPrefsFragment
import com.neverreader.app.settings.view.preferences.PreferenceViews.EnabledCondition
import com.neverreader.ui.view.settings.SettingsImportantButton

class ImportantPreference(
    settings: AbsPrefsFragment,
    label: String,
    action: OnClickAction?,
    longPressAction: OnClickAction?,
    condition: EnabledCondition?,
    identifier: String?
) : ActionPreference(settings, label, null, action, longPressAction, condition, identifier) {
    public override fun applyToView(layout: View?) {
        val view = layout as SettingsImportantButton
        view.bind().text(label)
        if (uiEntityIdentifier != null) {
            view.uiEntityIdentifier = uiEntityIdentifier
        }
    }

    public override val type: PrefViewType
        get() {
        return PrefViewType.IMPORTANT
    }
}
