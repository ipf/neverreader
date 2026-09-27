package com.neverreader.app.list

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.neverreader.app.R
import com.neverreader.app.list.add.AddUrlBottomSheetFragment
import com.neverreader.app.repository.ThumbnailRepository
import com.neverreader.app.list.list.ListManager
import com.neverreader.backend.model.Bookmark
import com.neverreader.backend.sync.SyncWorker
import com.neverreader.sdk.util.AbsNeverReaderFragment
import com.neverreader.ui.compose.AppBar
import com.neverreader.ui.compose.FilterChips
import com.neverreader.ui.compose.ItemRow
import com.neverreader.ui.compose.SwipeToArchiveBackground
import com.neverreader.ui.theme.AppTheme
import com.neverreader.ui.view.button.AppIconButton
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * The save list: unread / favorites / archive with filtering.
 */
@AndroidEntryPoint
class MyListFragment : AbsNeverReaderFragment() {

    private val viewModel: MyListViewModel by viewModels()

    @Inject
    lateinit var thumbnailRepository: ThumbnailRepository

    override fun onCreateViewImpl(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            AppTheme {
                MyListScreen(
                    viewModel = viewModel,
                    onOpenAddUrl = ::showAddUrl,
                    onOpenSettings = ::openSettings,
                    loadImage = { url -> thumbnailRepository.load(url) },
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.navigationEvents.collect { event ->
                    when (event) {
                        is MyListNavigationEvent.ShowAddUrl -> showAddUrl()
                        is MyListNavigationEvent.OpenReader -> openReader(event.url)
                    }
                }
            }
        }
    }

    private fun openReader(url: String) {
        // Tapping an article used to emit OpenReader into a no-op: both this
        // fragment and MainActivity deferred to each other, so the reader was
        // unreachable.
        findNavController().navigate(R.id.goToReader, bundleOf("url" to url))
    }

    private fun showAddUrl() {
        AddUrlBottomSheetFragment().show(parentFragmentManager, AddUrlBottomSheetFragment::class.java.name)
    }

    private fun openSettings() {
        findNavController().navigate(R.id.goToSettings)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyListScreen(
    viewModel: MyListViewModel,
    onOpenAddUrl: () -> Unit,
    onOpenSettings: () -> Unit,
    loadImage: suspend (String) -> ByteArray?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val items = viewModel.pagedBookmarks.collectAsLazyPagingItems()
    var refreshing by remember { mutableStateOf(false) }
    val sortFilter by viewModel.sortFilterState.collectAsStateWithLifecycle()

    // One root layout: a bare ComposeView positions every top-level child at
    // (0,0), so the app bar, the filter chips and the list all drew on top
    // of each other.
    Column(Modifier.fillMaxSize()) {
        AppBar(
            title = { Text(stringResource(R.string.nm_app)) },
            actions = {
                AppIconButton(onClick = onOpenAddUrl) {
                    Icon(
                        painter = painterResource(com.neverreader.ui.R.drawable.ic_nr_add_tags_line),
                        contentDescription = stringResource(R.string.settings_add_url),
                    )
                }
                // Settings used to live in the options menu. The Compose screens
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
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value != SwipeToDismissBoxValue.Settled) {
                                viewModel.archive(state.bookmark)
                            }
                            // Never settle on a value: the paging diff removes the row,
                            // and settling would animate it back into view.
                            false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = { SwipeToArchiveBackground() },
                    ) {
                        ItemRow(
                            title = state.title,
                            domain = state.domain,
                            meta = state.meta,
                            excerpt = state.excerpt,
                            imageUrl = state.imageUrl,
                            loadImage = loadImage,
                            favorite = state.favorite,
                            unread = state.unread,
                            onClick = { viewModel.onItemClicked(state.bookmark) },
                            onToggleFavorite = { viewModel.toggleFavorite(state.bookmark) },
                            onShare = { share(context, state.bookmark) },
                            onArchive = { viewModel.archive(state.bookmark) },
                        )
                    }
                }
            }
        }
    }
}

private fun share(context: android.content.Context, bookmark: Bookmark) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, bookmark.title)
        putExtra(Intent.EXTRA_TEXT, bookmark.url)
    }
    context.startActivity(Intent.createChooser(send, null))
}
