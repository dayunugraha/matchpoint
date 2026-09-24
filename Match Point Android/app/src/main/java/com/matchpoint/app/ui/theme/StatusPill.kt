package com.matchpoint.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Small rounded status badge — ported from StatusPill.swift. */
@Composable
fun StatusPill(text: String, filled: Boolean = false, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = if (filled) Theme.accentText else Theme.textSecondary,
        modifier = modifier
            .clip(CircleShape)
            .background(if (filled) Theme.accent else Color.Transparent)
            .border(BorderStroke(1.dp, if (filled) Color.Transparent else Theme.cardBorder), CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
