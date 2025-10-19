package com.example.lulu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.lulu.navigation.Screen
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.collections.forEach
import kotlin.collections.toList
import kotlin.to

@Composable
fun HomeScreen(navController: NavController) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showSymptomDialog by remember { mutableStateOf(false) }
    
    // Sample symptom dates (replace with your actual data)
    val symptomDates = remember {
        setOf(
            LocalDate.now().minusDays(2),
            LocalDate.now().minusDays(5),
            LocalDate.now()
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WelcomeSection()
        }
        
        item {
            StatsRow()
        }

        item {
            DailyOverviewCard(navController)
        }

        item {
            SimpleCalendarView(
                symptomDates = symptomDates,
                onDateSelected = { date -> selectedDate = date }
            )
        }

        item {
            RecentActivitiesCard()
        }

        item {
            SymptomSummaryCard(navController)
        }
    }

    if (showSymptomDialog && selectedDate != null) {
        DayOverviewDialog(
            date = selectedDate!!,
            hasSymptoms = selectedDate in symptomDates,
            onDismiss = { showSymptomDialog = false }
        )
    }

    // SimpleCalendarView(
    //     symptomDates = symptomDates,
    //     onDateSelected = { date ->
    //         selectedDate = date
    //         showSymptomDialog = true
    //     }
    // )
}

@Composable
private fun WelcomeSection() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Track and monitor your health",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            title = "This Week",
            value = "5",
            description = "recorded symptoms",
            icon = Icons.Default.TrendingUp
        )
        StatCard(
            modifier = Modifier.weight(1f),
            title = "Reports",
            value = "2",
            description = "generated this month",
            icon = Icons.Default.Assignment
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    description: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun RecentActivitiesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recent Activities",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            val activities = listOf(
                "Report generated for March" to "2 days ago",
                "Added new symptom entry" to "Yesterday",
                "Updated medication list" to "Today"
            )

            activities.forEach { (activity, time) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = activity,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
    }
}

@Composable
private fun DailyOverviewCard(navController: NavController) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Today's Overview",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "How are you feeling today?",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(
                onClick = {
                    // Navigate to Chat screen for symptom tracking
                    navController.navigate(Screen.Chat.route)
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Track Symptoms")
            }
        }
    }
}

@Composable
private fun QuickActionsRow(navController: NavController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            text = "New Entry",
            icon = Icons.Default.Add
        ) {
            navController.navigate(Screen.Chat.route)
        }
        QuickActionButton(
            text = "View History",
            icon = Icons.Default.History
        ) {
            navController.navigate(Screen.Reports.route)
        }
    }
}

@Composable
private fun QuickActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = text)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text)
        }
    }
}

@Composable
private fun SymptomSummaryCard(navController: NavController) {
    val recentSymptoms = remember {
        mutableStateListOf(
            "Headache - 2 hours ago",
            "Fatigue - Yesterday",
            "Nausea - Yesterday"
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Recent Symptoms",
                style = MaterialTheme.typography.titleMedium
            )
            if (recentSymptoms.isEmpty()) {
                Text(
                    text = "No recent symptoms recorded",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = recentSymptoms) { symptom ->
                        Text(
                            text = symptom,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                TextButton(
                    onClick = { navController.navigate(Screen.Reports.route) },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("View All")
                }
            }
        }
    }
}

@Composable
fun CalendarView(symptomDates: Set<LocalDate>) {
    // Placeholder for the calendar view
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Symptom Calendar",
                style = MaterialTheme.typography.titleMedium
            )
            // Here you would implement the actual calendar view,
            // for now, we just display the selected dates
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(items = symptomDates.toList()) { date ->
                    Text(
                        text = date.toString(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun DayOverviewDialog(
    date: LocalDate,
    hasSymptoms: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Overview for ${date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))}") },
        text = {
            if (hasSymptoms) {
                Column {
                    Text("Symptoms recorded on this date:")
                    Spacer(modifier = Modifier.height(8.dp))
                    // Add symptom list here
                    Text("• Headache (Severity: 7)")
                    Text("• Nausea (Severity: 4)")
                }
            } else {
                Text("No symptoms recorded for this date")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
