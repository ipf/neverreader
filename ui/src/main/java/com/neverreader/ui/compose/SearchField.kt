package com.neverreader.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppRadii
import com.neverreader.ui.theme.AppTheme

/**
 * The design system's first text input, so it is built on BasicTextField rather
 * than Material's TextField: the latter brings its own container, indicator and
 * colours, and every other component here is hand-built from AppTheme tokens so
 * it can be themed per surface. The cursor is tinted rather than left as the
 * platform accent, which would ignore the in-app theme.
 */
@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Focus and raise the keyboard as soon as the field appears. */
    autoFocus: Boolean = false,
) {
    val colors = AppTheme.colors
    val typography = AppTheme.typography
    val keyboard = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadii.chip))
            .background(colors.chipBackground)
            .padding(start = 14.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pkt_search_line),
            contentDescription = null,
            tint = colors.grey3,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(8.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = typography.p4.copy(color = colors.grey1),
            cursorBrush = SolidColor(colors.teal1),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_hint),
                        style = typography.p4,
                        color = colors.grey4,
                    )
                }
                inner()
            },
        )
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_pkt_close_x_line),
                    contentDescription = stringResource(R.string.search_clear),
                    tint = colors.grey3,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
