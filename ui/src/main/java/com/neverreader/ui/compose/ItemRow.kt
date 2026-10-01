package com.neverreader.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppRadii
import com.neverreader.ui.theme.AppTheme
import com.neverreader.ui.view.ThinDivider
import com.neverreader.ui.view.button.AppIconButton
import com.neverreader.ui.view.button.ArchiveIcon

/**
 * A saved article in the list.
 *
 * The meta-line is `domain · N min read` and drops either half when it is
 * unknown, rather than rendering an empty or literal "0 min" segment.
 */
@Composable
fun ItemRow(
    title: String,
    domain: String?,
    meta: String?,
    excerpt: String?,
    imageUrl: String?,
    loadImage: suspend (String) -> ByteArray?,
    favorite: Boolean,
    unread: Boolean,
    savedDate: String? = null,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val sideGrid = AppTheme.dimensions.sideGrid

    Column(
        modifier
            .clickable(onClick = onClick)
            .padding(horizontal = sideGrid)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.saves_row_min_height))
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Spacer(Modifier.size(AppTheme.dimensions.spaceSmall))
                Text(
                    text = title,
                    style = typography.p3,
                    color = if (unread) colors.grey1 else colors.grey2,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!domain.isNullOrBlank() || !meta.isNullOrBlank()) {
                    Spacer(Modifier.size(2.dp))
                    Text(
                        text = listOfNotNull(
                            domain?.takeIf { it.isNotBlank() },
                            meta?.takeIf { it.isNotBlank() },
                        ).joinToString(" · "),
                        style = typography.p4,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (!excerpt.isNullOrBlank()) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = excerpt,
                        style = typography.p4,
                        color = colors.textSecondary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.size(AppTheme.dimensions.spaceSmall))
            }

            if (!imageUrl.isNullOrBlank()) {
                // The bytes are fetched by the app, not by the image library, so the
                // Authorization header can be attached when the image is served by
                // the user's own server. Coil only decodes.
                val bytes by produceState<ByteArray?>(null, imageUrl) {
                    value = loadImage(imageUrl)
                }
                // Nothing is drawn until the bytes are actually here. A thumbnail
                // the server cannot serve is a dead link, and reserving a 90x60 tile
                // for it leaves a hole in the row with nothing in it - worse than
                // having no thumbnail at all, because the text beside it is narrowed
                // for a picture that never arrives.
                if (bytes != null) {
                    Spacer(Modifier.width(AppTheme.dimensions.spaceSmall))
                    AsyncImage(
                        model = bytes,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(top = AppTheme.dimensions.spaceSmall)
                            .size(
                                width = dimensionResource(R.dimen.saves_image_width),
                                height = dimensionResource(R.dimen.saves_image_height),
                            )
                            .clip(RoundedCornerShape(AppRadii.card)),
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Bottom-left: when the item was saved. The date absorbs all the
            // leftover width, so the actions end up flush right, which puts the
            // archive button on the same trailing edge as the thumbnail above.
            // This used to be a weighted date plus a second weighted spacer,
            // which split the leftover in half and left the icons stranded
            // mid-row with the remainder after them.
            if (savedDate != null) {
                Text(
                    text = savedDate,
                    style = typography.p4,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }
            AppIconButton(onClick = onToggleFavorite) {
                Icon(
                    painter = painterResource(
                        if (favorite) R.drawable.ic_nr_favorite_solid else R.drawable.ic_nr_favorite_line
                    ),
                    contentDescription = stringResource(R.string.ic_favorite),
                    tint = if (favorite) AppTheme.colors.amber3 else AppTheme.colors.grey3,
                )
            }
            AppIconButton(onClick = onShare) {
                Icon(
                    painter = painterResource(R.drawable.ic_nr_android_share_solid),
                    contentDescription = stringResource(R.string.ic_share),
                    tint = AppTheme.colors.grey3,
                )
            }
            AppIconButton(onClick = onArchive) {
                ArchiveIcon()
            }
        }

        ThinDivider()
    }
}

@Preview
@Composable
private fun ItemRowPreview() {
    AppTheme {
        ItemRow(
            title = "The Design System Pocket Wished It Had",
            domain = "example.com",
            meta = "6 min read",
            excerpt = "A long look at why design tokens drift, and what to do about it.",
            imageUrl = null,
            loadImage = { null },
            favorite = true,
            unread = true,
            onClick = {},
            onToggleFavorite = {},
            onShare = {},
            onArchive = {},
        )
    }
}
