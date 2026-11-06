package com.aikido.antrenamente.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Kind { TRAINING, STAGE, EXAM }

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

data class Stats(val present: Int, val absent: Int) {
    val total: Int get() = present + absent
    val percent: Int get() = if (total == 0) 0 else Math.round(present * 100f / total)
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
        .filter { it.kind != Kind.TRAINING && it.date.isAfter(today) }
        .sortedBy { it.date }

    /** The next regular training after [today]. */
    fun nextTraining(today: LocalDate): LocalDate {
        var day = today.plusDays(1)
        while (!isTrainingDay(day)) day = day.plusDays(1)
        return day
    }

    fun stats(entries: List<Entry>, include: (LocalDate) -> Boolean): Stats {
        val selected = entries.filter { include(it.date) }
        return Stats(
            present = selected.count { it.status == Status.PRESENT },
            absent = selected.count { it.status == Status.ABSENT },
        )
    }
}
