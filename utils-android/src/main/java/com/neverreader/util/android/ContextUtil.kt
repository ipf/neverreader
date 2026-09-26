package com.neverreader.util.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View

/**s
 * Helper methods related to [Context]
 */
object ContextUtil {
    /**
     * Gets an Activity instance from a view's context using [.getActivity].
     * @param view
     * @return
     */
    fun getActivity(view: View): Activity? {
        return getActivity(view.context)
    }

    /**
     * Tries to cast a Context to an Activity. Can find even within ContextThemeWrapper. If context is null or the context is not an activity, returns null.
     * @param context
     * @return
     */
    fun getActivity(context: Context?): Activity? {
        return getActivityInternal(context)
    }

    fun <T> findContext(view: View, clazz: Class<T>): T? {
        return findContext(view.context, clazz)
    }

    /**
     * Search this context and wrapped contexts for one that matches this class type
     */
    fun <T> findContext(context: Context?, clazz: Class<T>): T? {
        return if (context == null) {
            null
        } else if (clazz.isAssignableFrom(context.javaClass)) {
            context as T
        } else if (context is ContextWrapper) {
            findContext(context.baseContext, clazz)
        } else {
            null
        }
    }

    private fun getActivityInternal(context: Context?): Activity? = findContext(context, Activity::class.java)
}
