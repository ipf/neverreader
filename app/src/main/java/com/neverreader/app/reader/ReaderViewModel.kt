package com.neverreader.app.reader

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neverreader.backend.model.Bookmark
import com.neverreader.repository.ArticleRepository
import com.neverreader.repository.BookmarkRepository
import com.neverreader.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val articleRepository: ArticleRepository,
    private val bookmarks: BookmarkRepository,
    private val itemRepository: ItemRepository,
) : ViewModel() {

    private companion object {
        const val TAG = "ReaderViewModel"
    }

    val state: StateFlow<State>
        field = MutableStateFlow<State>(State.Loading(""))

    /**
     * @param id the bookmark to fetch when the caller already knows it. Looking
     *   the article up by URL first meant a single mismatch between the URL the
     *   list holds and the one in the database left the reader with nothing to
     *   fetch and no way to say why.
     */
    fun load(url: String, id: String? = null) {
        state.value = State.Loading(url)
        viewModelScope.launch {
            val bookmark = runCatching {
                id?.let { bookmarks.bookmarkOnce(it) } ?: bookmarks.bookmarkByUrlOnce(url)
            }.getOrNull()
            if (bookmark == null) {
                fail(url, "no saved item for this article")
                return@launch
            }
            // Publish the bookmark before the fetch so the title shows straight
            // away rather than after the round trip.
            state.value = State.Loading(url, bookmark)
            val html = runCatching { articleRepository.getArticleHtml(bookmark.id) }
                .onFailure { fail(url, it.message ?: it::class.java.simpleName, bookmark) }
                .getOrNull()
            if (html.isNullOrBlank()) {
                if (html != null) fail(url, "the server returned an empty article", bookmark)
                return@launch
            }
            state.value = State.Content(url, bookmark, html)
        }
    }

    private fun fail(url: String, reason: String, bookmark: Bookmark? = null) {
        // runCatching{}.getOrNull() swallowed all of this: the reader just said
        // "failed to load" with nothing in logcat to act on.
        Log.e(TAG, "reader could not load $url: $reason")
        state.value = State.Error(url, reason, bookmark)
    }

    fun toggleFavorite(bookmark: Bookmark) {
        viewModelScope.launch { itemRepository.toggleFavorite(bookmark) }
    }

    fun archive(bookmark: Bookmark) {
        viewModelScope.launch { itemRepository.archive(bookmark) }
    }

    fun delete(bookmark: Bookmark) {
        viewModelScope.launch { itemRepository.delete(bookmark) }
    }

    sealed class State {
        abstract val url: String

        /** [bookmark] is filled in as soon as the local record is read, so the
         *  title does not wait on the network. */
        data class Loading(override val url: String, val bookmark: Bookmark? = null) : State()
        data class Content(
            override val url: String,
            val bookmark: Bookmark?,
            val html: String,
        ) : State()

        data class Error(
            override val url: String,
            val reason: String,
            val bookmark: Bookmark?,
        ) : State()
    }
}
