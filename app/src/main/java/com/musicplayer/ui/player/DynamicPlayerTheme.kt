package com.musicplayer.ui.player

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracted color set from album artwork via AndroidX Palette.
 */
@Immutable
data class ArtworkColors(
    val dominant: Color = Color.Unspecified,
    val vibrant: Color = Color.Unspecified,
    val darkMuted: Color = Color.Unspecified,
    val muted: Color = Color.Unspecified,
    val lightVibrant: Color = Color.Unspecified,
    val onAccent: Color = Color.White,
    val isDefault: Boolean = true
)

/**
 * Derives a [ColorScheme] from [ArtworkColors], falling back to the
 * current [MaterialTheme.colorScheme] when no artwork colours are available.
 */
@Composable
fun artworkColorScheme(artworkColors: ArtworkColors, isDark: Boolean = true): ColorScheme {
    val fallback = MaterialTheme.colorScheme

    if (artworkColors.isDefault) return fallback

    val primary = artworkColors.vibrant.ifUnspecified(artworkColors.dominant)
    
    return if (isDark) {
        val surface = artworkColors.darkMuted.ifUnspecified(artworkColors.dominant.darken(0.7f))
        val onSurface = if (surface.luminance() > 0.5f) Color.Black else Color.White
        val onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White
        val surfaceVariant = artworkColors.muted.ifUnspecified(surface.lighten(0.15f))

        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.darken(0.3f),
            onPrimaryContainer = onPrimary,
            secondary = artworkColors.muted.ifUnspecified(primary),
            onSecondary = onPrimary,
            tertiary = artworkColors.lightVibrant.ifUnspecified(primary.lighten(0.2f)),
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurface.copy(alpha = 0.7f),
            inverseSurface = onSurface,
            inverseOnSurface = surface
        )
    } else {
        val surface = artworkColors.lightVibrant.ifUnspecified(artworkColors.dominant.lighten(0.9f))
        val onSurface = if (surface.luminance() > 0.5f) Color.Black else Color.White
        val onPrimary = if (primary.luminance() > 0.5f) Color.Black else Color.White
        val surfaceVariant = artworkColors.muted.ifUnspecified(surface.darken(0.1f))

        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.lighten(0.7f),
            onPrimaryContainer = Color.Black,
            secondary = artworkColors.muted.ifUnspecified(primary),
            onSecondary = onPrimary,
            tertiary = artworkColors.darkMuted.ifUnspecified(primary.darken(0.2f)),
            background = surface,
            onBackground = onSurface,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurface.copy(alpha = 0.7f),
            inverseSurface = Color.Black,
            inverseOnSurface = Color.White
        )
    }
}

/**
 * Wraps [artworkColorScheme] with smooth cross-fade animation on each color slot.
 */
@Composable
fun animatedArtworkColorScheme(artworkColors: ArtworkColors, isDark: Boolean = true): ColorScheme {
    val base = artworkColorScheme(artworkColors, isDark)
    val duration = 800
    return base.copy(
        primary = animateColorAsState(base.primary, tween(duration), label = "primary").value,
        onPrimary = animateColorAsState(base.onPrimary, tween(duration), label = "onPrimary").value,
        primaryContainer = animateColorAsState(base.primaryContainer, tween(duration), label = "primaryContainer").value,
        secondary = animateColorAsState(base.secondary, tween(duration), label = "secondary").value,
        onSecondary = animateColorAsState(base.onSecondary, tween(duration), label = "onSecondary").value,
        tertiary = animateColorAsState(base.tertiary, tween(duration), label = "tertiary").value,
        background = animateColorAsState(base.background, tween(duration), label = "background").value,
        onBackground = animateColorAsState(base.onBackground, tween(duration), label = "onBackground").value,
        surface = animateColorAsState(base.surface, tween(duration), label = "surface").value,
        onSurface = animateColorAsState(base.onSurface, tween(duration), label = "onSurface").value,
        surfaceVariant = animateColorAsState(base.surfaceVariant, tween(duration), label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(base.onSurfaceVariant, tween(duration), label = "onSurfaceVariant").value,
        inverseSurface = animateColorAsState(base.inverseSurface, tween(duration), label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(base.inverseOnSurface, tween(duration), label = "inverseOnSurface").value
    )
}

/**
 * Extracts [ArtworkColors] from the given image URI using AndroidX Palette.
 * Result is remembered and re-computed only when [artworkUri] changes.
 */
@Composable
fun rememberArtworkColors(artworkUri: String?): ArtworkColors {
    val context = LocalContext.current
    var colors by remember { mutableStateOf(ArtworkColors()) }

    LaunchedEffect(artworkUri) {
        if (artworkUri.isNullOrBlank()) {
            colors = ArtworkColors()
            return@LaunchedEffect
        }
        colors = withContext(Dispatchers.IO) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUri)
                    .allowHardware(false) // Palette needs a software bitmap
                    .size(128) // Small is fine for colour extraction
                    .build()
                val result = loader.execute(request)
                val bitmap = (result as? SuccessResult)?.drawable
                    ?.let { (it as? BitmapDrawable)?.bitmap }
                    ?: return@withContext ArtworkColors()

                val palette = Palette.from(bitmap).generate()
                ArtworkColors(
                    dominant = palette.dominantSwatch?.rgb?.let { Color(it) } ?: Color.Unspecified,
                    vibrant = palette.vibrantSwatch?.rgb?.let { Color(it) } ?: Color.Unspecified,
                    darkMuted = palette.darkMutedSwatch?.rgb?.let { Color(it) } ?: Color.Unspecified,
                    muted = palette.mutedSwatch?.rgb?.let { Color(it) } ?: Color.Unspecified,
                    lightVibrant = palette.lightVibrantSwatch?.rgb?.let { Color(it) } ?: Color.Unspecified,
                    onAccent = palette.vibrantSwatch?.titleTextColor?.let { Color(it) } ?: Color.White,
                    isDefault = false
                )
            } catch (_: Exception) {
                ArtworkColors()
            }
        }
    }
    return colors
}

// ── helper extensions ────────────────────────────────────────────────────────

private fun Color.ifUnspecified(fallback: Color): Color =
    if (this == Color.Unspecified) fallback else this

private fun Color.darken(factor: Float): Color {
    return Color(
        red = red * (1f - factor),
        green = green * (1f - factor),
        blue = blue * (1f - factor),
        alpha = alpha
    )
}

private fun Color.lighten(factor: Float): Color {
    return Color(
        red = (red + (1f - red) * factor).coerceAtMost(1f),
        green = (green + (1f - green) * factor).coerceAtMost(1f),
        blue = (blue + (1f - blue) * factor).coerceAtMost(1f),
        alpha = alpha
    )
}
