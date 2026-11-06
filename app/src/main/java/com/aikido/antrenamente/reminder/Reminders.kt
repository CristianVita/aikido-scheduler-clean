package com.aikido.antrenamente.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aikido.antrenamente.MainActivity
import com.aikido.antrenamente.R
import com.aikido.antrenamente.data.Store
import com.aikido.antrenamente.domain.Question
import com.aikido.antrenamente.domain.Schedule
import com.aikido.antrenamente.domain.Status
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

object Reminders {
    private const val CHANNEL_ID = "questions"
    private const val NOTIFICATION_ID = 1
    private const val EXTRA_DATE = "date"
    private const val EXTRA_STATUS = "status"

    /** Arms the next daily check. Re-arming replaces the previous alarm. */
    fun schedule(context: Context) {
        val time = Store.get(context).reminderTime
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        var next = now.with(time).withSecond(0).withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            next.toInstant().toEpochMilli(),
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ReminderReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    /** Asks the oldest open question, or clears the notification when nothing is due. */
    fun showNext(context: Context) {
        val question = dueQuestions(context).firstOrNull()
        if (question == null) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        } else {
            post(context, question)
        }
    }

    /** After an answer given in the app: moves on only if a notification is already showing. */
    fun syncActive(context: Context) {
        val active = context.getSystemService(NotificationManager::class.java)
            .activeNotifications.any { it.id == NOTIFICATION_ID }
        if (active) showNext(context)
    }

    private fun dueQuestions(context: Context): List<Question> {
        val store = Store.get(context)
        return Schedule.due(store.entries.value, store.trackingStart, LocalDateTime.now(), store.reminderTime)
    }

    private fun post(context: Context, question: Question) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Întrebări despre antrenamente",
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Antrenamente Aikido")
            .setContentText(question.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(question.text))
            .setContentIntent(open)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "Am fost", answer(context, question.date, Status.PRESENT))
            .addAction(0, "N-am fost", answer(context, question.date, Status.ABSENT))
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call; the app still shows the question.
        }
    }

    private fun answer(context: Context, date: LocalDate, status: Status): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            status.ordinal + 1,
            Intent(context, AnswerReceiver::class.java)
                .putExtra(EXTRA_DATE, date.toString())
                .putExtra(EXTRA_STATUS, status.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    internal fun handleAnswer(context: Context, intent: Intent) {
        val date = intent.getStringExtra(EXTRA_DATE)?.let(LocalDate::parse) ?: return
        val status = intent.getStringExtra(EXTRA_STATUS)?.let(Status::valueOf) ?: return
        Store.get(context).setStatus(date, status)
        showNext(context)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.showNext(context)
        Reminders.schedule(context)
    }
}

class AnswerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.handleAnswer(context, intent)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.schedule(context)
    }
}
