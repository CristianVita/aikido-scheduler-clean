package com.aikido.antrenamente.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aikido.antrenamente.domain.Entry
import com.aikido.antrenamente.domain.Kind
import com.aikido.antrenamente.domain.Schedule
import com.aikido.antrenamente.domain.Status
import com.aikido.antrenamente.domain.formatMonth
import com.aikido.antrenamente.domain.formatMonthName
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

private val WEEKDAYS = listOf("L", "Ma", "Mi", "J", "V", "S", "D")

@Composable
fun CalendarScreen(
    entries: List<Entry>,
    trackingStart: LocalDate,
    now: LocalDateTime,
    questionTime: LocalTime,
    onAnswer: (LocalDate, Status?) -> Unit,
    onEventRemoved: (LocalDate) -> Unit,
    onEventSaved: (LocalDate, LocalDate, Kind, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = now.toLocalDate()
    var shown by remember { mutableStateOf(YearMonth.from(today)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }
    val byDate = remember(entries) { entries.associateBy { it.date } }
    val pendingDays = remember(entries, now, questionTime) {
        Schedule.due(entries, trackingStart, now, questionTime).mapTo(HashSet()) { it.date }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { shown = shown.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Luna anterioară")
            }
            Text(
                formatMonth(shown.atDay(1)),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            IconButton(onClick = { shown = shown.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Luna următoare")
            }
        }

        MonthGrid(shown, byDate, pendingDays, today, onDayClick = { selected = it })
        Legend()
        YearSummary(entries, shown.year, today, onMonthClick = { shown = it })
        Spacer(Modifier.size(72.dp)) // Keeps the last rows clear of the add button.
    }

    selected?.let { date ->
        DayDialog(
            date = date,
            entry = byDate[date],
            onDismiss = { selected = null },
            onAnswer = onAnswer,
            onEventRemoved = onEventRemoved,
            onEventSaved = onEventSaved,
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    byDate: Map<LocalDate, Entry>,
    pendingDays: Set<LocalDate>,
    today: LocalDate,
    onDayClick: (LocalDate) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WEEKDAYS.forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
        val leadingBlanks = month.atDay(1).dayOfWeek.value - 1
        val cells: List<LocalDate?> =
            List(leadingBlanks) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (date != null) {
                            DayCell(date, byDate[date], date in pendingDays, date == today) { onDayClick(date) }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    entry: Entry?,
    pending: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val statusColor = when (entry?.status) {
        Status.PRESENT -> PresentColor
        Status.ABSENT -> AbsentColor
        Status.NOT_HELD -> NotHeldColor
        null -> null
    }
    val isExam = entry?.kind == Kind.EXAM
    val plannedTraining = statusColor == null && Schedule.isTrainingDay(date)
    val background = statusColor ?: when {
        isExam -> ExamColor
        plannedTraining -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val markColor = when {
        statusColor != null -> Color.White
        isExam -> OnExamColor
        else -> null
    }
    val border = when {
        // An answered exam keeps its attendance colour, so the gold ring is what marks it as an exam.
        isExam && statusColor != null -> BorderStroke(3.dp, ExamColor)
        pending -> BorderStroke(2.dp, AbsentColor)
        isToday -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else -> null
    }
    Box(
        Modifier
            .fillMaxSize()
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = markColor ?: MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
        )
        if (entry?.title != null) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(6.dp)
                    .background(markColor ?: MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(PresentColor, "Am fost")
        LegendItem(AbsentColor, "N-am fost")
        LegendItem(NotHeldColor, "Nu s-a ținut")
        LegendItem(ExamColor, "Examen")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(12.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun YearSummary(
    entries: List<Entry>,
    year: Int,
    today: LocalDate,
    onMonthClick: (YearMonth) -> Unit,
) {
    Text("Pe luni în $year", style = MaterialTheme.typography.titleMedium)
    Column(Modifier.fillMaxWidth()) {
        (1..12).map { YearMonth.of(year, it) }.forEach { month ->
            val stats = Schedule.monthStats(entries, month)
            if (stats.answered > 0 || month == YearMonth.from(today)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onMonthClick(month) }
                        .padding(vertical = 8.dp),
                ) {
                    Text(formatMonthName(month.atDay(1)), Modifier.weight(1f))
                    Text("${stats.present} / ${stats.scheduled}", Modifier.padding(end = 16.dp))
                    Text("${stats.percent}%", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
