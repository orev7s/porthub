package com.porthub.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.porthub.app.ui.components.BloomBackground
import com.porthub.app.ui.components.glass
import com.porthub.app.ui.components.pressScale
import com.porthub.app.ui.screens.DetailScreen
import com.porthub.app.ui.screens.HomeScreen
import com.porthub.app.ui.screens.LibraryScreen
import com.porthub.app.ui.screens.SearchScreen
import com.porthub.app.ui.screens.SettingsScreen
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink
import com.porthub.app.ui.theme.LocalAppLayout
import com.porthub.app.ui.theme.appLayout

enum class Tab(val label: String, val short: String, val icon: ImageVector) {
    Discover("Discover", "Home", Icons.Rounded.Home),
    Search("Search", "Search", Icons.Rounded.Search),
    Library("Library", "Saved", Icons.Rounded.FavoriteBorder),
    Settings("Settings", "Settings", Icons.Rounded.Settings),
}

@Composable
fun PortHubApp(vm: CatalogViewModel = viewModel()) {
    val layout = appLayout()
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()

    var tabName by rememberSaveable { mutableStateOf(Tab.Discover.name) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentTab = runCatching { Tab.valueOf(tabName) }.getOrDefault(Tab.Discover)

    CompositionLocalProvider(LocalAppLayout provides layout) {
        Box(Modifier.fillMaxSize().background(Ink.Background)) {
            BloomBackground(Modifier.fillMaxSize())

            val openGame = state.games.firstOrNull { it.id == openId }

            if (openGame == null) {
                Row(Modifier.fillMaxSize()) {
                    if (layout.isWide) {
                        SideRail(current = currentTab, onSelect = { tabName = it.name })
                    }

                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                val shift = if (forward) 1 else -1
                                (fadeIn(tween(240, delayMillis = 30)) + slideInHorizontally(tween(300)) { it / 24 * shift })
                                    .togetherWith(fadeOut(tween(120)))
                            },
                            label = "tab",
                        ) { tab ->
                            CenteredContent {
                                when (tab) {
                                    Tab.Discover -> HomeScreen(
                                        games = state.games,
                                        loading = state.loading,
                                        onOpen = { openId = it.id },
                                        onSearch = { tabName = Tab.Search.name },
                                        onSeeAll = { tabName = Tab.Search.name },
                                    )

                                    Tab.Search -> SearchScreen(
                                        games = state.games,
                                        onOpen = { openId = it.id },
                                    )

                                    Tab.Library -> LibraryScreen(
                                        games = state.games,
                                        favoriteIds = favorites,
                                        onOpen = { openId = it.id },
                                    )

                                    Tab.Settings -> SettingsScreen(
                                        feedUrl = vm.feedUrl,
                                        source = state.source?.name,
                                        gameCount = state.games.size,
                                        refreshing = state.refreshing,
                                        onApplyFeed = { vm.setFeed(it) },
                                        onRefresh = { vm.load(true) },
                                    )
                                }
                            }
                        }

                        if (!layout.isWide) {
                            GlassTabBar(
                                current = currentTab,
                                modifier = Modifier.align(Alignment.BottomCenter),
                                onSelect = { tabName = it.name },
                            )
                        }
                    }
                }
            } else {
                CenteredContent(maxWidth = 900.dp) {
                    DetailScreen(
                        game = openGame,
                        isFavorite = openGame.id in favorites,
                        onBack = { openId = null },
                        onToggleFavorite = { vm.toggleFavorite(openGame.id) },
                    )
                }
            }
        }
    }
}

/** Keeps line lengths readable on a tablet instead of stretching edge to edge. */
@Composable
private fun CenteredContent(
    maxWidth: androidx.compose.ui.unit.Dp = LocalAppLayout.current.maxContent,
    content: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = maxWidth).fillMaxSize()) { content() }
    }
}

/* ------------------------------------------------------------------ side rail */

@Composable
private fun SideRail(current: Tab, onSelect: (Tab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .statusBarsPadding()
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp)
            .width(104.dp)
            .clip(RoundedCornerShape(28.dp))
            .glass(shape = RoundedCornerShape(28.dp), topAlpha = 0.12f, shadowElevation = 20.dp)
            .padding(vertical = 16.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            "PH",
            style = AppText.Caption.copy(fontSize = 13.sp, letterSpacing = 1.sp),
            color = Ink.TextSecondary,
            modifier = Modifier.padding(bottom = 10.dp),
        )

        Tab.entries.forEach { tab ->
            RailItem(tab = tab, selected = tab == current, onClick = { onSelect(tab) })
        }
    }
}

@Composable
private fun RailItem(tab: Tab, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(260),
        label = "railBg",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) Ink.TextPrimary else Ink.TextTertiary,
        animationSpec = tween(200),
        label = "railTint",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.94f, interactionSource = interaction)
            .clip(RoundedCornerShape(18.dp))
            .background(background)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (selected && tab == Tab.Library) Icons.Rounded.Favorite else tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.height(5.dp))
        Text(
            tab.short,
            style = AppText.Caption.copy(fontSize = 10.sp),
            color = tint,
            maxLines = 1,
        )
    }
}

/* ---------------------------------------------------------------- bottom bar */

@Composable
private fun GlassTabBar(
    current: Tab,
    modifier: Modifier = Modifier,
    onSelect: (Tab) -> Unit,
) {
    val layout = LocalAppLayout.current

    Row(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(30.dp))
            .glass(shape = RoundedCornerShape(30.dp), topAlpha = 0.14f, shadowElevation = 28.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            BottomTabItem(tab = tab, selected = tab == current, width = layout.tabBarItemWidth) { onSelect(tab) }
        }
    }
}

@Composable
private fun BottomTabItem(
    tab: Tab,
    selected: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val background by animateColorAsState(
        targetValue = if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        animationSpec = tween(280),
        label = "tabBg",
    )
    val tint by animateColorAsState(
        targetValue = if (selected) Ink.TextPrimary else Ink.TextTertiary,
        animationSpec = tween(220),
        label = "tabTint",
    )
    val lift by animateFloatAsState(
        targetValue = if (selected) 0f else 1.2f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "tabLift",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "tabScale",
    )

    Column(
        modifier = Modifier
            .width(width)
            .pressScale(pressedScale = 0.93f, interactionSource = interaction)
            .clip(RoundedCornerShape(22.dp))
            .background(background)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = if (selected && tab == Tab.Library) Icons.Rounded.Favorite else tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier
                .size(21.dp)
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                    translationY = lift
                },
        )
        Spacer(Modifier.height(3.dp))
        Text(
            tab.short,
            style = AppText.Caption.copy(fontSize = 10.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            textAlign = TextAlign.Center,
        )
    }
}
