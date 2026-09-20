package com.neverreader.ui.analytics

import android.view.View

fun interface EngagementListener {
    fun onEngaged(view: View, value: String?)

    companion object {
        val NOT_LISTENING = EngagementListener { _, _ -> }
    }
}

interface Engageable : UiEntityable {
    fun setEngagementListener(listener: EngagementListener?)

    val engagementValue: String?
        get() = null
}

class EngageableHelper : UiEntityableHelper(), Engageable {
    private var engagementListener: EngagementListener = EngagementListener.NOT_LISTENING

    override fun setEngagementListener(listener: EngagementListener?) {
        engagementListener = listener ?: EngagementListener.NOT_LISTENING
    }

    @JvmOverloads fun onEngaged(view: View, value: String? = null) = Unit

    /** Wrap a [View.OnClickListener] to keep the same call shape without tracking. */
    fun getWrappedClickListener(listener: View.OnClickListener?): View.OnClickListener? = listener
}
