package com.neverreader.util.android

import com.neverreader.util.java.Logs


/**
 * A set of Dev methods that should never ever ship. These methods are only for debugging in process and should be removed after use.
 * @author max
 */
object WIP {
    // DEV make sure none of these are used.
    /**
     * A quick logging method. Meant to be used in places where logging has no value outside of debugging an immediate issue and will be removed after the issue is finished.
     */
    fun l(log: String?) {
        Logs.i("WIP", log)
    }
}
