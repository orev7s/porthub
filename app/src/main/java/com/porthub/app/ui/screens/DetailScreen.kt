package com.porthub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.porthub.app.data.Game
import com.porthub.app.data.GameFiles
import com.porthub.app.data.PortStatus
import com.porthub.app.data.PortType
import com.porthub.app.ui.components.Chip
import com.porthub.app.ui.components.CoverArt
import com.porthub.app.ui.components.GlassButton
import com.porthub.app.ui.components.IconCircleButton
import com.porthub.app.ui.components.glass
import com.porthub.app.ui.components.statusColor
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink

@Composable
fun DetailScreen(
    game: Game,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val context = LocalContext.current
    BackHandler(onBack = onBack)

    fun open(url: String?) {
        if (url.isNullOrBlank()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    val scroll = rememberScrollState()
    // Hero drifts and fades as the page scrolls — the parallax cue people expect on iOS.
    val heroLift by animateFloatAsState(
        targetValue = -(scroll.value * 0.28f),
        animationSpec = tween(0),
        label = "heroLift",
    )
    val heroAlpha by animateFloatAsState(
        targetValue = (1f - scroll.value / 700f).coerceIn(0.15f, 1f),
        animationSpec = tween(0),
        label = "heroAlpha",
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
        ) {
            // ---------------------------------------------------------- hero
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(300.dp),
            ) {
                val heroUrl = game.heroUrl
                if (!heroUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(heroUrl).crossfade(300).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = heroLift
                                alpha = heroAlpha
                                scaleX = 1.04f
                                scaleY = 1.04f
                            },
                    )
                } else {
                    CoverArt(game = game, modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(0.dp))
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.35f),
                                0.5f to Color.Transparent,
                                1f to Ink.Background,
                            ),
                        ),
                )
            }

            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoverArt(
                        game = game,
                        modifier = Modifier.size(width = 84.dp, height = 108.dp),
                        shape = RoundedCornerShape(18.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(game.title, style = AppText.ScreenTitle)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            listOfNotNull(game.originalTitle, game.year?.toString(), game.developer)
                                .joinToString(" · "),
                            style = AppText.Caption.copy(fontSize = 13.sp),
                            color = Ink.TextTertiary,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Chip(PortType.label(game.portType), tint = Ink.Accent, filled = true)
                            Chip(PortStatus.label(game.status), tint = statusColor(game.status), filled = true)
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

                if (game.description.isNotBlank()) {
                    Text(game.description, style = AppText.Body)
                }

                Spacer(Modifier.height(18.dp))

                // ------------------------------------------------ requirements
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .glass(shape = RoundedCornerShape(20.dp), shadowElevation = 0.dp)
                        .padding(16.dp),
                ) {
                    Column {
                        Text("BEFORE YOU INSTALL", style = AppText.Eyebrow)
                        Spacer(Modifier.height(8.dp))
                        Text(GameFiles.label(game.android.requiresGameFiles), style = AppText.Body.copy(color = Ink.TextPrimary))
                        if (!game.android.minAndroid.isNullOrBlank() || game.android.abis.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                listOfNotNull(
                                    game.android.minAndroid?.let { "Android $it+" },
                                    game.android.abis.takeIf { it.isNotEmpty() }?.joinToString(", "),
                                ).joinToString(" · "),
                                style = AppText.Caption.copy(fontSize = 12.sp),
                                color = Ink.TextTertiary,
                            )
                        }
                        game.android.notes?.takeIf { it.isNotBlank() }?.let {
                            Spacer(Modifier.height(6.dp))
                            Text(it, style = AppText.Caption.copy(fontSize = 12.sp), color = Ink.TextTertiary)
                        }
                    }
                }

                // ----------------------------------------------------- downloads
                if (game.downloads.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    Text("GET IT", style = AppText.Eyebrow)
                    Spacer(Modifier.height(10.dp))
                    game.downloads.forEach { download ->
                        Row(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .glass(shape = RoundedCornerShape(18.dp), shadowElevation = 0.dp)
                                .clickable { open(download.url) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(download.label, style = AppText.CardTitle.copy(fontSize = 15.sp))
                                val meta = listOfNotNull(
                                    download.version?.let { "v$it" },
                                    download.abi,
                                    download.sizeMb?.let { "${it.toInt()} MB" },
                                ).joinToString(" · ")
                                if (meta.isNotBlank()) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(meta, style = AppText.Caption.copy(fontSize = 12.sp), color = Ink.TextTertiary)
                                }
                            }
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = Ink.Accent,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }

                // --------------------------------------------------- screenshots
                if (game.artwork.screenshots.isNotEmpty()) {
                    Spacer(Modifier.height(18.dp))
                    Text("SCREENS", style = AppText.Eyebrow)
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(game.artwork.screenshots) { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(width = 220.dp, height = 124.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                            )
                        }
                    }
                }

                // ------------------------------------------------------- links
                Spacer(Modifier.height(24.dp))
                Text("SOURCE", style = AppText.Eyebrow)
                Spacer(Modifier.height(10.dp))
                LinkRow("Repository", game.links.repo) { open(game.links.repo) }
                LinkRow("Releases", game.links.releases ?: game.links.repo?.let { "$it/releases" }) {
                    open(game.links.releases ?: game.links.repo?.let { "$it/releases" })
                }
                game.links.website?.let { LinkRow("Website", it) { open(it) } }
                game.links.discord?.let { LinkRow("Discord", it) { open(it) } }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    game.license?.let { MetaText("License", it) }
                    game.lastUpdated?.let { MetaText("Updated", it) }
                    game.engine?.let { MetaText("Engine", it) }
                }

                Spacer(Modifier.height(30.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = if (game.downloads.isEmpty()) "Open source page" else "Download",
                        onClick = {
                            open(game.downloads.firstOrNull()?.url ?: game.links.releases ?: game.links.repo)
                        },
                        primary = true,
                        modifier = Modifier.weight(1f),
                    )
                    GlassButton(
                        text = if (isFavorite) "Saved" else "Save",
                        onClick = onToggleFavorite,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(60.dp))
            }
        }

        // ------------------------------------------------------- floating controls
        val heartScale = remember { Animatable(1f) }
        LaunchedEffect(isFavorite) {
            if (isFavorite) {
                heartScale.snapTo(1.35f)
                heartScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                )
            }
        }

        Row(
            modifier = Modifier
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconCircleButton(Icons.Rounded.ArrowBack, onClick = onBack)
            IconCircleButton(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                onClick = onToggleFavorite,
                tint = if (isFavorite) Ink.AccentPink else Ink.TextPrimary,
                modifier = Modifier.graphicsLayer {
                    scaleX = heartScale.value
                    scaleY = heartScale.value
                },
            )
        }
    }
}

@Composable
private fun LinkRow(label: String, value: String?, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(0.7.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .clickable(enabled = value != null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = AppText.Caption.copy(fontSize = 14.sp), color = Ink.TextSecondary)
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink.TextTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MetaText(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = AppText.Eyebrow.copy(letterSpacing = 1.sp))
        Spacer(Modifier.height(3.dp))
        Text(value, style = AppText.Caption.copy(fontSize = 13.sp), color = Ink.TextPrimary)
    }
}
