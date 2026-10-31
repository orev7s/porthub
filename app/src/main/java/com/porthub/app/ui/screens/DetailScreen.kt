package com.porthub.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.porthub.app.data.Game
import com.porthub.app.data.PortStatus
import com.porthub.app.data.PortType
import com.porthub.app.data.Setup
import com.porthub.app.ui.components.Chip
import com.porthub.app.ui.components.CoverArt
import com.porthub.app.ui.components.GlassButton
import com.porthub.app.ui.components.IconCircleButton
import com.porthub.app.ui.components.glass
import com.porthub.app.ui.components.statusColor
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink
import com.porthub.app.ui.theme.LocalAppLayout

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    game: Game,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val layout = LocalAppLayout.current
    val context = LocalContext.current
    val scroll = rememberScrollState()
    BackHandler(onBack = onBack)

    fun open(url: String?) {
        if (url.isNullOrBlank()) return
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    // Slides in on mount: one screen on stage at a time, so nothing can overlap.
    val enter = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        enter.animateTo(0f, tween(320, easing = FastOutSlowInEasing))
    }

    val heroLift by animateFloatAsState(
        targetValue = -(scroll.value * 0.28f),
        animationSpec = tween(0),
        label = "heroLift",
    )
    val heroAlpha by animateFloatAsState(
        targetValue = (1f - scroll.value / 700f).coerceIn(0.2f, 1f),
        animationSpec = tween(0),
        label = "heroAlpha",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Ink.Background)
            .graphicsLayer {
                translationX = enter.value
                alpha = 1f - enter.value * 0.55f
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
        ) {
            // ------------------------------------------------------------ hero
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(layout.heroHeight),
            ) {
                val heroUrl = game.heroUrl
                if (!heroUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context).data(heroUrl).crossfade(300).build(),
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = heroLift
                                alpha = heroAlpha
                                scaleX = 1.05f
                                scaleY = 1.05f
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
                                0f to Color.Black.copy(alpha = 0.40f),
                                0.55f to Color.Transparent,
                                1f to Ink.Background,
                            ),
                        ),
                )
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = layout.gutter),
            ) {
                HeaderBlock(game = game)

                if (game.description.isNotBlank()) {
                    Spacer(Modifier.height(18.dp))
                    Text(game.description, style = AppText.Body)
                }

                Spacer(Modifier.height(18.dp))
                SetupCard(setup = game.setup, onOpen = { open(it) })

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
                                Text(
                                    download.label,
                                    style = AppText.CardTitle.copy(fontSize = 15.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
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
                            Spacer(Modifier.width(10.dp))
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Ink.Accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }

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

                Spacer(Modifier.height(24.dp))
                Text("SOURCE", style = AppText.Eyebrow)
                Spacer(Modifier.height(10.dp))
                LinkRow("Repository", game.links.repo) { open(game.links.repo) }
                LinkRow("Releases", game.links.releases) { open(game.links.releases) }
                game.links.website?.let { LinkRow("Website", it) { open(it) } }
                game.links.discord?.let { LinkRow("Discord", it) { open(it) } }

                Spacer(Modifier.height(18.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    game.license?.let { MetaText("License", it) }
                    game.lastUpdated?.let { MetaText("Updated", it) }
                    game.engine?.let { MetaText("Engine", it) }
                }

                Spacer(Modifier.height(26.dp))

                val apkUrl = game.downloads.firstOrNull()?.url
                val target = apkUrl ?: game.links.releases ?: game.links.repo
                val label = when {
                    apkUrl != null -> "Download APK"
                    game.links.releases != null -> "Open releases"
                    game.links.repo != null -> "Open project"
                    else -> "No verified link"
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GlassButton(
                        text = label,
                        onClick = { open(target) },
                        primary = apkUrl != null,
                        enabled = target != null,
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

        // ------------------------------------------------------ floating controls
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
                .padding(horizontal = layout.gutter, vertical = 8.dp),
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

/**
 * Cover + title side by side with a fixed cover size. The text column takes the
 * remaining width and wraps, so long titles can never land on top of the artwork.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeaderBlock(game: Game) {
    val layout = LocalAppLayout.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        CoverArt(
            game = game,
            modifier = Modifier.size(width = layout.coverWidth, height = layout.coverHeight),
            shape = RoundedCornerShape(16.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                game.title,
                style = AppText.ScreenTitle.copy(fontSize = if (layout.isWide) 26.sp else 21.sp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = listOfNotNull(game.originalTitle, game.year?.toString(), game.developer)
                    .joinToString(" · ")
                    .ifBlank { game.engine ?: "Android port" },
                style = AppText.Caption.copy(fontSize = 12.5.sp),
                color = Ink.TextTertiary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(9.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Chip(PortType.label(game.portType), tint = Ink.Accent, filled = true)
                Chip(PortStatus.label(game.status), tint = statusColor(game.status), filled = true)
                if (game.android.abis.isNotEmpty()) {
                    Chip(game.android.abis.first(), tint = Ink.TextSecondary)
                }
            }
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
        Icon(Icons.Rounded.KeyboardArrowRight, contentDescription = null, tint = Ink.TextTertiary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MetaText(label: String, value: String) {
    Column {
        Text(label.uppercase(), style = AppText.Eyebrow.copy(letterSpacing = 1.sp))
        Spacer(Modifier.height(3.dp))
        Text(value, style = AppText.Caption.copy(fontSize = 13.sp), color = Ink.TextPrimary, maxLines = 1)
    }
}

/* --------------------------------------------------------------- setup card */

private val difficultyColor = mapOf(
    "easy" to Ink.Positive,
    "medium" to Ink.Accent,
    "advanced" to Ink.Warning,
)

@Composable
private fun SetupCard(setup: Setup?, onOpen: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val chevron by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "chevron",
    )

    val headline = setup?.headline ?: "Check the project page before installing"
    val accent = difficultyColor[setup?.difficulty ?: "medium"] ?: Ink.Accent

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .glass(shape = RoundedCornerShape(22.dp), shadowElevation = 0.dp)
            .animateContentSize()
            .clickable { expanded = !expanded }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SETUP", style = AppText.Eyebrow)
                    if (setup != null) {
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(5.dp).clip(CircleShape).background(accent))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            buildString {
                                append(setup.difficulty.replaceFirstChar { it.uppercase() })
                                setup.minutes?.let { append(" · ~").append(it).append(" min") }
                            },
                            style = AppText.Caption.copy(fontSize = 11.sp),
                            color = accent,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    headline,
                    style = AppText.CardTitle.copy(fontSize = 15.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowRight,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = Ink.TextTertiary,
                modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = chevron },
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column {
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth().height(0.7.dp).background(Color.White.copy(alpha = 0.08f)))
                Spacer(Modifier.height(14.dp))

                setup?.steps?.forEachIndexed { index, step ->
                    Row(Modifier.padding(bottom = 10.dp)) {
                        Box(
                            Modifier
                                .size(19.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", style = AppText.Caption.copy(fontSize = 10.sp), color = Ink.TextSecondary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(step, style = AppText.Body.copy(fontSize = 13.5.sp))
                    }
                }

                setup?.engine?.let { engine ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GlassButton("Get ${engine.name}", onClick = { onOpen(engine.url) }, primary = true)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(engine.name, style = AppText.Caption.copy(fontSize = 12.sp), color = Ink.TextPrimary, maxLines = 1)
                            engine.note?.let {
                                Text(it, style = AppText.Caption.copy(fontSize = 11.sp), color = Ink.TextTertiary, maxLines = 2)
                            }
                        }
                    }
                }

                if (!setup?.buy.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("BUY THE ORIGINAL", style = AppText.Eyebrow)
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        setup.buy.forEach { link ->
                            GlassButton(link.store, onClick = { onOpen(link.url) })
                        }
                    }
                }

                if (setup?.confidence == "generic") {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Generated from this project's metadata — confirm the exact steps on its page.",
                        style = AppText.Caption.copy(fontSize = 11.sp),
                        color = Ink.TextTertiary,
                    )
                }
            }
        }
    }
}
