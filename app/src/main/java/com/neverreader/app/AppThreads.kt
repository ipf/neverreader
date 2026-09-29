package com.neverreader.app

import android.os.Handler
import android.os.Looper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The app's main-thread [Handler], shared so posting to it does not allocate a
 * new one per call site.
 *
 * There used to be a general-purpose task pool here, with priority ordering, a
 * wake lock held while tasks were pending, and a logout policy that terminated
 * the pool. The only two things that survived its removal are this handler and
 * [AddActivity]'s timeout; no task ever ran through the pools, so the wake
 * locks, the [LogoutPolicy] and two manifest services bought nothing but
 * background-execution surface. Work that needs a background thread now uses
 * a coroutine dispatcher.
 */
@Singleton
class AppThreads @Inject constructor() {
    val handler: Handler = Handler(Looper.getMainLooper())
}
