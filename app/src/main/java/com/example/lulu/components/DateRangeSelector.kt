package com.example.lulu.components

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*
import kotlin.collections.forEach

@Composable
fun DateRangeSelector(
    showDialog: Boolean,
    startDate: LocalDate?,
    endDate: LocalDate?,
    onDateRangeSelected: (LocalDate, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    if (showDialog) {
        var currentMonth by remember { mutableStateOf(YearMonth.now()) }
        var tempStartDate by remember { mutableStateOf(startDate) }
        var tempEndDate by remember { mutableStateOf(endDate) }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                modifier = Modifier.width(IntrinsicSize.Min)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Calendar Header with month navigation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                            Icon(Icons.Default.ChevronLeft, "Previous month")
                        }
                        Text(
                            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                            style = MaterialTheme.typography.titleMedium
                        )
                        IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                            Icon(Icons.Default.ChevronRight, "Next month")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Weekday headers
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar grid
                    val firstDayOfMonth = currentMonth.atDay(1)
                    val firstDayOfGrid = firstDayOfMonth.minusDays(firstDayOfMonth.getDayOfWeekValue().toLong() - 1)

                    for (week in 0..5) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (day in 0..6) {
                                val date = firstDayOfGrid.plusDays((week * 7 + day).toLong())
                                val isSelected = date == tempStartDate || date == tempEndDate
                                val isInRange = tempStartDate != null && tempEndDate != null &&
                                        date.isAfter(tempStartDate) && date.isBefore(tempEndDate)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                isInRange -> MaterialTheme.colorScheme.primaryContainer
                                                else -> MaterialTheme.colorScheme.surface
                                            }
                                        )
                                        .clickable {
                                            when {
                                                tempStartDate == null -> tempStartDate = date
                                                tempEndDate == null && date.isAfter(tempStartDate) ->
                                                    tempEndDate = date

                                                else -> {
                                                    tempStartDate = date
                                                    tempEndDate = null
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = date.dayOfMonth.toString(),
                                        color = when {
                                            isSelected -> MaterialTheme.colorScheme.onPrimary
                                            isInRange -> MaterialTheme.colorScheme.onPrimaryContainer
                                            date.month == currentMonth.month ->
                                                MaterialTheme.colorScheme.onSurface

                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (tempStartDate != null && tempEndDate != null) {
                                    onDateRangeSelected(tempStartDate!!, tempEndDate!!)
                                    onDismiss()
                                }
                            },
                            enabled = tempStartDate != null && tempEndDate != null
                        ) {
                            Text("Select")
                        }
                    }
                }
            }
        }
    }
}

// Helper function to get first day of week (Sunday = 1, Monday = 2, etc.)
@RequiresApi(Build.VERSION_CODES.O)
private fun LocalDate.getDayOfWeekValue(): Int {
    return when (dayOfWeek) {
        DayOfWeek.SUNDAY -> 1
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
        DayOfWeek.SATURDAY -> 7
    }
}
