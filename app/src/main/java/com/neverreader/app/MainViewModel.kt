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


    fun onEventCollectionStarted() {
        if (userManager.hadBadCredentials()) {
            viewModelScope.launch { _events.emit(Event.ShowBadCredentialsToast) }
            userManager.onShowedBadCredentialsMessage()
        }
    }

    sealed class Event {
        data object ShowBadCredentialsToast : Event()
    }
}
