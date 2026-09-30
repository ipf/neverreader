package com.neverreader.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neverreader.app.list.MyListScreen
import com.neverreader.app.list.MyListViewModel
import com.neverreader.app.list.add.AddUrlSheet
import com.neverreader.app.reader.ReaderScreen
import com.neverreader.app.reader.ReaderViewModel
import com.neverreader.app.repository.ThumbnailRepository
import com.neverreader.app.settings.SettingsScreen
import com.neverreader.sdk.preferences.AppPrefs

/** Destinations, as routes. The graph lives in Kotlin, not in navigation XML. */
object Routes {
    const val SAVES = "saves"
    const val SETTINGS = "settings"
    const val ARG_ID = "id"
    const val ARG_URL = "url"
    const val READER = "reader/{$ARG_ID}?$ARG_URL={$ARG_URL}"

    /**
     * The url travels as a query argument rather than a path segment: an article
     * url routinely contains slashes, question marks, and fragments, and encoding
     * all of that into a path segment is at best fragile and at worst silently
     * truncated.
     */
    fun reader(id: String, url: String) = "reader/${Uri.encode(id)}?$ARG_URL=${Uri.encode(url)}"
}

/**
 * The app's navigation graph, in Kotlin.
 *
 * Destinations are composables, so there is no NavHostFragment, no
 * FragmentContainerView, and no navigation XML. ViewModels are scoped to the back
 * stack entry, which is what gives each screen the same instance across
 * recomposition and its own instance from its neighbors'.
 */
@Composable
fun AppNavHost(
    thumbnailRepository: ThumbnailRepository,
    userManager: UserManager,
    appPrefs: AppPrefs,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current

    // The add sheet is a dialog over the list rather than a destination, so that
    // dismissing it leaves the list exactly as it was.
    var addUrlSheet by rememberSaveable { mutableStateOf(false) }
    if (addUrlSheet) {
        AddUrlSheet(onDismiss = { addUrlSheet = false })
    }

    NavHost(
        navController = navController,
        startDestination = Routes.SAVES,
        modifier = modifier,
    ) {
        composable(Routes.SAVES) {
            val viewModel: MyListViewModel = hiltViewModel()
            MyListScreen(
                viewModel = viewModel,
                loadImage = { url -> thumbnailRepository.load(url) },
                onOpenAddUrl = { addUrlSheet = true },
                onOpenSettings = { navController.navigateTop(Routes.SETTINGS) },
                onOpenReader = { id, url -> navController.navigateTop(Routes.reader(id, url)) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                appPrefs = appPrefs,
                userManager = userManager,
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.READER,
            arguments = listOf(
                navArgument(Routes.ARG_ID) { type = NavType.StringType },
                navArgument(Routes.ARG_URL) { type = NavType.StringType },
            ),
        ) { entry ->
            val viewModel: ReaderViewModel = hiltViewModel()
            ReaderScreen(
                viewModel = viewModel,
                articleId = entry.arguments?.getString(Routes.ARG_ID).orEmpty(),
                articleUrl = entry.arguments?.getString(Routes.ARG_URL).orEmpty(),
                darkTheme = darkTheme,
                onBack = { navController.popBackStack() },
                onShare = { url, title -> share(context, url, title) },
            )
        }
    }
}

/**
 * Navigate without stacking a duplicate of the same destination on top of
 * itself: the list can emit a tap twice while a scroll settles.
 *
 * [androidx.navigation.NavOptionsBuilder.launchSingleTop] is the built-in comparison, and it
 * matches on the full route, so a different article still opens.
 */
private fun NavHostController.navigateTop(route: String) {
    navigate(route) { launchSingleTop = true }
}

private fun share(context: Context, url: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, url)
    }
    context.startActivity(Intent.createChooser(send, null))
}
