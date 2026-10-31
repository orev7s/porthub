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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

enum class Tab(val label: String, val icon: ImageVector) {
    Discover("Discover", Icons.Rounded.Home),
    Search("Search", Icons.Rounded.Search),
    Library("Library", Icons.Rounded.FavoriteBorder),
    Settings("Settings", Icons.Rounded.Settings),
}

@Composable
fun PortHubApp(vm: CatalogViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favoriteIds.collectAsStateWithLifecycle()

    var tabName by rememberSaveable { mutableStateOf(Tab.Discover.name) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentTab = runCatching { Tab.valueOf(tabName) }.getOrDefault(Tab.Discover)

    Box(Modifier.fillMaxSize().background(Ink.Background)) {
        BloomBackground(Modifier.fillMaxSize())

        AnimatedContent(
            targetState = openId,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally(tween(320)) { it / 5 } + fadeIn(tween(240)) + scaleIn(tween(320), initialScale = 0.985f))
                        .togetherWith(fadeOut(tween(150)))
                } else {
                    (fadeIn(tween(220)) + scaleIn(tween(260), initialScale = 0.99f))
                        .togetherWith(slideOutHorizontally(tween(280)) { it / 5 } + fadeOut(tween(150)))
                }
            },
            label = "root",
        ) { id ->
            val game = state.games.firstOrNull { it.id == id }
            if (game == null) {
                Box(Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            val forward = targetState.ordinal > initialState.ordinal
                            val shift = if (forward) 1 else -1
                            (fadeIn(tween(260, delayMillis = 40)) + slideInHorizontally(tween(320)) { it / 18 * shift })
                                .togetherWith(
                                    fadeOut(tween(130)) + slideOutHorizontally(tween(260)) { -it / 22 * shift },
                                )
                        },
                        label = "tab",
                    ) { tab ->
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

                    GlassTabBar(
                        current = currentTab,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onSelect = { tabName = it.name },
                    )
                }
            } else {
                DetailScreen(
                    game = game,
                    isFavorite = game.id in favorites,
                    onBack = { openId = null },
                    onToggleFavorite = { vm.toggleFavorite(game.id) },
                )
            }
        }
    }
}

@Composable
private fun GlassTabBar(
    current: Tab,
    modifier: Modifier = Modifier,
    onSelect: (Tab) -> Unit,
) {
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
            TabBarItem(
                tab = tab,
                selected = tab == current,
                onClick = { onSelect(tab) },
            )
        }
    }
}

@Composable
private fun TabBarItem(
    tab: Tab,
    selected: Boolean,
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
            .width(66.dp)
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
            tab.label,
            style = AppText.Caption.copy(fontSize = 9.5.sp, letterSpacing = 0.1.sp),
            color = tint,
        )
    }
}
