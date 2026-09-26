package com.neverreader.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neverreader.ui.R
import com.neverreader.ui.view.themed.AppTheme

@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(dimensionResource(R.dimen.nr_thin_divider_height))
            .background(AppTheme.colors.grey6)
    )
}

@Preview()
@Composable
fun DividersPreview() {
    Column(
        Modifier.padding(horizontal = 20.dp, vertical = dimensionResource(R.dimen.nr_space_sm))
    ) {
        ThinDivider()
        // Spacer()
        // ThickDivider()
        // etc.
    }
}
