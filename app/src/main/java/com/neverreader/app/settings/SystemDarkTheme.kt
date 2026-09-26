package com.neverreader.app.settings

import android.content.Context
import android.content.res.Configuration
import android.view.View
import com.neverreader.app.AppLifecycle
import com.neverreader.app.AppLifecycle.LogoutPolicy
import com.neverreader.app.AppLifecycleEventDispatcher
import com.neverreader.util.android.ApiLevel
import com.neverreader.util.prefs.BooleanPreference
import com.neverreader.util.prefs.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A new app setting to take advantage of a new system setting in Android Q. Users can now toggle between
 * light and dark theme for their whole device and if they like, the app can always use the same theme as the system.
 */
@Singleton
class SystemDarkTheme @Inject constructor(
    prefs: Preferences,
    theme: Theme,
    @ApplicationContext context: Context,
    dispatcher: AppLifecycleEventDispatcher
) : AppLifecycle {
    private val context: Context
    val preference: BooleanPreference
    private val theme: Theme

    init {
        dispatcher.registerAppLifecycleObserver<SystemDarkTheme>(this)
        this.theme = theme
        this.preference = prefs.forUser("appThemeSystem", ApiLevel.hasSystemDarkTheme())
        this.context = context
        updateTheme(getCurrentConfiguration(context))
    }

    private val isEnabled: Boolean
        get() = ApiLevel.hasSystemDarkTheme()

    val isOn: Boolean
        get() = this.isEnabled && preference.get()

    fun turnOn(view: View) {
        if (!preference.get()) {
            preference.set(true)
            updateTheme(getCurrentConfiguration(view.context))
        }
    }

    fun turnOn(context: Context) {
        if (!preference.get()) {
            preference.set(true)
            updateTheme(getCurrentConfiguration(context))
        }
    }

    fun turnOff(view: View) {
        if (preference.get()) {
            preference.set(false)
            updateTheme(getCurrentConfiguration(view.context))
        }
    }

    fun turnOff(context: Context) {
        if (preference.get()) {
            preference.set(false)
            updateTheme(getCurrentConfiguration(context))
        }
    }

    override fun onLoggedIn(isNewUser: Boolean) {
        updateTheme(getCurrentConfiguration(context))
    }

    override fun onConfigurationChanged(configuration: Configuration?) {
        updateTheme(configuration)
    }

    override fun onLogoutStarted(): LogoutPolicy {
        return object : LogoutPolicy {
            override fun stopModifyingUserData() {}
            override fun deleteUserData() {}
            override fun restart() {}
            override fun onLoggedOut() {
                // switch users back to follow system theme on log out
                updateTheme(getCurrentConfiguration(context))
            }
        }
    }

    private fun getCurrentConfiguration(context: Context): Configuration? {
        return context.getResources().getConfiguration()
    }

    private fun updateTheme(configuration: Configuration?) {
        if (!preference.isSet) {
            // Pick a default for this
            val value: Boolean
            if (!ApiLevel.hasSystemDarkTheme()) {
                value = false
            } else {
                val current = theme.get()
                if (current == Theme.Companion.LIGHT) {
                    // If they are using the default, let's opt them in,
                    // in case they discover the system setting, but not ours.
                    value = true
                } else if (current == Theme.Companion.DARK && Companion.isSystemSetToDark(
                        getCurrentConfiguration(context)!!
                    )
                ) {
                    // If they were using dark theme, but also enabled system dark theme
                    // then let's opt them in so the themes stay in sync.
                    value = true
                } else {
                    // If other
                    value = false
                }
            }
            preference.set(value)
        }

        if (this.isOn) {
            val currentTheme = theme.get()
            if (isSystemSetToLight(configuration) && currentTheme != Theme.Companion.LIGHT) {
                theme.set(Theme.Companion.LIGHT)
            } else if (isSystemSetToDark(configuration) && currentTheme != Theme.Companion.DARK) {
                theme.set(Theme.Companion.DARK)
            }
        }
    }

    companion object {
        private fun isSystemSetToLight(configuration: Configuration?): Boolean {
            return !isSystemSetToDark(configuration)
        }

        private fun isSystemSetToDark(configuration: Configuration?): Boolean {
            val nightMode = (configuration?.uiMode ?: 0) and Configuration.UI_MODE_NIGHT_MASK
            return nightMode == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
