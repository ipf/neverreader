package com.neverreader.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neverreader.ui.theme.AppRadii
import com.neverreader.ui.theme.AppTheme

/**
 * A chip that shows the current choice and opens a menu of the alternatives.
 *
 * Generic over the option type on purpose: the design system has no dependency on
 * :backend, so the caller supplies both the options and their labels.
 */
@Composable
fun <T> MenuChip(
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    icon: Int,
    iconContentDescription: Int,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(AppRadii.chip))
            .background(colors.chipBackground)
            .clickable { expanded = true }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = optionLabel(selected),
            style = AppTheme.typography.p4,
            color = colors.grey3,
        )
        Spacer(Modifier.width(AppTheme.dimensions.spaceSmall))
        Icon(
            painter = painterResource(icon),
            contentDescription = stringResource(iconContentDescription),
            tint = colors.grey3,
            modifier = Modifier.size(16.dp),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            // Material draws the menu on its own surfaceContainer colour, which
            // ignored the in-app theme and came out lavender.
            shape = RoundedCornerShape(AppRadii.chip),
            containerColor = colors.chipBackground,
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = optionLabel(option),
                            style = AppTheme.typography.p4,
                            // The current choice is already spelled out on the chip,
                            // so this is the one that reads as selected.
                            color = if (option == selected) colors.teal1 else colors.grey1,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
