package com.neverreader.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppRadii
import com.neverreader.ui.theme.AppTheme

/**
 * The horizontally scrolling filter row above the list.
 *
 * @param labels resolves a tab to its display string, so this component does not
 *   need to know about the app's domain enum.
 */
@Composable
fun <T> FilterChips(
    tabs: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(
                horizontal = AppTheme.dimensions.sideGrid,
                vertical = AppTheme.dimensions.spaceSmall,
            ),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall),
    ) {
        tabs.forEach { tab ->
            FilterChip(
                label = label(tab),
                selected = tab == selected,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    Text(
        text = label,
        style = AppTheme.typography.p4,
        color = if (selected) colors.teal1 else colors.grey3,
        modifier = modifier
            .clip(RoundedCornerShape(AppRadii.chip))
            .background(if (selected) colors.chipSelectedBackground else colors.chipBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    )
}

/**
 * Revealed behind a row while it is swiped away, matching the archive glyph the
 * old `leftSwipeImage` / `rightSwipeImage` pair used.
 */
@Composable
fun SwipeToArchiveBackground(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(AppTheme.colors.chipBackground)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_nr_archive_line),
            contentDescription = stringResource(R.string.ic_archive),
            tint = AppTheme.colors.grey3,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 40.dp)
                .size(18.dp),
        )
        Icon(
            painter = painterResource(R.drawable.ic_nr_archive_line),
            contentDescription = stringResource(R.string.ic_archive),
            tint = AppTheme.colors.grey3,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 40.dp)
                .size(18.dp),
        )
    }
}

@Preview
@Composable
private fun FilterChipsPreview() {
    AppTheme {
        FilterChips(
            tabs = listOf("Unread", "Favorites", "Archive"),
            selected = "Unread",
            onSelect = {},
            label = { it },
        )
    }
}
