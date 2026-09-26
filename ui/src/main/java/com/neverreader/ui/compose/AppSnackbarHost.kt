package com.neverreader.ui.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neverreader.ui.theme.AppTheme

/**
 * A snackbar host that matches the app's colours.
 *
 * Replaces the old `AppSnackbar` view, which needed the `state_dark` machinery
 * to pick its background and so kept the whole themed-view layer alive for the
 * sake of six toasts.
 */
@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(AppTheme.dimensions.sideGrid),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = AppTheme.colors.grey2,
                contentColor = AppTheme.colors.onBackground,
                actionColor = AppTheme.colors.teal6,
                shape = MaterialTheme.shapes.medium,
            )
        }
    }
}
