package com.porthub.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.porthub.app.data.Game
import com.porthub.app.data.PortStatus
import com.porthub.app.data.PortType
import com.porthub.app.ui.theme.AppText
import com.porthub.app.ui.theme.Ink

/* ---------------------------------------------------------------- colour utils */

private val fallbackAccents = listOf(
    Color(0xFF6C7BFF), Color(0xFFFF6B9A), Color(0xFF4ADEDE),
    Color(0xFFFFB86B), Color(0xFFB08CFF), Color(0xFF57D9A3),
)

fun accentOf(game: Game): Color {
    game.artwork.accent?.let { hex ->
        runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull()?.let { return it }
    }
    val hash = game.id.hashCode()
    val index = (if (hash < 0) -hash else hash) % fallbackAccents.size
    return fallbackAccents[index]
}

fun statusColor(raw: String): Color = when (raw) {
    PortStatus.STABLE -> Ink.Positive
    PortStatus.BETA -> Ink.Accent
    PortStatus.ALPHA, PortStatus.WIP -> Ink.Warning
    else -> Ink.TextTertiary
}

/* --------------------------------------------------------------------- cover */

@Composable
fun CoverArt(
    game: Game,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    showMonogram: Boolean = true,
) {
    val accent = remember(game.id, game.artwork.accent) { accentOf(game) }
    val context = LocalContext.current
    val imageUrl = game.coverUrl

    // Placeholder breathes instead of sitting there dead.
    val transition = rememberInfiniteTransition(label = "placeholder")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.55f),
                        accent.copy(alpha = 0.18f),
                        Color(0xFF0A0A12),
                    ),
                ),
            ),
    ) {
        // Painted first so it shows through whenever the remote image fails or is missing.
        if (showMonogram) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accent.copy(alpha = glow * 0.45f), Color.Transparent),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = game.monogram,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        letterSpacing = (-1).sp,
                        color = Color.White.copy(alpha = 0.92f),
                    ),
                )
            }
        }

        if (imageUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(260)
                    .build(),
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // bottom scrim so overlay text always reads
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.55f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.45f),
                    ),
                ),
        )
    }
}

/* ---------------------------------------------------------------------- chip */

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    tint: Color = Ink.TextSecondary,
    filled: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (filled) tint.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f))
            .border(0.7.dp, Color.White.copy(alpha = 0.10f), CircleShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    ) {
        Text(text, style = AppText.Caption.copy(fontSize = 12.sp), color = tint)
    }
}

@Composable
fun StatusDot(status: String) {
    Box(
        Modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(statusColor(status)),
    )
}

/* ------------------------------------------------------------------ sections */

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = AppText.ScreenTitle)
        if (action != null) {
            Text(
                action,
                style = AppText.Caption.copy(color = Ink.Accent),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = onAction != null) { onAction?.invoke() }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------- cards */

@Composable
fun GameCard(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 148.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(if (cardWidth != Dp.Unspecified) Modifier.width(cardWidth) else Modifier)
            .pressScale(interactionSource = interaction)
            .clip(RoundedCornerShape(22.dp))
            .clickable(interactionSource = interaction, indication = null) { onClick() },
    ) {
        Box {
            CoverArt(game = game, modifier = Modifier.fillMaxWidth().aspectRatio(0.74f))
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.42f))
                    .border(0.7.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                StatusDot(game.status)
                Text(
                    PortType.label(game.portType),
                    style = AppText.Caption.copy(fontSize = 11.sp),
                    color = Ink.TextPrimary,
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(game.title, style = AppText.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        Text(
            listOfNotNull(game.originalTitle, game.year?.toString()).joinToString(" · ")
                .ifBlank { game.engine ?: "Android port" },
            style = AppText.Caption.copy(fontSize = 12.sp),
            color = Ink.TextTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GameRow(
    game: Game,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressedScale = 0.985f, interactionSource = interaction)
            .clip(RoundedCornerShape(22.dp))
            .glass(shape = RoundedCornerShape(22.dp), shadowElevation = 0.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoverArt(
            game = game,
            modifier = Modifier.size(width = 58.dp, height = 74.dp),
            shape = RoundedCornerShape(14.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(game.title, style = AppText.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusDot(game.status)
                Text(
                    "${PortType.label(game.portType)} · ${PortStatus.label(game.status)}",
                    style = AppText.Caption.copy(fontSize = 12.sp),
                    color = Ink.TextTertiary,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                game.tags.take(3).joinToString(" · ").ifBlank { game.engine ?: "" },
                style = AppText.Caption.copy(fontSize = 11.sp),
                color = Ink.TextTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            Icons.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink.TextTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/* ------------------------------------------------------------------ buttons */

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interactionSource = interaction)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (primary) {
                    Modifier.background(Brush.verticalGradient(listOf(Ink.Accent, Ink.Accent.copy(alpha = 0.78f))))
                } else {
                    Modifier.glass(shape = RoundedCornerShape(16.dp), shadowElevation = 0.dp)
                },
            )
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(PaddingValues(horizontal = 18.dp, vertical = 13.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppText.CardTitle.copy(fontSize = 14.sp),
            color = if (primary) Color(0xFF0A0A12) else Ink.TextPrimary,
        )
    }
}

@Composable
fun IconCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Ink.TextPrimary,
    size: Dp = 40.dp,
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(size)
            .pressScale(pressedScale = 0.92f, interactionSource = interaction)
            .clip(CircleShape)
            .glass(shape = CircleShape, shadowElevation = 0.dp)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}
