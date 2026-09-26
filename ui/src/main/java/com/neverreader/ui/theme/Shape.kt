package com.neverreader.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Corner radii transcribed from the legacy `nr_*` dimens.
 */
object AppRadii {
    val card: Dp = 6.dp
    val button: Dp = 4.dp
    val bottomSheet: Dp = 28.dp
    val chip: Dp = 14.dp
}

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(AppRadii.button),
    medium = RoundedCornerShape(AppRadii.card),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(AppRadii.bottomSheet),
)
