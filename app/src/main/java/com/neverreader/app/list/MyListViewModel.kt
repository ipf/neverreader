package com.neverreader.app.list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.neverreader.app.list.list.ListManager
import com.neverreader.backend.model.Bookmark
import com.neverreader.repository.BookmarkRepository
import com.neverreader.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyListViewModel @Inject constructor(
    val listManager: ListManager,
    private val itemRepository: ItemRepository,
    private val bookmarks: BookmarkRepository,
) : ViewModel() {

    val navigationEvents: SharedFlow<MyListNavigationEvent>
        field = MutableSharedFlow<MyListNavigationEvent>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedBookmarks: Flow<PagingData<ListItemUiState>> =
        listManager.sortFilterState
            .flatMapLatest { listManager.bookmarks() }
            .map { paging -> paging.map { it.toUiState() } }
            .cachedIn(viewModelScope)

    val sortFilterState: StateFlow<ListManager.SortFilterState> = listManager.sortFilterState

    fun setTab(tab: ListManager.Tab) {
        listManager.setTab(tab)
    }

    fun archive(bookmark: Bookmark) {
        viewModelScope.launch { itemRepository.archive(bookmark) }
    }

    fun unArchive(bookmark: Bookmark) {
        viewModelScope.launch { itemRepository.unArchive(bookmark) }
    }

    fun toggleFavorite(bookmark: Bookmark) {
        viewModelScope.launch { itemRepository.toggleFavorite(bookmark) }
    }

    fun delete(bookmark: Bookmark) {
        viewModelScope.launch { itemRepository.delete(bookmark) }
    }

    fun onItemClicked(bookmark: Bookmark) {
        navigationEvents.tryEmit(MyListNavigationEvent.OpenReader(bookmark.url))
    }

    private fun Bookmark.toUiState() = ListItemUiState(
        bookmark = this,
        title = title.ifBlank { domain ?: url },
        domain = domain,
        excerpt = excerpt,
        imageUrl = imageUrl,
        favorite = favorite,
        unread = unread,
        meta = readingTimeLabel(readingTimeMinutes),
    )

    /** The host shown under the title, or null when the url has no usable host. */
    private val Bookmark.domain: String?
        get() = displayHost(url)
}

internal const val MAX_READING_TIME_LABEL = 99

/**
 * Backends report a reading time in whole minutes. Anything missing or zero is
 * dropped rather than rendered as a literal "0 min read".
 */
internal fun readingTimeLabel(minutes: Int?): String? = when {
    minutes == null || minutes <= 0 -> null
    minutes > MAX_READING_TIME_LABEL -> "60+ min read"
    else -> "$minutes min read"
}

/** Strips the scheme and a leading `www.` so the row shows `example.com`. */
internal fun displayHost(url: String): String? =
    runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() }

data class ListItemUiState(
    val bookmark: Bookmark,
    val title: String,
    val domain: String?,
    val excerpt: String,
    val imageUrl: String?,
    val favorite: Boolean,
    val unread: Boolean,
    val meta: String?,
)

sealed class MyListNavigationEvent {
    data object ShowAddUrl : MyListNavigationEvent()
    data class OpenReader(val url: String) : MyListNavigationEvent()
}
