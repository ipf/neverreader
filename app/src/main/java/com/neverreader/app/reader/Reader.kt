package com.neverreader.app.reader

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Reader @Inject constructor() {

    sealed class NavigationEvent {
        data class Open(val url: String) : NavigationEvent()
    }

    interface NavigationEventHandler {
        fun handleNavigationEvent(event: NavigationEvent)
    }
}
