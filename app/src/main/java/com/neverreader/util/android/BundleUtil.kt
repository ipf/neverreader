package com.neverreader.util.android

import android.os.Bundle
import android.os.Parcelable

/**
 * TODO Documentation
 */
object BundleUtil {
    /**
     * Safely unbundle a custom parcelable class, avoiding class not found errors.
     *
     * @param bundle
     * @param key
     * @param clazz
     * @param <T>
     * @return
    </T> */
    fun <T : Parcelable?> getParcelable(bundle: Bundle?, key: String?, clazz: Class<T?>): T? {
        if (bundle == null || !bundle.containsKey(key)) {
            return null
        }
        val before = bundle.classLoader
        bundle.classLoader = clazz.classLoader
        val value = bundle.getParcelable<T?>(key)
        bundle.classLoader = before
        return value
    }
}
