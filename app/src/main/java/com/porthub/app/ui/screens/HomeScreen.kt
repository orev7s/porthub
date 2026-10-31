package com.porthub.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.porthub.app.data.Game
import com.porthub.app.data.PortStatus
import com.porthub.app.data.PortType
import com.porthub.app.ui.components.Chip
import com.porthub.app.ui.components.CoverArt
import com.porthub.app.ui.components.GameRow
import com.porthub.app.ui.components.SectionHeader
import com.porthub.app.ui.components.StaggeredAppear
import com.porthub.app.ui.components.glass
import com.porthub.app.ui.components.pressScale
import com.porthub.app.ui.components.pulse
import com.porthub.app.ui.components.statusColor
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink
import com.porthub.app.ui.theme.LocalAppLayout

@Composable
fun HomeScreen(
    games: List<Game>,
    loading: Boolean,
    onOpen: (Game) -> Unit,
    onSearch: () -> Unit,
    onSeeAll: () -> Unit,
) {
    val layout = LocalAppLayout.current
    val featured = remember(games) { games.sortedByDescending { it.stars ?: 0 }.take(6) }
    val latest = remember(games) { games.take(10) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp, bottom = if (layout.isWide) 48.dp else 130.dp),
    ) {
        item {
            StaggeredAppear(index = 0) {
                Column(Modifier.padding(horizontal = layout.gutter)) {
                    Text("OPEN SOURCE · ANDROID", style = AppText.Eyebrow)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Discover", style = AppText.LargeTitle)
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(Ink.Accent.copy(alpha = 0.14f))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text("${games.size} ports", style = AppText.Caption.copy(fontSize = 12.sp), color = Ink.Accent)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    SearchBarButton(onClick = onSearch)
                }
            }
        }

        if (featured.isNotEmpty()) {
            item {
                StaggeredAppear(index = 1) {
                    Column {
                        Spacer(Modifier.height(26.dp))
                        SectionHeader("Featured", modifier = Modifier.padding(horizontal = layout.gutter), action = "See all", onAction = onSeeAll)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            item {
                StaggeredAppear(index = 2) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = layout.gutter),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(featured, key = { "feat-${it.id}" }) { game ->
                            FeaturedCard(game = game, onClick = { onOpen(game) }, width = if (layout.isWide) 360.dp else 292.dp, height = if (layout.isWide) 212.dp else 184.dp)
                        }
                    }
                }
            }
        }

        item {
            StaggeredAppear(index = 3) {
                Column {
                    Spacer(Modifier.height(28.dp))
                    SectionHeader("Latest additions", modifier = Modifier.padding(horizontal = layout.gutter))
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        if (loading && games.isEmpty()) {
            items(4) { SkeletonRow(Modifier.padding(horizontal = layout.gutter, vertical = 5.dp)) }
        } else if (games.isEmpty()) {
            item { EmptyState(Modifier.padding(horizontal = layout.gutter)) }
        } else {
            items(latest, key = { "row-${it.id}" }) { game ->
                GameRow(
                    game = game,
                    onClick = { onOpen(game) },
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = 20.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun SearchBarButton(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.985f, interactionSource = interaction)
            .clip(RoundedCornerShape(18.dp))
            .glass(shape = RoundedCornerShape(18.dp), shadowElevation = 0.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = Ink.TextTertiary, modifier = Modifier.size(19.dp))
        Text("Search a game, engine, console…", style = AppText.Body.copy(fontSize = 15.sp), color = Ink.TextTertiary)
    }
}

@Composable
private fun FeaturedCard(game: Game, onClick: () -> Unit, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .pressScale(pressedScale = 0.975f, interactionSource = interaction)
            .clip(RoundedCornerShape(26.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
    ) {
        CoverArt(game = game, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(26.dp))
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.55f to Color.Black.copy(alpha = 0.25f),
                        1f to Color.Black.copy(alpha = 0.82f),
                    ),
                ),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(PortType.label(game.portType), tint = Ink.TextPrimary)
                Chip(PortStatus.label(game.status), tint = statusColor(game.status))
            }
            Spacer(Modifier.height(9.dp))
            Text(game.title, style = AppText.ScreenTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            game.originalTitle?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, style = AppText.Caption.copy(fontSize = 12.sp), color = Ink.TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(94.dp)
            .clip(RoundedCornerShape(22.dp))
            .glass(shape = RoundedCornerShape(22.dp), shadowElevation = 0.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 58.dp, height = 74.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .pulse(),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Box(Modifier.width(150.dp).height(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)).pulse())
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(96.dp).height(10.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.04f)).pulse())
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .glass(shape = RoundedCornerShape(22.dp), shadowElevation = 0.dp)
            .padding(20.dp),
    ) {
        Text("Nothing here yet", style = AppText.CardTitle)
        Spacer(Modifier.height(6.dp))
        Text("The feed is empty or unreachable. Pull the feed again from Settings.", style = AppText.Body)
    }
}
