package com.aikido.antrenamente.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Kind { TRAINING, STAGE, EXAM, VACATION }

enum class Status { PRESENT, ABSENT, NOT_HELD }

/** One recorded day. A row with no status is a planned stage or exam waiting for an answer. */
data class Entry(
    val date: LocalDate,
    val kind: Kind,
    val status: Status?,
    val title: String?,
)

data class Question(val date: LocalDate, val kind: Kind, val title: String?) {
    val text: String
        get() = if (title != null) {
            "Ai fost la „$title” ${formatDay(date)}?"
        } else {
            "Ai fost la antrenament ${formatDay(date)}?"
        }
}

/**
 * [scheduled] counts every session in the period, past and future, so the user sees how many they
 * attended out of all of them. [percent] only uses answered days, so it stays fair mid-month.
 */
data class Stats(val present: Int, val absent: Int, val scheduled: Int) {
    val answered: Int get() = present + absent
    val percent: Int get() = if (answered == 0) 0 else Math.round(present * 100f / answered)
}

/** "1 antrenament", "13 antrenamente", "20 de antrenamente" (Romanian needs "de" from 20 up). */
fun trainingCount(n: Int): String = when {
    n == 1 -> "1 antrenament"
    n % 100 >= 20 || (n >= 100 && n % 100 == 0) -> "$n de antrenamente"
    else -> "$n antrenamente"
}

private val RO = Locale.forLanguageTag("ro")
private val DAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, d MMMM", RO)
private val MONTH_FORMAT = DateTimeFormatter.ofPattern("LLLL yyyy", RO)
private val MONTH_NAME_FORMAT = DateTimeFormatter.ofPattern("LLLL", RO)

fun formatDay(date: LocalDate): String = DAY_FORMAT.format(date)
fun formatMonth(date: LocalDate): String = MONTH_FORMAT.format(date).replaceFirstChar { it.uppercase(RO) }
fun formatMonthName(date: LocalDate): String = MONTH_NAME_FORMAT.format(date).replaceFirstChar { it.uppercase(RO) }

object Schedule {
    fun isTrainingDay(date: LocalDate): Boolean = when (date.dayOfWeek) {
        DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY -> true
        else -> false
    }

    /**
     * Everything still unanswered up to and including [today], oldest first: planned events, plus
     * every training day since [trackingStart] with no row. A day stays here until it is answered,
     * which is what makes a missed Monday come back on Tuesday (and later, if still ignored).
     */
    fun pending(entries: List<Entry>, trackingStart: LocalDate, today: LocalDate): List<Question> {
        val recorded = entries.mapTo(HashSet()) { it.date }
        val questions = entries
            .filter { it.status == null && !it.date.isAfter(today) }
            .mapTo(ArrayList()) { Question(it.date, it.kind, it.title) }

        var day = trackingStart
        while (!day.isAfter(today)) {
            if (isTrainingDay(day) && day !in recorded) {
                questions += Question(day, Kind.TRAINING, null)
            }
            day = day.plusDays(1)
        }
        return questions.sortedBy { it.date }
    }

    /**
     * The questions to actually ask at [now]. Today's only counts from [questionTime] onwards,
     * because before that the training may not have happened yet.
     */
    fun due(
        entries: List<Entry>,
        trackingStart: LocalDate,
        now: LocalDateTime,
        questionTime: LocalTime,
    ): List<Question> {
        val today = now.toLocalDate()
        val todayIsDue = !now.toLocalTime().isBefore(questionTime)
        return pending(entries, trackingStart, today).filter { it.date != today || todayIsDue }
    }

    /** Stages and exams after [today], soonest first. */
    fun upcomingEvents(entries: List<Entry>, today: LocalDate): List<Entry> = entries
        .filter { it.kind != Kind.TRAINING && it.kind != Kind.VACATION && it.date.isAfter(today) }
        .sortedBy { it.date }

    /** The next regular training after [today], skipping days already marked as not held (vacation included). */
    fun nextTraining(entries: List<Entry>, today: LocalDate): LocalDate {
        val byDate = entries.associateBy { it.date }
        var day = today.plusDays(1)
        while (!isTrainingDay(day) || byDate[day]?.status == Status.NOT_HELD) day = day.plusDays(1)
        return day
    }

    /**
     * Statistics for [from]..[to] inclusive. A day is a session when it is a regular training day or
     * has a stage, exam or answer, unless it was not held. Days before the first recorded day are
     * left out, since there is no data for them.
     */
    fun stats(entries: List<Entry>, from: LocalDate, to: LocalDate): Stats {
        val byDate = entries.associateBy { it.date }
        val firstKnown = entries.minOfOrNull { it.date } ?: from
        var present = 0
        var absent = 0
        var scheduled = 0
        var day = maxOf(from, firstKnown)
        while (!day.isAfter(to)) {
            val entry = byDate[day]
            when (entry?.status) {
                Status.PRESENT -> present++
                Status.ABSENT -> absent++
                else -> Unit
            }
            val isSession = entry?.status != Status.NOT_HELD && (entry != null || isTrainingDay(day))
            if (isSession) scheduled++
            day = day.plusDays(1)
        }
        return Stats(present, absent, scheduled)
    }

    fun monthStats(entries: List<Entry>, month: YearMonth): Stats =
        stats(entries, month.atDay(1), month.atEndOfMonth())

    fun yearStats(entries: List<Entry>, year: Int): Stats =
        stats(entries, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
}
