package com.neverreader.app

/**
 * A type of build mode that app is running in.
 */
enum class AppMode {
    DEV,
    TEAM_ALPHA,
    PRODUCTION;

    val isForInternalCompanyOnly: Boolean
        /**
         * @return true if this is a dev or internal beta build. **PLEASE** read the security note in [AppMode]'s docs.
         */
        get() = this == AppMode.DEV || this == AppMode.TEAM_ALPHA

    val isDevBuild: Boolean
        get() = this == AppMode.DEV

    val isPublic: Boolean
        get() = this == AppMode.PRODUCTION
}
