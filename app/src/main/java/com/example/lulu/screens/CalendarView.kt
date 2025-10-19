package com.example.lulu.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import kotlin.collections.chunked
import kotlin.collections.forEach
import kotlin.collections.map
import kotlin.text.take

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarView(
    symptomDates: Set<LocalDate>,
    modifier: Modifier = Modifier,
    onDateClick: (LocalDate) -> Unit = {}
) {
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            CalendarHeader(
                currentMonth = currentMonth,
                onPreviousMonth = { currentMonth = currentMonth.minusMonths(1) },
                onNextMonth = { currentMonth = currentMonth.plusMonths(1) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DaysOfWeekHeader()
            Spacer(modifier = Modifier.height(4.dp))
            CalendarGrid(
                currentMonth = currentMonth,
                symptomDates = symptomDates,
                onDateClick = onDateClick
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarHeader(currentMonth: YearMonth, onPreviousMonth: () -> Unit, onNextMonth: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.ChevronLeft, "Previous Month")
            }

            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Icon(Icons.Default.ChevronRight, "Next Month")
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DaysOfWeekHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        val days = DayOfWeek.values()
        // Shift array to start from Sunday
        val shiftedDays = Array(7) { days[(it + 6) % 7] }
        shiftedDays.forEach { dayOfWeek ->
            Text(
                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    symptomDates: Set<LocalDate>,
    onDateClick: (LocalDate) -> Unit
) {
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = currentMonth.atDay(1).dayOfWeek
    // Adjust for Sunday as the first day of the week (DayOfWeek.SUNDAY.value is 7)
    val firstDayOffset = (firstDayOfMonth.value) % 7

    Column {
        val totalCells = if (firstDayOffset + daysInMonth > 35) 42 else 35
        val days = (1..totalCells).map {
            val day = it - firstDayOffset
            if (day in 1..daysInMonth) {
                currentMonth.atDay(day)
            } else {
                null
            }
        }

        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                hasSymptom = date in symptomDates,
                                onClick = { onDateClick(date) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
private fun DayCell(date: LocalDate, hasSymptom: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isToday = date.isEqual(LocalDate.now())

    val backgroundColor = when {
        isHovered && hasSymptom -> MaterialTheme.colorScheme.primaryContainer
        isHovered -> MaterialTheme.colorScheme.surfaceVariant
        hasSymptom -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        else -> Color.Transparent
    }

    val textColor = when {
        isHovered && hasSymptom -> MaterialTheme.colorScheme.onPrimaryContainer
        hasSymptom -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp),
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = if (isHovered) 4.dp else 0.dp
    ) {
        Column(
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isToday || hasSymptom) FontWeight.Bold else FontWeight.Normal
            )

            if (hasSymptom) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isHovered) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
        }
    }
}
