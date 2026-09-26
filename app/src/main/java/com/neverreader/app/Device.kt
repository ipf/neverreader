package com.neverreader.app

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.neverreader.util.android.ApiLevel
import com.neverreader.util.java.Safe
import com.neverreader.util.java.Safe.Get
import com.neverreader.util.prefs.Preferences
import com.neverreader.util.prefs.StringPreference
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for getting device info and/or overriding the device info we send to various endpoints to test certain features in team and dev builds.
 */
@Singleton
class Device(
    prefs: Preferences,
    mode: AppMode,
    private val defaultManuf: String?,
    private val defaultModel: String?,
    private val defaultProduct: String?,
    private val defaultAnid: String?,
    private val defaultSid: String?
) {
    private val overridesEnabled: Boolean = mode.isForInternalCompanyOnly
    private val overrideManuf: StringPreference
    private val overrideModel: StringPreference
    private val overrideProduct: StringPreference
    private val overrideAnid: StringPreference
    private val overrideSid: StringPreference

    /**
     * Creates defaults based on the running Android environment
     */
    @Inject
    constructor(@ApplicationContext context: Context, prefs: Preferences, mode: AppMode) : this(
        prefs,
        mode,
        Build.MANUFACTURER,
        Build.MODEL,
        Build.PRODUCT,
        Safe.get<String?>(Get {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        }),
        Safe.get<String?>(Get { if (true) Build.SERIAL else null })
    )

    /**
     * Manually supply defaults.
     */
    init {

        val group = prefs.group("dcfig_device")!!
        this.overrideManuf = group.forApp("device_manuf", null as String?)
        this.overrideModel = group.forApp("device_model", null as String?)
        this.overrideProduct = group.forApp("device_product", null as String?)
        this.overrideAnid = group.forApp("device_anid", null as String?)
        this.overrideSid = group.forApp("device_sid", null as String?)
    }

    fun manufacturer(): String? {
        return get(defaultManuf, overrideManuf)
    }

    fun model(): String? {
        return get(defaultModel, overrideModel)
    }

    fun product(): String? {
        return get(defaultProduct, overrideProduct)
    }

    fun anid(): String? {
        return get(defaultAnid, overrideAnid)
    }

    fun sid(): String? {
        return get(defaultSid, overrideSid)
    }

    private fun get(defaultValue: String?, overridePref: StringPreference): String? {
        return if (overridesEnabled) StringUtils.defaultIfEmpty<String?>(
            overridePref.get(),
            defaultValue
        ) else defaultValue
    }

    fun setOverrideManuf(value: String?) {
        if (overridesEnabled) overrideManuf.set(value)
    }

    fun setOverrideModel(value: String?) {
        if (overridesEnabled) overrideModel.set(value)
    }

    fun setOverrideProduct(value: String?) {
        if (overridesEnabled) overrideProduct.set(value)
    }

    fun setOverrideAnid(value: String?) {
        if (overridesEnabled) overrideAnid.set(value)
    }

    fun setOverrideSid(value: String?) {
        if (overridesEnabled) overrideSid.set(value)
    }
}
