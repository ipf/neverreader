package com.neverreader.ui.view.button

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.neverreader.ui.R

@Composable
fun UpIcon(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_nr_back_arrow_line),
        stringResource(R.string.ic_up),
        modifier,
    )
}

/**
 * The archive box. Every action icon is a 24x24 vector on a 50dp AppIconButton;
 * ic_nr_android_overflow_solid was 4x24, so it sat off-centre in its button and
 * made the row look unaligned.
 */
@Composable
fun ArchiveIcon(modifier: Modifier = Modifier) {
    Icon(
        painterResource(R.drawable.ic_nr_archive_line),
        stringResource(R.string.ic_archive),
        modifier,
    )
}
