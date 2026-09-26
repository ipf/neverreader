package com.neverreader.util.java

import org.apache.commons.lang3.exception.ExceptionUtils

/**
 * A Throwable that can explicitly provide a human friendly, user facing message.
 * [Throwable.getMessage] may be overly technical or not clear whether or not it is intended or safe for users to view.
 */
interface UserFacingErrorMessage {
    val userFacingMessage: String?

    companion object {
        fun find(t: Throwable?): String? {
            val i = ExceptionUtils.indexOfType(t, UserFacingErrorMessage::class.java)
            return if (i >= 0) (ExceptionUtils.getThrowables(t)[i] as UserFacingErrorMessage).userFacingMessage else null
        }
    }
}
