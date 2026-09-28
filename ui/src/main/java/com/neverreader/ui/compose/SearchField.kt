package com.neverreader.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neverreader.ui.R
import com.neverreader.ui.theme.AppRadii
import com.neverreader.ui.theme.AppTheme

private val SearchFieldHeight: Dp = 40.dp
private val SearchFieldBorderWidth: Dp = 1.dp

/**
 * The design system's first text input, so it is built on BasicTextField rather
 * than Material's TextField: the latter brings its own container, indicator and
 * colours, and every other component here is hand-built from AppTheme tokens so
 * it can be themed per surface. The cursor is tinted rather than left as the
 * platform accent, which would ignore the in-app theme.
 *
 * Outlined rather than filled, because the filled version used chipBackground -
 * the same token as the filter chips - so it read as a fifth chip instead of an
 * input. The border is what separates them, and it turns teal while focused.
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
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(autoFocus) {
        if (autoFocus) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    val shape = RoundedCornerShape(AppRadii.chip)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(SearchFieldHeight)
            .clip(shape)
            .background(colors.background)
            .border(
                width = SearchFieldBorderWidth,
                color = if (focused) colors.teal1 else colors.divider,
                shape = shape,
            )
            // Symmetric, so the empty field does not reserve dead space on the
            // right for a clear button that is not there yet.
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_pkt_search_line),
            contentDescription = null,
            tint = colors.grey4,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_hint),
                    style = typography.p4,
                    color = colors.grey4,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = typography.p4.copy(color = colors.grey1),
                cursorBrush = SolidColor(colors.teal1),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onFocusChanged { focused = it.isFocused },
            )
        }
        if (query.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_pkt_close_x_line),
                    contentDescription = stringResource(R.string.search_clear),
                    tint = colors.grey3,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
