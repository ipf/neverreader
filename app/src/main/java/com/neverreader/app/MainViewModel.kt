package com.neverreader.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neverreader.backend.repo.AccountManager
import com.neverreader.repository.BookmarkRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userManager: UserManager,
    private val bookmarks: BookmarkRepository,
    private val accounts: AccountManager,
) : ViewModel() {

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 1)
    val events: SharedFlow<Event> = _events

    fun onSavesClicked() {
        _events.tryEmit(Event.GoToSaves)
    }

    fun onSettingsClicked() {
        _events.tryEmit(Event.GoToSettings)
    }

    fun onEventCollectionStarted() {
        if (userManager.hadBadCredentials()) {
            viewModelScope.launch { _events.emit(Event.ShowBadCredentialsToast) }
            userManager.onShowedBadCredentialsMessage()
        }
    }

    fun onReaderDeepLinkReceived(url: String) {
        viewModelScope.launch {
            _events.emit(Event.OpenReader(url))
        }
    }

    sealed class Event {
        data object GoToSaves : Event()
        data object GoToSettings : Event()
        data class OpenReader(val url: String) : Event()
        data object ShowBadCredentialsToast : Event()
    }
}
