package com.aikido.antrenamente.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.aikido.antrenamente.domain.Entry
import com.aikido.antrenamente.domain.Kind
import com.aikido.antrenamente.domain.Schedule
import com.aikido.antrenamente.domain.Status
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class BackupStatus(val uri: String?, val lastSuccess: LocalDateTime?, val failed: Boolean)

/**
 * Single process-wide store. Receivers and the UI share it, so an answer given from a notification
 * appears on screen immediately through [entries].
 */
class Store private constructor(private val context: Context) :
    SQLiteOpenHelper(context, "aikido.db", null, 1) {

    private val _entries by lazy { MutableStateFlow(readEntries()) }
    val entries: StateFlow<List<Entry>> get() = _entries

    private val _backupStatus by lazy { MutableStateFlow(readBackupStatus()) }
    val backupStatus: StateFlow<BackupStatus> get() = _backupStatus

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE entries (date TEXT PRIMARY KEY, kind TEXT NOT NULL, status TEXT, title TEXT)",
        )
        db.execSQL("CREATE TABLE meta (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        val history = context.assets.open("history.json").bufferedReader().use { it.readText() }
        writeBackup(db, BackupCodec.decode(history))
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    val trackingStart: LocalDate
        get() = LocalDate.parse(meta(KEY_TRACKING_START)!!)

    var reminderTime: LocalTime
        get() = meta(KEY_REMINDER_TIME)?.let(LocalTime::parse) ?: DEFAULT_REMINDER_TIME
        set(value) = setMeta(KEY_REMINDER_TIME, value.toString())

    /** Records or clears the answer for a day. A plain training row with no answer is removed. */
    fun setStatus(date: LocalDate, status: Status?) {
        val current = entry(date)
        val kind = current?.kind ?: Kind.TRAINING
        if (status == null && kind == Kind.TRAINING) {
            delete(date)
        } else {
            put(Entry(date, kind, status, current?.title))
        }
        refresh()
    }

    /** Adds or edits a stage or exam on each day of a range, keeping answers already given. */
    fun putEvent(from: LocalDate, to: LocalDate, kind: Kind, title: String) {
        var day = from
        while (!day.isAfter(to)) {
            put(Entry(day, kind, entry(day)?.status, title))
            day = day.plusDays(1)
        }
        refresh()
    }

    /** Removes the event label. An answer stays only if the day is a regular training anyway. */
    fun removeEvent(date: LocalDate) {
        val current = entry(date) ?: return
        if (current.status != null && Schedule.isTrainingDay(date)) {
            put(Entry(date, Kind.TRAINING, current.status, null))
        } else {
            delete(date)
        }
        refresh()
    }

    /**
     * Marks every day in [from]..[to] as vacation: no question is ever asked about these days (not
     * even later), and they don't count in statistics. Overwrites any existing answer or event on
     * those days.
     */
    fun setVacation(from: LocalDate, to: LocalDate) {
        var day = from
        while (!day.isAfter(to)) {
            put(Entry(day, Kind.VACATION, Status.NOT_HELD, "Vacanță"))
            day = day.plusDays(1)
        }
        refresh()
    }

    /**
     * Clears the whole vacation block that [date] belongs to (every contiguous vacation day around
     * it), so those days can be asked about normally again.
     */
    fun removeVacation(date: LocalDate) {
        if (entry(date)?.kind != Kind.VACATION) return
        var start = date
        while (entry(start.minusDays(1))?.kind == Kind.VACATION) start = start.minusDays(1)
        var end = date
        while (entry(end.plusDays(1))?.kind == Kind.VACATION) end = end.plusDays(1)
        var day = start
        while (!day.isAfter(end)) {
            delete(day)
            day = day.plusDays(1)
        }
        refresh()
    }

    fun exportBackup(): String = BackupCodec.encode(Backup(trackingStart, entries.value))

    /** Sets the automatic backup file, or turns automatic backups off when null. */
    fun setBackupTarget(uri: String?) {
        writableDatabase.delete(
            "meta",
            "key IN (?, ?, ?)",
            arrayOf(KEY_BACKUP_URI, KEY_BACKUP_LAST, KEY_BACKUP_FAILED),
        )
        if (uri != null) setMeta(KEY_BACKUP_URI, uri)
        _backupStatus.value = readBackupStatus()
    }

    fun recordBackupResult(success: Boolean) {
        if (success) {
            setMeta(KEY_BACKUP_LAST, LocalDateTime.now().toString())
            writableDatabase.delete("meta", "key = ?", arrayOf(KEY_BACKUP_FAILED))
        } else {
            setMeta(KEY_BACKUP_FAILED, "1")
        }
        _backupStatus.value = readBackupStatus()
    }

    private fun readBackupStatus() = BackupStatus(
        uri = meta(KEY_BACKUP_URI),
        lastSuccess = meta(KEY_BACKUP_LAST)?.let(LocalDateTime::parse),
        failed = meta(KEY_BACKUP_FAILED) != null,
    )

    fun importBackup(text: String) {
        val backup = BackupCodec.decode(text)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("entries", null, null)
            writeBackup(db, backup)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    private fun writeBackup(db: SQLiteDatabase, backup: Backup) {
        backup.entries.forEach { insert(db, it) }
        db.insertWithOnConflict(
            "meta",
            null,
            ContentValues().apply {
                put("key", KEY_TRACKING_START)
                put("value", backup.trackingStart.toString())
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun entry(date: LocalDate): Entry? = entries.value.firstOrNull { it.date == date }

    private fun put(entry: Entry) = insert(writableDatabase, entry)

    private fun insert(db: SQLiteDatabase, entry: Entry) {
        db.insertWithOnConflict(
            "entries",
            null,
            ContentValues().apply {
                put("date", entry.date.toString())
                put("kind", entry.kind.name)
                put("status", entry.status?.name)
                put("title", entry.title)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    private fun delete(date: LocalDate) {
        writableDatabase.delete("entries", "date = ?", arrayOf(date.toString()))
    }

    private fun refresh() {
        _entries.value = readEntries()
    }

    private fun readEntries(): List<Entry> =
        readableDatabase.rawQuery("SELECT date, kind, status, title FROM entries ORDER BY date", null)
            .use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(
                            Entry(
                                date = LocalDate.parse(cursor.getString(0)),
                                kind = Kind.valueOf(cursor.getString(1)),
                                status = cursor.getString(2)?.let(Status::valueOf),
                                title = cursor.getString(3),
                            ),
                        )
                    }
                }
            }

    private fun meta(key: String): String? =
        readableDatabase.rawQuery("SELECT value FROM meta WHERE key = ?", arrayOf(key)).use {
            if (it.moveToFirst()) it.getString(0) else null
        }

    private fun setMeta(key: String, value: String) {
        writableDatabase.insertWithOnConflict(
            "meta",
            null,
            ContentValues().apply {
                put("key", key)
                put("value", value)
            },
            SQLiteDatabase.CONFLICT_REPLACE,
        )
    }

    companion object {
        private const val KEY_TRACKING_START = "tracking_start"
        private const val KEY_REMINDER_TIME = "reminder_time"
        private const val KEY_BACKUP_URI = "backup_uri"
        private const val KEY_BACKUP_LAST = "backup_last"
        private const val KEY_BACKUP_FAILED = "backup_failed"
        private val DEFAULT_REMINDER_TIME: LocalTime = LocalTime.of(21, 0)

        @Volatile
        private var instance: Store? = null

        fun get(context: Context): Store = instance ?: synchronized(this) {
            instance ?: Store(context.applicationContext).also { instance = it }
        }
    }
}
