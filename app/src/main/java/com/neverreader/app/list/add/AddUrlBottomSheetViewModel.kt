package com.neverreader.app.list.add

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neverreader.analytics.Tracker
import com.neverreader.analytics.appevents.SavesEvents
import com.neverreader.usecase.Save
import com.neverreader.util.java.UrlFinder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddUrlBottomSheetViewModel
@Inject constructor(
    private val save: Save,
    private val tracker: Tracker,
) : ViewModel() {

    private val _navigationEvents = MutableSharedFlow<NavigationEvent>()
    val navigationEvents: SharedFlow<NavigationEvent> get() = _navigationEvents

    fun onViewShown() {
        tracker.track(SavesEvents.addUrlBottomSheetShown())
    }

    var textFieldValue by mutableStateOf("")
        private set

    fun onTextFieldValueChange(value: String) {
        textFieldValue = value
        textFieldIsError = false
    }

    var textFieldIsError by mutableStateOf(false)

    fun onSaveButtonClick() {
        val url = UrlFinder.getFirstUrlOrNull(textFieldValue)
        if (url != null) {
            viewModelScope.launch {
                when (save(url)) {
                    Save.Result.Success -> {
                        tracker.track(SavesEvents.addUrlBottomSheetSaveSucceeded())
                        _navigationEvents.emit(NavigationEvent.Close)
                    }
                    Save.Result.NotLoggedIn -> {
                        // We require logging in before we show this view.
                    }
                }
            }
        } else {
            tracker.track(SavesEvents.addUrlBottomSheetSaveFailed())
            textFieldIsError = true
        }
    }

    sealed class NavigationEvent {
        data object Close : NavigationEvent()
    }
}
