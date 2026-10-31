package com.porthub.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * One place that decides how wide things get. Every screen reads this instead of
 * hardcoding columns, gutters and hero heights — that is what made tablets look
 * like a phone with the zoom turned up.
 */
data class LayoutSpec(
    val widthDp: Int,
    /** Side rail instead of a bottom bar, two columns of content, wider gutters. */
    val isWide: Boolean,
    /** Room for a side-by-side master/detail list. */
    val isExtraWide: Boolean,
    /** Minimum width a grid cell is allowed to shrink to. */
    val gridMin: Dp,
    /** Horizontal page margin. */
    val gutter: Dp,
    /** Reading width for long text; content is centred above this. */
    val maxContent: Dp,
    val heroHeight: Dp,
    val coverWidth: Dp,
    val coverHeight: Dp,
    val tabBarItemWidth: Dp,
) {
    val columns: Int
        get() = when {
            widthDp >= 1200 -> 5
            widthDp >= 960 -> 4
            widthDp >= 700 -> 3
            else -> 2
        }
}

val CompactLayout = LayoutSpec(
    widthDp = 400,
    isWide = false,
    isExtraWide = false,
    gridMin = 138.dp,
    gutter = 20.dp,
    maxContent = Dp.Infinity,
    heroHeight = 300.dp,
    coverWidth = 84.dp,
    coverHeight = 108.dp,
    tabBarItemWidth = 72.dp,
)

val WideLayout = LayoutSpec(
    widthDp = 900,
    isWide = true,
    isExtraWide = true,
    gridMin = 158.dp,
    gutter = 32.dp,
    maxContent = 1180.dp,
    heroHeight = 340.dp,
    coverWidth = 104.dp,
    coverHeight = 134.dp,
    tabBarItemWidth = 76.dp,
)

val LocalAppLayout = compositionLocalOf { CompactLayout }

@Composable
@ReadOnlyComposable
fun appLayout(): LayoutSpec {
    val width = LocalConfiguration.current.screenWidthDp
    return when {
        width >= 840 -> WideLayout.copy(widthDp = width)
        width >= 600 -> WideLayout.copy(widthDp = width, isExtraWide = false, maxContent = 900.dp, heroHeight = 300.dp)
        else -> CompactLayout.copy(widthDp = width)
    }
}
