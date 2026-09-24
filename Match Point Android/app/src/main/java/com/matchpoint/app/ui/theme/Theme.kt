package com.matchpoint.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import io.github.iamcalledrob.smoothRoundedCornerShape.SmoothRoundedCornerShape

/**
 * iOS-style continuous ("squircle") corners — a drop-in replacement for
 * `RoundedCornerShape`. 0.6 smoothing roughly matches the look of SwiftUI's
 * `.continuous` corner style used throughout the iOS app's DesignSystem.
 *
 * Wrapped rather than returned directly: `androidx.graphics.shapes.RoundedPolygon`
 * (which the library builds its outline from) throws `IllegalArgumentException:
 * Can't get the direction of a 0-length vector` if it's ever asked to outline a
 * zero-width or zero-height box — which transient layout passes do produce, e.g.
 * a ModalBottomSheet's very first frame while it's still animating in. Falling
 * back to a plain rectangle for a degenerate size sidesteps the crash; nothing
 * visible is lost since there's no area to draw a corner into anyway.
 */
fun SmoothCorner(radius: Dp): Shape = SafeSmoothCornerShape(SmoothRoundedCornerShape(smoothing = 0.6f, radius = radius))
fun SmoothCorner(percent: Int): Shape = SafeSmoothCornerShape(SmoothRoundedCornerShape(smoothing = 0.6f, percent = percent))

private class SafeSmoothCornerShape(private val delegate: Shape) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: androidx.compose.ui.unit.Density
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(Rect(0f, 0f, size.width, size.height))
        }
        return delegate.createOutline(size, layoutDirection, density)
    }
}

/** The active court palette, provided by [MatchPointTheme]. Defaults to Roland Garros
 * for any preview/context that renders outside the app's theme root. */
val LocalCourtTheme = compositionLocalOf { CourtTheme.ROLAND_GARROS }

/**
 * Dark, frosted/translucent-card look, switchable between Roland Garros
 * (burnt clay) and Wimbledon (deep green) — ported 1:1 from the iOS app's
 * Theme.swift. Every color reads the current [LocalCourtTheme] so a theme
 * switch propagates to every composable already reading `Theme.xxx`, no
 * extra wiring needed per screen.
 */
object Theme {
    val backgroundTop: Color @Composable get() = LocalCourtTheme.current.backgroundTop
    val backgroundBottom: Color @Composable get() = LocalCourtTheme.current.backgroundBottom

    val accent: Color @Composable get() = LocalCourtTheme.current.accent
    val accentText: Color @Composable get() = LocalCourtTheme.current.accentText

    val cardFill: Color @Composable get() = LocalCourtTheme.current.cardFill
    val cardBorder: Color @Composable get() = LocalCourtTheme.current.cardBorder

    val textPrimary: Color @Composable get() = LocalCourtTheme.current.textPrimary
    val textSecondary: Color @Composable get() = LocalCourtTheme.current.textSecondary

    val danger: Color @Composable get() = LocalCourtTheme.current.danger

    // Light fill for text fields — dark-mode default reads poorly against
    // the court background, so inputs get an off-white surface instead.
    val fieldFill: Color @Composable get() = LocalCourtTheme.current.fieldFill
    val fieldText: Color @Composable get() = LocalCourtTheme.current.fieldText

    val backgroundGradient: Brush
        @Composable get() = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom))

    val cornerRadius = 20.dp
    val formTileHeight = 64.dp
}

fun themeTitle(size: androidx.compose.ui.unit.TextUnit) = TextStyle(
    fontFamily = GoogleSans,
    fontWeight = FontWeight.Bold,
    fontSize = size
)

fun themeScore(size: androidx.compose.ui.unit.TextUnit) = TextStyle(
    fontFamily = GoogleSans,
    fontWeight = FontWeight.Black,
    fontSize = size
)

@Composable
fun MatchPointTheme(preferences: com.matchpoint.app.data.AppPreferences, content: @Composable () -> Unit) {
    val courtTheme by preferences.courtTheme.collectAsState()
    CompositionLocalProvider(LocalCourtTheme provides courtTheme) {
        val appDarkColors = darkColorScheme(
            primary = Theme.accent,
            onPrimary = Theme.accentText,
            secondary = Theme.accent,
            background = Theme.backgroundTop,
            onBackground = Theme.textPrimary,
            surface = Theme.backgroundBottom,
            onSurface = Theme.textPrimary,
            error = Theme.danger
        )
        MaterialTheme(
            colorScheme = appDarkColors,
            typography = MaterialTheme.typography.copy(
                bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = GoogleSans),
                bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = GoogleSans),
                bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = GoogleSans),
                titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
                titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = GoogleSans, fontWeight = FontWeight.Bold),
                labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold)
            ),
            content = content
        )
    }
}

/** Full-bleed court gradient background, matching `themedScreenBackground()` on iOS. */
@Composable
fun ThemedScreenBackground(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Theme.backgroundGradient)
    ) {
        content()
    }
}
