package com.matchpoint.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.border

/** Frosted glass card — translucent white fill + hairline border, ported from GlassCard.swift. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(SmoothCorner(Theme.cornerRadius))
            .background(Theme.cardFill)
            .border(BorderStroke(1.dp, Theme.cardBorder), SmoothCorner(Theme.cornerRadius))
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        content()
    }
}
