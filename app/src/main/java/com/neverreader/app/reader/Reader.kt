package com.neverreader.app.reader

import com.neverreader.app.reader.queue.InitialQueueType
import com.neverreader.util.prefs.Preferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Reader
@Inject constructor(
    prefs: Preferences,
) {
    private val previousAndNext = prefs.forUser("previousAndNext", true)
    var isPreviousAndNextOn: Boolean
        get() = previousAndNext.get()
        set(value) = previousAndNext.set(value)

    interface Initializer {
        fun onInitialized(
            url: String,
            initialQueueType: InitialQueueType,
            queueStartingIndex: Int,
        )
    }

    interface PreviousNextInteractions {
        fun onPreviousClicked()
        fun onNextClicked()
    }

    interface NavigationInteractions {
        fun onBackstackPopped()
    }

    sealed class NavigationEvent {

        open var url: String = ""
        open var addToBackstack: Boolean = false

        data class GoToArticle(
            override var url: String,
            override var addToBackstack: Boolean,
        ) : NavigationEvent()
        data class  GoToCollection(
            override var url: String,
            override var addToBackstack: Boolean,
        ) : NavigationEvent()
        data class  GoToOriginalWeb(
            override var url: String,
            override var addToBackstack: Boolean,
        ) : NavigationEvent()
    }

    interface NavigationEventHandler {
        fun handleNavigationEvent(event: NavigationEvent)
    }
}