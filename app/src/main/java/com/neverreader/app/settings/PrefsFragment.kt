package com.neverreader.app.settings

import dagger.hilt.android.AndroidEntryPoint
import android.view.View
import com.neverreader.app.App
import com.neverreader.app.R
import com.neverreader.app.UserManager
import com.neverreader.app.settings.account.AccountManagementActivity
import com.neverreader.app.settings.view.preferences.HeaderPreference
import com.neverreader.app.settings.view.preferences.Preference
import com.neverreader.app.settings.view.preferences.PreferenceViews
import com.neverreader.app.R.string
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderFragment
import javax.inject.Inject

/**
 * The settings screen: account and open-source licenses.
 */
@AndroidEntryPoint
class PrefsFragment : AbsPrefsFragment() {

    private val userManager: UserManager get() = App.from(requireContext()).userManager

    override val title: Int
        get() = R.string.settings_title

    override val bannerView: View? = null

    override fun createPrefs(prefs: ArrayList<Preference>?) {
        prefs?.add(HeaderPreference(this, getString(R.string.settings_section_account), false))
        prefs?.add(
            PreferenceViews.newActionBuilder(this, R.string.settings_account)
                .setOnClickListener {
                    AccountManagementActivity.startActivity(requireContext())
                }
                .build(),
        )
        prefs?.add(
            PreferenceViews.newActionBuilder(this, R.string.settings_logout)
                .setOnClickListener {
                    userManager.logout(activity as AbsNeverReaderActivity)
                }
                .build(),
        )

        prefs?.add(HeaderPreference(this, getString(R.string.settings_section_about), false))
        prefs?.add(
            PreferenceViews.newActionBuilder(this, R.string.settings_open_source_licenses)
                .setOnClickListener {
                    OpenSourceLicensesActivity.startActivity(requireContext())
                }
                .build(),
        )
    }
}
