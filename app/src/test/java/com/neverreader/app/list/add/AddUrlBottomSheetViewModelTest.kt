package com.neverreader.app.list.add

import app.cash.turbine.test
import com.neverreader.analytics.FakeTracker
import com.neverreader.analytics.appevents.SavesEvents
import com.neverreader.analytics.assertTracked
import com.neverreader.repository.FakeItemRepository
import com.neverreader.repository.FakeUserRepository
import com.neverreader.test.MainDispatcherRule
import com.neverreader.usecase.Save
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail

class AddUrlBottomSheetViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    val itemRepository = FakeItemRepository()
    val tracker = FakeTracker()
    val subject = AddUrlBottomSheetViewModel(
        Save(itemRepository, FakeUserRepository()),
        tracker,
    )

    @Test
    fun `starts with empty text field`() {
        assertEquals("", subject.textFieldValue)
    }

    @Test
    fun `starts without showing error`() {
        assertFalse(subject.textFieldIsError)
    }

    @Test
    fun `tracks impression`() {
        subject.onViewShown()

        tracker.assertTracked(SavesEvents.addUrlBottomSheetShown())
    }

    @Test
    fun `when trying to save an invalid url then shows error`() {
        subject.onTextFieldValueChange("this is not a valid URL")
        subject.onSaveButtonClick()

        assertTrue(subject.textFieldIsError)
    }

    @Test
    fun `when trying to save an invalid url then sends an analytics event`() {
        subject.onTextFieldValueChange("this is not a valid URL")
        subject.onSaveButtonClick()

        tracker.assertTracked(SavesEvents.addUrlBottomSheetSaveFailed())
    }
}
