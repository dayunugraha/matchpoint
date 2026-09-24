package com.matchpoint.app.ui.common

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import com.matchpoint.app.ui.theme.SmoothCorner
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.matchpoint.app.ui.theme.GoogleSans
import com.matchpoint.app.ui.theme.Theme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** "Edit Mabar" — name, date, and planned time window. Badge photo picker isn't ported (out of scope). */
@Composable
fun EditSessionDetailsDialog(
    initialName: String,
    initialDate: Long,
    initialStartTime: Long?,
    initialEndTime: Long?,
    onDismiss: () -> Unit,
    onSave: (name: String, date: Long, startTime: Long?, endTime: Long?) -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(initialName) }
    var date by remember { mutableStateOf(initialDate) }
    var startTime by remember { mutableStateOf(initialStartTime) }
    var endTime by remember { mutableStateOf(initialEndTime) }

    val dateFmt = remember { SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()) }
    val timeFmt = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Mabar") },
        text = {
            Column {
                Text("Mabar Name", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(bottom = 6.dp))
                ThemedField(value = name, onValueChange = { name = it }, placeholder = "e.g. Rabu Pagi")

                Text("Date", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                Tile(dateFmt.format(java.util.Date(date))) {
                    val cal = Calendar.getInstance().apply { timeInMillis = date }
                    DatePickerDialog(context, { _, y, m, d ->
                        cal.set(y, m, d)
                        date = cal.timeInMillis
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }

                Text("Mabar Time", fontFamily = GoogleSans, fontSize = 12.sp, color = Theme.textSecondary, modifier = Modifier.padding(top = 16.dp, bottom = 6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        Tile(startTime?.let { timeFmt.format(java.util.Date(it)) } ?: "Start") {
                            val cal = Calendar.getInstance().apply { timeInMillis = startTime ?: date }
                            TimePickerDialog(context, { _, h, min ->
                                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min)
                                startTime = cal.timeInMillis
                            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Tile(endTime?.let { timeFmt.format(java.util.Date(it)) } ?: "End") {
                            val cal = Calendar.getInstance().apply { timeInMillis = endTime ?: date }
                            TimePickerDialog(context, { _, h, min ->
                                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min)
                                endTime = cal.timeInMillis
                            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, date, startTime, endTime) }, enabled = name.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ThemedField(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SmoothCorner(10.dp))
            .background(Theme.fieldFill)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (value.isEmpty()) {
            Text(placeholder, fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText.copy(alpha = 0.45f))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(fontFamily = GoogleSans, fontSize = 14.sp, color = Theme.fieldText),
            cursorBrush = SolidColor(Theme.fieldText),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Tile(label: String, onClick: () -> Unit) {
    Text(
        label,
        fontFamily = GoogleSans,
        fontSize = 13.sp,
        color = Theme.textPrimary,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(SmoothCorner(10.dp))
            .background(Theme.cardFill)
            .border(1.dp, Theme.cardBorder, SmoothCorner(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    )
}
