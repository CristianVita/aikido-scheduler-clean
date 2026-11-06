package com.aikido.antrenamente.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aikido.antrenamente.domain.Entry
import com.aikido.antrenamente.domain.Question
import com.aikido.antrenamente.domain.Schedule
import com.aikido.antrenamente.domain.Stats
import com.aikido.antrenamente.domain.Status
import com.aikido.antrenamente.domain.formatDay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Composable
fun HomeScreen(
    entries: List<Entry>,
    trackingStart: LocalDate,
    now: LocalDateTime,
    questionTime: LocalTime,
    backupFailed: Boolean,
    onAnswer: (LocalDate, Status?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = now.toLocalDate()
    val due = Schedule.due(entries, trackingStart, now, questionTime)
    // Today's session, while it is still too early to ask about it.
    val laterToday = Schedule.pending(entries, trackingStart, today)
        .firstOrNull { it.date == today && it !in due }
    val upcoming = Schedule.upcomingEvents(entries, today)

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Antrenamente", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)

        if (backupFailed) {
            Text("Copia automată nu s-a salvat. Verifică în Setări.", color = AbsentColor)
        }

        due.forEach { question -> QuestionCard(question, today, onAnswer) }

        if (laterToday != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (laterToday.title != null) "Azi: ${laterToday.title}" else "Azi ai antrenament",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("Te întreb cum a fost după ora $questionTime.")
                }
            }
        } else if (due.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ești la zi", style = MaterialTheme.typography.titleMedium)
                    Text("Următorul antrenament: ${formatDay(Schedule.nextTraining(today))}")
                }
            }
        }

        StatsRow(
            month = Schedule.stats(entries) { it.year == today.year && it.month == today.month },
            year = Schedule.stats(entries) { it.year == today.year },
            yearNumber = today.year,
        )

        if (upcoming.isNotEmpty()) {
            Text("Urmează", style = MaterialTheme.typography.titleMedium)
            upcoming.forEach { event ->
                Column {
                    Text(event.title.orEmpty(), fontWeight = FontWeight.Medium)
                    Text(formatDay(event.date), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.size(72.dp)) // Keeps the last rows clear of the add button.
    }
}

@Composable
private fun QuestionCard(
    question: Question,
    today: LocalDate,
    onAnswer: (LocalDate, Status?) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (question.date) {
                today -> Text("Azi", style = MaterialTheme.typography.labelLarge)
                today.minusDays(1) -> Text("Ieri", style = MaterialTheme.typography.labelLarge)
            }
            Text(question.text, style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onAnswer(question.date, Status.PRESENT) },
                    colors = ButtonDefaults.buttonColors(containerColor = PresentColor, contentColor = Color.White),
                    modifier = Modifier.weight(1f),
                ) { Text("Am fost") }
                Button(
                    onClick = { onAnswer(question.date, Status.ABSENT) },
                    colors = ButtonDefaults.buttonColors(containerColor = AbsentColor, contentColor = Color.White),
                    modifier = Modifier.weight(1f),
                ) { Text("N-am fost") }
            }
            TextButton(
                onClick = { onAnswer(question.date, Status.NOT_HELD) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) { Text("Nu s-a ținut") }
        }
    }
}

@Composable
private fun StatsRow(month: Stats, year: Stats, yearNumber: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Luna asta", month, Modifier.weight(1f))
        StatCard("În $yearNumber", year, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, stats: Stats, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text("${stats.percent}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text("${stats.present} din ${stats.total} prezențe", style = MaterialTheme.typography.bodySmall)
        }
    }
}
