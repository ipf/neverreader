package com.neverreader.util.android

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View

/** Helper methods related to [Context]. */
object ContextUtil {

    /**
     * The [Activity] behind this view's context, or null if there is not one.
     *
     * @return
     */
    fun getActivity(view: View): Activity? = getActivity(view.context)

    /**
     * The [Activity] behind this context, or null if the context is not one.
     * Looks through [ContextWrapper]s, so a themed or Compose-hosted context
     * still resolves.
     *
     * @param context
     * @return
     */
    fun getActivity(context: Context?): Activity? = findContext(context)

    /**
     * The first context in this one, or in the contexts it wraps, that is a [T].
     * Null if there is none.
     *
     * Reified so the test is a real type check at the call site. Taking a
     * [Class] instead would have meant casting the result back to T unchecked,
     * so nothing checked that the two agreed and a mismatch became a
     * ClassCastException at runtime instead of a compiler error.
     */
    inline fun <reified T : Context> findContext(context: Context?): T? {
        var current = context
        while (current != null) {
            if (current is T) return current
            current = (current as? ContextWrapper)?.baseContext
        }
        return null
    }
}
