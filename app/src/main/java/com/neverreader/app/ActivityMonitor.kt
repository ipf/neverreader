package com.neverreader.app

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import com.neverreader.sdk.util.AbsNeverReaderActivity
import com.neverreader.sdk.util.AbsNeverReaderActivity.SimpleOnLifeCycleChangedListener
import com.neverreader.util.android.ApiLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks the current [AbsNeverReaderActivity]'s that are in play. These are activities that are
 * either starting, resumed or in the process of stopping. This can be used to know where the user might be coming from or
 * going to as NeverReader's internal task stack changes.
 */
@Singleton
class ActivityMonitor @Inject internal constructor() {
    enum class State {
        CREATED,
        RESTARTED,
        STARTED,
        RESUMED,
        PAUSED,
        STOPPED
    }

    private val mActivities = HashMap<State?, Activity?>()
    private val mListeners: MutableSet<Listener> = HashSet<Listener>()

    fun onActivityCreate(activity: AbsNeverReaderActivity) {
        set(activity, State.CREATED)

        activity.addOnLifeCycleChangedListener(object : SimpleOnLifeCycleChangedListener() {
            override fun onActivityCreate(
                savedInstanceState: Bundle?,
                activity: AbsNeverReaderActivity?
            ) {
                // Not expected to happen since onCreate is what invokes the parent addListener
                set(activity, State.CREATED)
            }

            override fun onActivityRestart(activity: AbsNeverReaderActivity?) {
                set(activity, State.RESTARTED)
            }

            override fun onActivityStart(activity: AbsNeverReaderActivity?) {
                set(activity, State.STARTED)
            }

            override fun onActivityResume(activity: AbsNeverReaderActivity?) {
                set(activity, State.RESUMED)
            }

            override fun onActivityPause(activity: AbsNeverReaderActivity?) {
                set(activity, State.PAUSED)
            }

            override fun onActivityStop(activity: AbsNeverReaderActivity?) {
                set(activity, State.STOPPED)
            }

            override fun onActivityDestroy(activity: AbsNeverReaderActivity?) {
                set(activity, null)
            }

            override fun onRequestPermissionsResult(
                requestCode: Int,
                permissions: Array<out String>,
                grantResults: IntArray
            ) {
            }
        })
    }

    /**
     *
     * @param activity
     * @param state The updated state or null to remove the activity from the monitor.
     */
    private fun set(activity: Activity?, state: State?) {
        // Remove all references to this activity from any previous state
        val it: MutableIterator<MutableMap.MutableEntry<State?, Activity?>?> =
            mActivities.entries.iterator()
        while (it.hasNext()) {
            if (it.next()!!.value === activity) {
                it.remove()
            }
        }

        // Attach it to the new state
        if (state != null) {
            mActivities[state] = activity
        }

        // Invoke listeners
        for (listener in mListeners) {
            if (state == null) {
                continue
            }
            when (state) {
                State.STARTED -> listener.onActivityStarted(activity)
                State.RESUMED -> listener.onActivityResumed(activity)
                State.PAUSED -> listener.onActivityPaused(activity)
                else -> {}
            }
        }
    }

    val visible: Activity?
        /**
         * @return An app activity that is visible to the user. In most cases this just means in the resumed or started state,
         * but in a multi-window mode, it could include paused.
         * null if nothing is visible.
         */
        get() {
            if (mActivities.containsKey(State.RESUMED)) {
                return mActivities[State.RESUMED]
            } else if (mActivities.containsKey(State.STARTED)) {
                return mActivities[State.STARTED]
            } else if (mActivities.containsKey(State.PAUSED)) {
                val paused =
                    mActivities[State.PAUSED]
                return if (paused!!.isInMultiWindowMode) {
                    paused
                } else {
                    null
                }
            } else {
                return null
            }
        }

    interface Listener {
        fun onActivityStarted(activity: Activity?)
        fun onActivityResumed(activity: Activity?)
        fun onActivityPaused(activity: Activity?)
    }

}
