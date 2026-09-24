package com.matchpoint.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Filled accent CTA — ported from PrimaryButtonStyle. */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = themeTitle(16.sp),
        textAlign = TextAlign.Center,
        color = Theme.accentText,
        modifier = modifier
            .fillMaxWidth()
            .clip(SmoothCorner(14.dp))
            .background(if (enabled) Theme.accent else Theme.accent.copy(alpha = 0.35f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp)
    )
}

/** Outlined, low-emphasis button — ported from GhostButtonStyle. */
@Composable
fun GhostButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = themeTitle(15.sp),
        textAlign = TextAlign.Center,
        color = if (enabled) Theme.textPrimary else Theme.textSecondary,
        modifier = modifier
            .fillMaxWidth()
            .clip(SmoothCorner(14.dp))
            .border(BorderStroke(1.5.dp, Theme.cardBorder), SmoothCorner(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp)
    )
}

/** Outlined destructive button — ported from DangerButtonStyle. */
@Composable
fun DangerButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = text,
        style = themeTitle(15.sp),
        textAlign = TextAlign.Center,
        color = Theme.danger,
        modifier = modifier
            .fillMaxWidth()
            .clip(SmoothCorner(14.dp))
            .border(BorderStroke(1.5.dp, Theme.danger.copy(alpha = 0.6f)), SmoothCorner(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 13.dp)
    )
}

/**
 * Custom ON/OFF toggle — a sliding, label-carrying pill inside a track, in the app's own
 * accent/clay palette rather than the stock Material3 switch. Mechanic based on
 * https://medium.com/@narendersaini32/how-to-create-custom-toggle-button-in-react-26128c986cdc
 * (rectangular track, an absolutely-positioned thumb that slides between two anchors with a
 * short transition) — ported to Compose with an animated offset instead of CSS `left`.
 */
@Composable
fun ThemedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackWidth = 64.dp
    val trackHeight = 32.dp
    val thumbWidth = 34.dp
    val edgePadding = 3.dp
    val thumbHeight = trackHeight - edgePadding * 2
    val maxOffset = trackWidth - thumbWidth - edgePadding * 2

    val offset by animateDpAsState(targetValue = if (checked) maxOffset else 0.dp, animationSpec = tween(220), label = "switchThumbOffset")
    val thumbColor by animateColorAsState(targetValue = if (checked) Theme.accent else Theme.textSecondary.copy(alpha = 0.25f), label = "switchThumbColor")

    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(SmoothCorner(trackHeight / 2))
            .background(Theme.cardFill)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { onCheckedChange(!checked) }
            )
            .padding(edgePadding)
    ) {
        Box(
            modifier = Modifier
                .offset(x = offset)
                .width(thumbWidth)
                .height(thumbHeight)
                .clip(SmoothCorner(thumbHeight / 2))
                .background(thumbColor)
        )
    }
}

/** Plain destructive text link — ported from DangerPlainButtonStyle. */
@Composable
fun DangerTextButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = text,
        fontFamily = GoogleSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        color = Theme.danger,
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp)
    )
}

/** Tab-pill segmented picker — a soft rounded track with a subtle highlighted pill behind
 * the selected option, matching Home's Standings/Matches scoreboard switch. Used anywhere a
 * lighter-weight alternative to the bolder accent-filled segmented style is wanted. */
@Composable
fun <T> SegmentedTabPicker(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(SmoothCorner(10.dp))
            .background(Theme.cardFill)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Text(
                label(option),
                fontFamily = GoogleSans,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isSelected) Theme.textPrimary else Theme.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(SmoothCorner(8.dp))
                    .background(if (isSelected) Theme.textPrimary.copy(alpha = 0.12f) else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(vertical = 7.dp)
            )
        }
    }
}
