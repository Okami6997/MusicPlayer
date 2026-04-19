package com.musicplayer.ui.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A squiggly (wavy) seek bar inspired by Android 14+ media controls.
 *
 * - The elapsed portion is drawn as an animated sine wave.
 * - The remaining portion is a straight line.
 * - The wave animates while playing and flattens when paused.
 * - A small circular thumb sits at the progress point.
 */
@Composable
fun SquigglySeekBar(
    progress: Float,
    onSeek: (Float) -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbColor: Color = MaterialTheme.colorScheme.primary,
    waveHeight: Dp = 6.dp,
    strokeWidth: Dp = 4.dp,
    thumbRadius: Dp = 7.dp,
    barHeight: Dp = 48.dp
) {
    // ── animated phase (scrolls the wave while playing) ──────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val rawPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    // ── animate amplitude: full when playing, zero when paused ───────────
    val targetAmplitude = if (isPlaying) 1f else 0f
    val amplitude by animateFloatAsState(
        targetValue = targetAmplitude,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "amplitude"
    )

    // Use raw phase only while there is amplitude, avoid visual jitter at 0
    val phase = if (amplitude > 0.001f) rawPhase else 0f

    val density = LocalDensity.current
    val waveHeightPx = with(density) { waveHeight.toPx() }
    val strokeWidthPx = with(density) { strokeWidth.toPx() }
    val thumbRadiusPx = with(density) { thumbRadius.toPx() }

    // Track user dragging so we can show immediate feedback
    var isDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress) }
    val displayProgress = if (isDragging) dragProgress else progress

    Box(modifier = modifier.height(barHeight)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragProgress =
                                (offset.x / size.width).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            onSeek(dragProgress)
                            isDragging = false
                        },
                        onDragCancel = { isDragging = false },
                        onHorizontalDrag = { _, dragAmount ->
                            dragProgress =
                                (dragProgress + dragAmount / size.width).coerceIn(0f, 1f)
                        }
                    )
                }
        ) {
            val centerY = size.height / 2f
            val width = size.width
            val progressX = displayProgress * width

            // ── inactive (remaining) track: straight line ────────────────
            drawLine(
                color = inactiveColor,
                start = Offset(progressX, centerY),
                end = Offset(width, centerY),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )

            // ── active (elapsed) track: squiggly sine wave ──────────────
            if (progressX > 0f) {
                drawSquigglyLine(
                    color = activeColor,
                    startX = 0f,
                    endX = progressX,
                    centerY = centerY,
                    amplitude = waveHeightPx * amplitude,
                    wavelength = strokeWidthPx * 12f,
                    phase = phase,
                    strokeWidth = strokeWidthPx
                )
            }

            // ── thumb ────────────────────────────────────────────────────
            val thumbY = if (amplitude > 0.001f && progressX > 0f) {
                // Place thumb on the wave
                val waveY = sin(
                    (progressX / (strokeWidthPx * 12f)) * 2f * PI.toFloat() + phase
                ) * waveHeightPx * amplitude
                centerY + waveY
            } else {
                centerY
            }
            drawCircle(
                color = thumbColor,
                radius = thumbRadiusPx,
                center = Offset(progressX, thumbY)
            )
        }
    }
}

/**
 * Draws a sine-wave path from [startX] to [endX] at [centerY].
 */
private fun DrawScope.drawSquigglyLine(
    color: Color,
    startX: Float,
    endX: Float,
    centerY: Float,
    amplitude: Float,
    wavelength: Float,
    phase: Float,
    strokeWidth: Float
) {
    if (endX - startX < 1f) return

    val path = Path()
    val step = 2f // px per segment — smooth enough, cheap enough
    var x = startX
    val firstY = centerY + sin((x / wavelength) * 2f * PI.toFloat() + phase) * amplitude
    path.moveTo(x, firstY)

    while (x < endX) {
        x = (x + step).coerceAtMost(endX)
        val y = centerY + sin((x / wavelength) * 2f * PI.toFloat() + phase) * amplitude
        path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}
