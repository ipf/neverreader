package com.neverreader.app.reader

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

    private val _state = MutableStateFlow<State>(State.Loading(""))
    val state: StateFlow<State> get() = _state

    fun load(url: String) {
        _state.value = State.Loading(url)
        viewModelScope.launch {
            val bookmark = runCatching { bookmarks.bookmarkByUrlOnce(url) }.getOrNull()
            val html = runCatching {
                if (bookmark != null) articleRepository.getArticleHtml(bookmark.id) else null
            }.getOrNull()
            _state.value = if (html != null) {
                State.Content(url, bookmark, html)
            } else {
                State.Error(url)
            }
        }
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

        data class Loading(override val url: String) : State()
        data class Content(
            override val url: String,
            val bookmark: Bookmark?,
            val html: String,
        ) : State()

        data class Error(override val url: String) : State()
    }
}
