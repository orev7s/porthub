package com.porthub.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.porthub.app.ui.theme.Ink
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The "liquid glass" surface: a translucent vertical gradient, a bright hairline on top
 * of the edge, a soft drop shadow. Real backdrop blur is impossible in Compose without
 * hijacking the window, so the illusion comes from layered light instead.
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(24.dp),
    topAlpha: Float = 0.10f,
    bottomAlpha: Float = 0.035f,
    borderTopAlpha: Float = 0.28f,
    borderBottomAlpha: Float = 0.06f,
    shadowElevation: Dp = 18.dp,
): Modifier = this
    .shadow(
        elevation = shadowElevation,
        shape = shape,
        clip = false,
        ambientColor = Color.Black.copy(alpha = 0.55f),
        spotColor = Color.Black.copy(alpha = 0.65f),
    )
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = topAlpha), Color.White.copy(alpha = bottomAlpha)),
        ),
    )
    .border(
        BorderStroke(
            0.8.dp,
            Brush.verticalGradient(
                listOf(Color.White.copy(alpha = borderTopAlpha), Color.White.copy(alpha = borderBottomAlpha)),
            ),
        ),
        shape,
    )

/** Slight press-scale, iOS style. Nothing bounces twice. */
@Composable
fun Modifier.pressScale(
    pressedScale: Float = 0.97f,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    return this.graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * Entry animation used across the app: content fades up from 26dp below, slightly late
 * per index so a screen assembles itself instead of popping in one frame.
 */
@Composable
fun StaggeredAppear(
    index: Int = 0,
    durationMs: Int = 420,
    stepMs: Int = 55,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val rise = remember { Animatable(26f) }

    LaunchedEffect(Unit) {
        delay((index * stepMs).toLong())
        launch { alpha.animateTo(1f, tween(durationMs, easing = FastOutSlowInEasing)) }
        rise.animateTo(0f, tween(durationMs, easing = FastOutSlowInEasing))
    }

    Box(
        Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationY = rise.value
        },
    ) {
        content()
    }
}

/** Slow breathing glow for skeleton placeholders. */
@Composable
fun Modifier.pulse(): Modifier {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    return this.graphicsLayer { this.alpha = alpha }
}

/** Ambient colour bloom behind everything. Two or three soft lights, never more. */
@Composable
fun BloomBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                0f to Ink.BackgroundLift,
                0.45f to Ink.Background,
                1f to Color(0xFF030306),
            ),
        )

        fun bloom(center: Offset, radius: Float, color: Color) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color, Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        bloom(Offset(size.width * 0.12f, size.height * 0.02f), size.minDimension * 0.85f, Ink.Accent.copy(alpha = 0.20f))
        bloom(Offset(size.width * 0.95f, size.height * 0.16f), size.minDimension * 0.70f, Ink.AccentPink.copy(alpha = 0.12f))
        bloom(Offset(size.width * 0.55f, size.height * 0.92f), size.minDimension * 0.75f, Ink.AccentTeal.copy(alpha = 0.07f))
    }
}
