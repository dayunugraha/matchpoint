package com.matchpoint.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import com.matchpoint.app.ui.theme.ThemedSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchpoint.app.domain.MatchType
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.PrimaryButton
import com.matchpoint.app.ui.theme.Theme
import java.util.Calendar
import java.util.UUID

private val TileHeight = 52.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartSessionSheet(viewModel: HomeViewModel, onDismiss: () -> Unit, onCreated: () -> Unit = {}) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var draftSessionId by remember { mutableStateOf<UUID?>(null) }
    var confirmed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.startDraftSession { draftSessionId = it }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!confirmed) draftSessionId?.let { viewModel.cancelDraftSession(it) }
        }
    }

    var name by remember { mutableStateOf("") }
    var startTimeMillis by remember { mutableStateOf<Long?>(roundedUpToNext15Minutes()) }
    var showTimePicker by remember { mutableStateOf(false) }
    var durationHours by remember { mutableStateOf(2.0) }
    var matchType by remember { mutableStateOf(viewModel.preferences.defaultMatchType.value) }
    var formatIndex by remember { mutableIntStateOf(0) } // 0 = Quick(4), 1 = Medium(6), 2 = Custom
    var customGames by remember { mutableStateOf("8") }
    var customWinConditionIsGames by remember { mutableStateOf(true) }
    var deuceEnabled by remember { mutableStateOf(viewModel.preferences.defaultDeuceAdvantageEnabled.value) }

    // Quick Match / Medium Match map to a fixed game target; Custom isn't wired into the
    // scoring engine yet, so it stays at the prior default — matches the iOS app's stub.
    val effectiveFirstToGames = when (formatIndex) {
        0 -> 4
        1 -> 6
        else -> 4
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Theme.backgroundBottom) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cancel", fontFamily = GoogleSans, fontSize = 15.sp, color = Theme.accent, modifier = Modifier.clickable { onDismiss() })
                Text("Start New Mabar", fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Theme.textPrimary)
                Text("", modifier = Modifier.width(48.dp))
            }
            HorizontalDivider(color = Theme.cardBorder)

            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                SectionLabel("MABAR NAME")
                ThemedField(value = name, onValueChange = { name = it }, placeholder = "e.g. Rabu Pagi")

                SectionLabel("MABAR TIME")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlineTile(
                            label = startTimeMillis?.let { android.text.format.DateFormat.format("h:mm a", it).toString() } ?: "Set time",
                            onClick = { showTimePicker = true }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(TileHeight)
                            .clip(SmoothCorner(14.dp))
                            .background(Theme.cardFill)
                            .border(1.dp, Theme.cardBorder, SmoothCorner(14.dp))
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("−", color = Theme.textPrimary, fontSize = 18.sp, modifier = Modifier.clickable { if (durationHours > 0.5) durationHours -= 0.5 }.padding(8.dp))
                        Text(durationLabel(durationHours), color = Theme.textPrimary, fontFamily = GoogleSans, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("+", color = Theme.textPrimary, fontSize = 18.sp, modifier = Modifier.clickable { if (durationHours < 8.0) durationHours += 0.5 }.padding(8.dp))
                    }
                }

                SectionLabel("MODE")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    ModeTile("Doubles", matchType == MatchType.DOUBLES, Modifier.weight(1f)) { matchType = MatchType.DOUBLES }
                    ModeTile("Singles", matchType == MatchType.SINGLES, Modifier.weight(1f)) { matchType = MatchType.SINGLES }
                }

                SectionLabel("MATCH FORMAT")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    FormatTile("Quick Match", "First to 4", formatIndex == 0, Modifier.weight(1f)) { formatIndex = 0 }
                    FormatTile("Medium Match", "First to 6", formatIndex == 1, Modifier.weight(1f)) { formatIndex = 1 }
                    FormatTile("Custom", "Your own", formatIndex == 2, Modifier.weight(1f)) { formatIndex = 2 }
                }

                if (formatIndex == 2) {
                    SectionLabel("WIN CONDITION")
                    Column {
                        WinConditionRow("First to X Games", "e.g. First to 8 games", customWinConditionIsGames) { customWinConditionIsGames = true }
                        HorizontalDivider(color = Theme.cardBorder)
                        WinConditionRow("Standard Tennis", "Best of 3 Sets", !customWinConditionIsGames) { customWinConditionIsGames = false }
                    }
                    if (customWinConditionIsGames) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 10.dp)) {
                            Text("First to", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary, modifier = Modifier.padding(end = 8.dp))
                            Box(modifier = Modifier.width(64.dp)) {
                                ThemedField(
                                    value = customGames,
                                    onValueChange = { customGames = it.filter(Char::isDigit) },
                                    placeholder = "8",
                                    textAlign = TextAlign.Center
                                )
                            }
                            Text("games", fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.textPrimary, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                SectionLabel("")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TileHeight)
                        .clip(SmoothCorner(14.dp))
                        .background(Theme.cardFill)
                        .border(1.dp, Theme.cardBorder, SmoothCorner(14.dp))
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Deuce & Advantage", fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Theme.textPrimary)
                        Text("Win by 2 clear points at deuce", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary)
                    }
                    ThemedSwitch(
                        checked = deuceEnabled,
                        onCheckedChange = { deuceEnabled = it }
                    )
                }

                PrimaryButton(
                    text = "Create mabar now",
                    enabled = name.isNotBlank() && draftSessionId != null,
                    modifier = Modifier.padding(top = 20.dp),
                    onClick = {
                        val id = draftSessionId ?: return@PrimaryButton
                        val endTime = startTimeMillis?.let { it + (durationHours * 3600_000).toLong() }
                        confirmed = true
                        viewModel.finalizeSession(
                            sessionId = id,
                            name = name,
                            matchType = matchType,
                            firstToGames = effectiveFirstToGames,
                            deuceAdvantageEnabled = deuceEnabled,
                            startTime = startTimeMillis,
                            endTime = endTime,
                            onDone = { onCreated(); onDismiss() }
                        )
                    }
                )
            }
        }
    }

    if (showTimePicker) {
        QuarterHourTimePickerDialog(
            initialMillis = startTimeMillis ?: roundedUpToNext15Minutes(),
            onDismiss = { showTimePicker = false },
            onConfirm = { startTimeMillis = it }
        )
    }
}

/**
 * Rounds up to the next :00/:15/:30/:45 mark — seeds a fresh mabar's default Start
 * time so it lands exactly on one of the minute-interval picker's selectable values
 * instead of "now". Mirrors the iOS app's `Date.roundedUpToNext15Minutes()`: ceiling
 * division against absolute epoch millis, which lines up with local wall-clock
 * quarter-hours in every real-world time zone without needing calendar-aware math.
 */
private fun roundedUpToNext15Minutes(): Long {
    val interval = 15 * 60 * 1000L
    val now = System.currentTimeMillis()
    return ((now + interval - 1) / interval) * interval
}

/**
 * Android's `TimePicker`/`TimePickerDialog` has no `minuteInterval` equivalent to UIKit's
 * `UIDatePicker.minuteInterval` — so Mabar Time gets its own compact two-wheel dialog
 * (hour 0–23, minute restricted via `NumberPicker.displayedValues` to :00/:15/:30/:45)
 * instead of the free-scrolling native picker.
 */
@Composable
private fun QuarterHourTimePickerDialog(
    initialMillis: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val initialCal = remember(initialMillis) { Calendar.getInstance().apply { timeInMillis = initialMillis } }
    var hour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var quarterIndex by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE) / 15) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set time") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.NumberPicker(ctx).apply {
                            minValue = 0
                            maxValue = 23
                            displayedValues = (0..23).map { "%02d".format(it) }.toTypedArray()
                            value = hour
                            setOnValueChangedListener { _, _, newVal -> hour = newVal }
                        }
                    }
                )
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.widget.NumberPicker(ctx).apply {
                            minValue = 0
                            maxValue = 3
                            displayedValues = arrayOf("00", "15", "30", "45")
                            value = quarterIndex
                            setOnValueChangedListener { _, _, newVal -> quarterIndex = newVal }
                        }
                    }
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = {
                val cal = Calendar.getInstance().apply {
                    timeInMillis = initialMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, quarterIndex * 15)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onConfirm(cal.timeInMillis)
                onDismiss()
            }) { Text("OK", color = Theme.accent) }
        },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Theme.backgroundBottom,
        titleContentColor = Theme.textPrimary
    )
}

private fun durationLabel(hours: Double): String = when {
    hours == 1.0 -> "1 hour"
    hours == hours.toInt().toDouble() -> "${hours.toInt()} hours"
    else -> "${hours}h"
}

@Composable
private fun SectionLabel(text: String) {
    if (text.isEmpty()) {
        androidx.compose.foundation.layout.Spacer(Modifier.height(18.dp))
        return
    }
    Text(
        text,
        fontFamily = GoogleSans,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        color = Theme.textSecondary,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp)
    )
}

@Composable
private fun ThemedField(value: String, onValueChange: (String) -> Unit, placeholder: String, textAlign: TextAlign = TextAlign.Start) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmoothCorner(10.dp))
            .background(Theme.fieldFill)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText.copy(alpha = 0.45f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText, textAlign = textAlign),
            cursorBrush = SolidColor(Theme.fieldText),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OutlineTile(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = GoogleSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = Theme.textPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .height(TileHeight)
            .clip(SmoothCorner(14.dp))
            .background(Theme.cardFill)
            .border(1.dp, Theme.cardBorder, SmoothCorner(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp)
            .wrapContentHeight(Alignment.CenterVertically),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ModeTile(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Text(
        text = label,
        fontFamily = GoogleSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = if (selected) Theme.accentText else Theme.textPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .height(TileHeight)
            .clip(SmoothCorner(14.dp))
            .background(if (selected) Theme.accent else Theme.cardFill)
            .border(1.dp, if (selected) Theme.accent else Theme.cardBorder, SmoothCorner(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
    )
}

@Composable
private fun FormatTile(title: String, subtitle: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .height(64.dp)
            .clip(SmoothCorner(14.dp))
            .background(if (selected) Theme.accent else Theme.cardFill)
            .border(1.dp, if (selected) Theme.accent else Theme.cardBorder, SmoothCorner(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            title,
            fontFamily = GoogleSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (selected) Theme.accentText else Theme.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            subtitle,
            fontFamily = GoogleSans,
            fontSize = 11.sp,
            color = if (selected) Theme.accentText.copy(alpha = 0.75f) else Theme.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun WinConditionRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontFamily = GoogleSans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Theme.textPrimary)
            Text(subtitle, fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary)
        }
        Icon(
            if (selected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) Theme.accent else Theme.textSecondary
        )
    }
}
