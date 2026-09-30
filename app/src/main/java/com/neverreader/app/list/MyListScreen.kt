package com.neverreader.app.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.core.os.bundleOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.neverreader.app.R
import com.neverreader.app.repository.ThumbnailRepository
import com.neverreader.app.list.list.ListManager
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.model.BookmarkSort
import com.neverreader.backend.sync.SyncWorker
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.compose.FilterChips
import com.neverreader.ui.compose.SearchField
import com.neverreader.ui.compose.MenuChip
import com.neverreader.ui.compose.ItemRow
import com.neverreader.ui.theme.AppTheme
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.UpIcon
import kotlinx.coroutines.launch

/** Kept out of :ui, which has no dependency on :backend. */
private fun BookmarkSort.labelRes(): Int = when (this) {
    BookmarkSort.NEWEST -> com.neverreader.ui.R.string.sort_newest
    BookmarkSort.OLDEST -> com.neverreader.ui.R.string.sort_oldest
    BookmarkSort.TITLE_ASC -> com.neverreader.ui.R.string.sort_title_asc
    BookmarkSort.TITLE_DESC -> com.neverreader.ui.R.string.sort_title_desc
}

/**
 * The saves list, a navigation destination.
 *
 * Navigation is passed in as callbacks rather than reached for through a
 * NavController, so this has no dependency on how the host is structured.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    viewModel: MyListViewModel,
    loadImage: suspend (String) -> ByteArray?,
    onOpenAddUrl: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenReader: (id: String, url: String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(viewModel) {
        viewModel.navigationEvents.collect { event ->
            when (event) {
                is MyListNavigationEvent.OpenReader -> onOpenReader(event.id, event.url)
            }
        }
    }
    val items = viewModel.pagedBookmarks.collectAsLazyPagingItems()
    var refreshing by remember { mutableStateOf(false) }
    val sortFilter by viewModel.sortFilterState.collectAsStateWithLifecycle()
    // Search UI state lives here, not in the ViewModel: the field has to stay
    // responsive while the query itself is debounced.
    // Seeded from the ViewModel, which outlives a configuration change: otherwise
    // a rotation would close the field while leaving the list filtered by it.
    var searching by remember { mutableStateOf(viewModel.searchText.value.isNotBlank()) }
    var query by remember { mutableStateOf(viewModel.searchText.value) }

    val exitSearch = {
        searching = false
        query = ""
        viewModel.clearSearch()
    }
    // Search mode is local UI state with no screen of its own, so back has to be
    // consumed here, or it would leave the app instead of closing the field.
    BackHandler(enabled = searching) { exitSearch() }

    // One root layout: a bare ComposeView positions every top-level child at
    // (0,0), so the app bar, the filter chips and the list all drew on top
    // of each other.
    Column(Modifier.fillMaxSize()) {
        AppBar(
            navigationIcon = {
                // While searching, the bar's actions would crowd out the field, so
                // they are replaced by a way out.
                if (searching) {
                    AppIconButton(onClick = exitSearch) { UpIcon() }
                }
            },
            title = {
                if (searching) {
                    SearchField(
                        query = query,
                        onQueryChange = {
                            query = it
                            viewModel.onSearchChange(it)
                        },
                        autoFocus = true,
                        modifier = Modifier.padding(end = AppTheme.dimensions.spaceSmall),
                    )
                } else {
                    Text(stringResource(R.string.nm_app))
                }
            },
            actions = {
                if (searching) return@AppBar
                AppIconButton(onClick = onOpenAddUrl) {
                    Icon(
                        painter = painterResource(com.neverreader.ui.R.drawable.ic_nr_add_tags_line),
                        contentDescription = stringResource(R.string.settings_add_url),
                    )
                }
                AppIconButton(onClick = { searching = true }) {
                    Icon(
                        painter = painterResource(com.neverreader.ui.R.drawable.ic_pkt_search_line),
                        contentDescription = stringResource(com.neverreader.ui.R.string.ic_search),
                        tint = AppTheme.colors.grey3,
                    )
                }
                // Settings used to live in the options' menu. The Compose screens
                // have no ActionBar, so without this the settings screen is
                // unreachable - onOpenSettings was being passed in and never called.
                AppIconButton(onClick = onOpenSettings) {
                    Icon(
                        painter = painterResource(R.drawable.ic_menu_settings),
                        contentDescription = stringResource(R.string.settings_title),
                        tint = AppTheme.colors.grey3,
                    )
                }
            },
        )

        FilterChips(
            tabs = ListManager.Tab.entries,
            selected = sortFilter.tab,
            onSelect = viewModel::setTab,
            label = { tab ->
                stringResource(
                    when (tab) {
                        ListManager.Tab.UNREAD -> R.string.my_list_tab_unread
                        ListManager.Tab.FAVORITES -> R.string.my_list_filter_favorites
                        ListManager.Tab.ARCHIVE -> R.string.nm_archive
                    }
                )
            },
            trailing = {
                MenuChip(
                    options = BookmarkSort.entries,
                    selected = sortFilter.sort,
                    optionLabel = { stringResource(it.labelRes()) },
                    onSelect = viewModel::setSort,
                    icon = com.neverreader.ui.R.drawable.ic_pkt_sort_line,
                    iconContentDescription = com.neverreader.ui.R.string.ic_sort,
                )
            },
        )

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                SyncWorker.enqueueNow(context)
                scope.launch {
                    // The list updates from the sync; clear the spinner on the next
                    // emission rather than guessing at a duration.
                    items.refresh()
                    refreshing = false
                }
            },
            // The list takes the height left under the app bar and chips, rather
            // than the full height, or it spills past the bottom of the column.
            modifier = Modifier.weight(1f),
        ) {
            LazyColumn(Modifier.fillMaxSize()) {
                items(
                    count = items.itemCount,
                    key = { index -> items.peek(index)?.bookmark?.id ?: index },
                ) { index ->
                    val state = items[index] ?: return@items
                    // No SwipeToDismissBox here. It swallowed every tap meant for the
                    // row - tapping an article did nothing at all, which is why the
                    // reader looked broken. The row's own archive button is the
                    // single archive affordance.
                    ItemRow(
                            title = state.title,
                            domain = state.domain,
                            meta = state.meta,
                            excerpt = state.excerpt,
                            imageUrl = state.imageUrl,
                            loadImage = loadImage,
                            favorite = state.favorite,
                            unread = state.unread,
                            savedDate = state.savedDate,
                            onClick = { viewModel.onItemClicked(state.bookmark) },
                            onToggleFavorite = { viewModel.toggleFavorite(state.bookmark) },
                            onShare = { share(context, state.bookmark) },
                            onArchive = { viewModel.archive(state.bookmark) },
                        )
                }

                // These strings survived the XML layouts; nothing had been using
                // them, so an empty list - or a search with no hits - was just a
                // blank screen.
                //
                // Gated on the load state too: itemCount is 0 while the first
                // page is still being queried, which on a large list is long
                // enough to flash "Start building your list" at a full library.
                if (items.itemCount == 0 &&
                    !refreshing &&
                    items.loadState.refresh is androidx.paging.LoadState.NotLoading
                ) {
                    item {
                        EmptyState(
                            title = stringResource(emptyTitleRes(sortFilter)),
                            message = stringResource(
                                if (sortFilter.search != null) {
                                    R.string.list_empty_no_result_matched
                                } else {
                                    R.string.empty_list_all_message
                                },
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppTheme.dimensions.sideGrid),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AppTheme.typography.h6,
            color = AppTheme.colors.grey1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(AppTheme.dimensions.spaceSmall))
        Text(
            text = message,
            style = AppTheme.typography.p4,
            color = AppTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

private fun emptyTitleRes(state: ListManager.SortFilterState): Int = when {
    state.search != null -> R.string.list_empty_search_title
    state.tab == ListManager.Tab.FAVORITES -> R.string.empty_list_favorites_title
    state.tab == ListManager.Tab.ARCHIVE -> R.string.empty_list_archive_title
    else -> R.string.empty_list_all_title
}

private fun share(context: android.content.Context, bookmark: Bookmark) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, bookmark.title)
        putExtra(Intent.EXTRA_TEXT, bookmark.url)
    }
    context.startActivity(Intent.createChooser(send, null))
}
