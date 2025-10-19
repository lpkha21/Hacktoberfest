package com.example.lulu.screens

import android.graphics.Color
import android.widget.CalendarView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.time.LocalDate
import java.util.*
import kotlin.apply
import kotlin.collections.forEach

@Composable
fun SimpleCalendarView(
    symptomDates: Set<LocalDate>,
    onDateSelected: (LocalDate) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        AndroidView(
            modifier = Modifier.padding(16.dp),
            factory = { context ->
                CalendarView(context).apply {
                    // Style the calendar
                    setBackgroundColor(Color.TRANSPARENT)

                    // Highlight dates with symptoms
                    symptomDates.forEach { date ->
                        val calendar = Calendar.getInstance().apply {
                            set(date.year, date.monthValue - 1, date.dayOfMonth)
                        }
                        // Add visual indicator for symptom dates
                        setDate(calendar.timeInMillis, true, true)
                    }

                    // Date selection callback
                    setOnDateChangeListener { _, year, month, dayOfMonth ->
                        onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                    }
                }
            },
            update = { view ->
                // Update logic here if needed
                view.setOnDateChangeListener { _, year, month, dayOfMonth ->
                    onDateSelected(LocalDate.of(year, month + 1, dayOfMonth))
                }
            }
        )
    }
}
