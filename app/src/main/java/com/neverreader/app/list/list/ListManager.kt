package com.neverreader.app.list.list

import androidx.paging.PagingData
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.BookmarkSort
import com.neverreader.backend.model.ListFilter
import com.neverreader.repository.BookmarkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the current list filter and exposes the paged bookmarks for the list screen.
 */
@Singleton
class ListManager @Inject constructor(
    private val bookmarks: BookmarkRepository,
) {

    enum class Tab(val unread: Boolean?, val favorite: Boolean?) {
        UNREAD(unread = true, favorite = null),
        FAVORITES(unread = true, favorite = true),
        ARCHIVE(unread = false, favorite = null),
    }

    data class SortFilterState(
        val tab: Tab = Tab.UNREAD,
        val tag: String? = null,
        val search: String? = null,
        val sort: BookmarkSort = BookmarkSort.NEWEST,
    )

    private val _sortFilterState = MutableStateFlow(SortFilterState())
    val sortFilterState: StateFlow<SortFilterState> = _sortFilterState

    fun bookmarks(): Flow<PagingData<Bookmark>> = bookmarks.bookmarks(filter())

    private fun filter(): ListFilter {
        val state = _sortFilterState.value
        return ListFilter(
            unread = state.tab.unread,
            favorite = state.tab.favorite,
            tag = state.tag,
            search = state.search,
            sort = state.sort,
        )
    }

    fun setTab(tab: Tab) {
        // Sort deliberately survives a tab change - it is a preference about how
        // to read the list, not a narrowing of it. Search and tag do reset.
        _sortFilterState.update { it.copy(tab = tab, tag = null, search = null) }
    }

    fun setSort(sort: BookmarkSort) {
        _sortFilterState.update { it.copy(sort = sort) }
    }

    fun setSearch(search: String?) {
        _sortFilterState.update { it.copy(search = search, tag = null) }
    }

}
