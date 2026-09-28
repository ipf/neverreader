package com.neverreader.app.list

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.neverreader.app.list.list.ListManager
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.BookmarkSort
import com.neverreader.backend.repo.AccountManager
import com.neverreader.sdk.preferences.AppPrefs
import com.neverreader.repository.BookmarkRepository
import com.neverreader.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MyListViewModel @Inject constructor(
    val listManager: ListManager,
    private val itemRepository: ItemRepository,
    private val bookmarks: BookmarkRepository,
    private val accountManager: AccountManager,
    private val appPrefs: AppPrefs,
) : ViewModel() {

    val navigationEvents: SharedFlow<MyListNavigationEvent>
        field = MutableSharedFlow<MyListNavigationEvent>(extraBufferCapacity = 1)

    /**
     * Whether thumbnails served by a third party may be loaded. Flipping this
     * re-maps the list, so it is combined into the paging stream rather than
     * read once.
     */
    private val allowThirdPartyImages: Flow<Boolean> =
        appPrefs.LOAD_THIRD_PARTY_IMAGES.withChanges
            .map { it == true }
            .distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    val pagedBookmarks: Flow<PagingData<ListItemUiState>> =
        combine(listManager.sortFilterState, allowThirdPartyImages) { _, allow -> allow }
            .flatMapLatest { allow ->
                listManager.bookmarks().map { paging -> paging.map { it.toUiState(allow) } }
            }
            .cachedIn(viewModelScope)

    val sortFilterState: StateFlow<ListManager.SortFilterState> = listManager.sortFilterState

    private val searchInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            // Every keystroke otherwise tears down the Pager and re-runs the query.
            // A blank query clears immediately so closing search is instant.
            searchInput
                .debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
                .distinctUntilChanged()
                .collect { listManager.setSearch(it.ifBlank { null }) }
        }
    }

    /** The text as typed, so the field stays responsive while the query lags. */
    val searchText: StateFlow<String> = searchInput

    fun onSearchChange(query: String) {
        searchInput.value = query
    }

    fun clearSearch() {
        searchInput.value = ""
    }

    fun setTab(tab: ListManager.Tab) {
        listManager.setTab(tab)
    }

    fun setSort(sort: BookmarkSort) {
        listManager.setSort(sort)
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
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
        navigationEvents.tryEmit(MyListNavigationEvent.OpenReader(bookmark.id, bookmark.url))
    }

    private fun Bookmark.toUiState(allowThirdPartyImages: Boolean) = ListItemUiState(
        bookmark = this,
        title = title.ifBlank { domain ?: url },
        domain = domain,
        excerpt = excerpt,
        // A thumbnail on our own server is fine; one on an article's CDN is not,
        // unless the user opted in.
        imageUrl = imageUrl?.takeIf { allowThirdPartyImages || isServedByOurServer(it) },
        favorite = favorite,
        unread = unread,
        meta = readingTimeLabel(readingTimeMinutes),
        savedDate = savedDateLabel(createdAt),
    )

    private fun isServedByOurServer(imageUrl: String): Boolean {
        val serverHost = accountManager.activeCached?.serverUrl?.let { hostOf(it) } ?: return false
        return hostOf(imageUrl)?.equals(serverHost, ignoreCase = true) == true
    }

    /** The host shown under the title, or null when the url has no usable host. */
    private val Bookmark.domain: String?
        get() = displayHost(url)
}

internal const val MAX_READING_TIME_LABEL = 99

/**
 * Backends report a reading time in whole minutes. Anything missing or zero is
 * dropped rather than rendered as a literal "0 min read".
 */
/**
 * When the item was saved, for the bottom-left of the row.
 *
 * This is the save date, not a publication date: neither backend records when an
 * article was published. Null when unset, so a row never reads "1 Jan 1970".
 */
internal fun savedDateLabel(
    epochMillis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String? {
    if (epochMillis <= 0L) return null
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))
}

internal fun readingTimeLabel(minutes: Int?): String? = when {
    minutes == null || minutes <= 0 -> null
    minutes > MAX_READING_TIME_LABEL -> "60+ min read"
    else -> "$minutes min read"
}

/** Strips the scheme and a leading `www.` so the row shows `example.com`. */
internal fun displayHost(url: String): String? =
    runCatching { Uri.parse(url).host }.getOrNull()?.removePrefix("www.")?.takeIf { it.isNotBlank() }

/**
 * The bare host, without any www prefix, for comparing two origins.
 *
 * java.net.URI rather than android.net.Uri: this decides whether a request is
 * authorised, and Uri is deliberately lenient in ways that make it a poor thing
 * to base an origin check on. It is also pure JVM, so the rule is testable
 * without an Android runtime.
 */
internal fun hostOf(url: String): String? =
    runCatching { java.net.URI(url).host }
        .getOrNull()
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }

data class ListItemUiState(
    val bookmark: Bookmark,
    val title: String,
    val domain: String?,
    val excerpt: String,
    val imageUrl: String?,
    val favorite: Boolean,
    val unread: Boolean,
    val meta: String?,
    val savedDate: String?,
)

sealed class MyListNavigationEvent {
    data object ShowAddUrl : MyListNavigationEvent()
    data class OpenReader(val id: String, val url: String) : MyListNavigationEvent()
}
