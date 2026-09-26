package com.neverreader.app.settings.account

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import dagger.hilt.android.AndroidEntryPoint
import com.neverreader.app.R
import com.neverreader.app.App
import com.neverreader.app.UserManager
import com.neverreader.app.settings.AbsPrefsFragment
import com.neverreader.app.settings.view.preferences.Preference
import com.neverreader.app.settings.view.preferences.PreferenceViews
import com.neverreader.sdk.util.AbsNeverReaderActivity
import javax.inject.Inject

/** A thin class to allow [AccountManagementFragment] to be launched as a fullscreen activity. */
class AccountManagementActivity : AbsNeverReaderActivity() {

    override val accessType: ActivityAccessRestriction = ActivityAccessRestriction.ANY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(android.R.id.content, AccountManagementFragment())
                .commit()
        }
    }

    companion object {
        fun startActivity(context: Context) {
            context.startActivity(Intent(context, AccountManagementActivity::class.java))
        }
    }
}

/** Account info and logout. */
@AndroidEntryPoint
class AccountManagementFragment : AbsPrefsFragment() {

    private val userManager: UserManager get() = App.from(requireContext()).userManager

    override val title: Int
        get() = R.string.setting_account_management

    override val bannerView: View? = null

    override fun createPrefs(prefs: ArrayList<Preference>?) {
        prefs?.add(
            PreferenceViews.newActionBuilder(this, R.string.settings_logout)
                .setOnClickListener {
                    userManager.logout(activity as? AbsNeverReaderActivity)
                }
                .build(),
        )
    }
}
