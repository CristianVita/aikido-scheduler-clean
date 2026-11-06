package com.aikido.antrenamente

import com.aikido.antrenamente.data.Backup
import com.aikido.antrenamente.data.BackupCodec
import com.aikido.antrenamente.domain.Entry
import com.aikido.antrenamente.domain.Kind
import com.aikido.antrenamente.domain.Schedule
import com.aikido.antrenamente.domain.Status
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class ScheduleTest {
    private val start = LocalDate.of(2026, 11, 3) // Tuesday
    private val wed = LocalDate.of(2026, 11, 4)
    private val thu = LocalDate.of(2026, 11, 5)
    private val fri = LocalDate.of(2026, 11, 6)
    private val sat = LocalDate.of(2026, 11, 7)
    private val sun = LocalDate.of(2026, 11, 8)
    private val mon = LocalDate.of(2026, 11, 9)

    private fun pendingDays(entries: List<Entry>, today: LocalDate) =
        Schedule.pending(entries, start, today).map { it.date }

    private fun answered(date: LocalDate) = Entry(date, Kind.TRAINING, Status.PRESENT, null)

    @Test
    fun trainingDaysAreMondayWednesdayFriday() {
        assertEquals(
            listOf(true, false, true, false, true, false, false),
            (0L..6L).map { Schedule.isTrainingDay(mon.plusDays(it)) },
        )
    }

    @Test
    fun asksAboutTodayOnATrainingDay() {
        assertEquals(listOf(fri), pendingDays(listOf(answered(wed)), fri))
    }

    @Test
    fun unansweredDayIsAskedAgainOnTheNextDay() {
        assertEquals(listOf(wed), pendingDays(emptyList(), thu))
    }

    @Test
    fun fridayStillOpenOnTheWeekendAndMondayIsAdded() {
        val entries = listOf(answered(wed))
        assertEquals(listOf(fri), pendingDays(entries, sat))
        assertEquals(listOf(fri), pendingDays(entries, sun))
        assertEquals(listOf(fri, mon), pendingDays(entries, mon))
    }

    @Test
    fun todayIsNotAskedBeforeTheQuestionTime() {
        val at = { hour: Int, minute: Int -> fri.atTime(hour, minute) }
        val questionTime = LocalTime.of(21, 0)
        val days = { hour: Int, minute: Int ->
            Schedule.due(emptyList(), start, at(hour, minute), questionTime).map { it.date }
        }
        assertEquals(listOf(wed), days(1, 23))
        assertEquals(listOf(wed), days(20, 59))
        assertEquals(listOf(wed, fri), days(21, 0))
    }

    @Test
    fun yesterdayIsAskedFromMidnight() {
        val questions = Schedule.due(listOf(answered(wed)), start, sat.atTime(0, 5), LocalTime.of(21, 0))
        assertEquals(listOf(fri), questions.map { it.date })
    }

    @Test
    fun stageTodayAlsoWaitsForTheQuestionTime() {
        val stage = Entry(sat, Kind.STAGE, null, "Stagiu")
        val entries = listOf(answered(wed), answered(fri), stage)
        val time = LocalTime.of(21, 0)
        assertEquals(emptyList<LocalDate>(), Schedule.due(entries, start, sat.atTime(10, 0), time).map { it.date })
        assertEquals(listOf(sat), Schedule.due(entries, start, sat.atTime(21, 30), time).map { it.date })
    }

    @Test
    fun nothingAskedOnARestDayWhenEverythingIsAnswered() {
        assertEquals(emptyList<LocalDate>(), pendingDays(listOf(answered(wed), answered(fri)), sat))
    }

    @Test
    fun daysBeforeTrackingStartAreNeverAsked() {
        assertEquals(emptyList<LocalDate>(), pendingDays(emptyList(), start))
    }

    @Test
    fun stageOnARandomDayIsAskedOnlyOnceItArrives() {
        val stage = Entry(sat, Kind.STAGE, null, "Stagiu Perpelici")
        val entries = listOf(answered(wed), answered(fri), stage)
        assertEquals(emptyList<LocalDate>(), pendingDays(entries, fri))
        val question = Schedule.pending(entries, start, sat).single()
        assertEquals(sat, question.date)
        assertTrue(question.text.contains("Stagiu Perpelici"))
    }

    @Test
    fun stageOnATrainingDayProducesOneQuestion() {
        val stage = Entry(fri, Kind.STAGE, null, "Stagiu")
        val questions = Schedule.pending(listOf(answered(wed), stage), start, fri)
        assertEquals("Stagiu", questions.single().title)
    }

    @Test
    fun notHeldCountsAsAnsweredButNotInStats() {
        val entries = listOf(
            answered(wed),
            Entry(fri, Kind.TRAINING, Status.NOT_HELD, null),
            Entry(mon, Kind.TRAINING, Status.ABSENT, null),
        )
        assertEquals(emptyList<LocalDate>(), pendingDays(entries, mon))
        val stats = Schedule.stats(entries) { true }
        assertEquals(1, stats.present)
        assertEquals(1, stats.absent)
        assertEquals(50, stats.percent)
    }

    @Test
    fun nextTrainingSkipsRestDays() {
        assertEquals(mon, Schedule.nextTraining(fri))
        assertEquals(fri, Schedule.nextTraining(wed))
        assertFalse(Schedule.isTrainingDay(sat))
    }

    @Test
    fun backupRoundTripKeepsEveryField() {
        val backup = Backup(
            start,
            listOf(
                answered(wed),
                Entry(fri, Kind.TRAINING, Status.NOT_HELD, null),
                Entry(sat, Kind.STAGE, null, "Stagiu „Dorin” Marchis"),
            ),
        )
        assertEquals(backup, BackupCodec.decode(BackupCodec.encode(backup)))
    }

    @Test(expected = Exception::class)
    fun malformedBackupIsRejected() {
        BackupCodec.decode("""{"trackingStart":"2026-11-03","entries":[{"date":"nope"}]}""")
    }
}
