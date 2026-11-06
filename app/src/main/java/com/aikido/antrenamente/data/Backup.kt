package com.aikido.antrenamente.data

import com.aikido.antrenamente.domain.Entry
import com.aikido.antrenamente.domain.Kind
import com.aikido.antrenamente.domain.Status
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class Backup(val trackingStart: LocalDate, val entries: List<Entry>)

/** The backup file format, also used by the bundled assets/history.json. */
object BackupCodec {
    fun encode(backup: Backup): String {
        val rows = JSONArray()
        backup.entries.sortedBy { it.date }.forEach { entry ->
            rows.put(
                JSONObject()
                    .put("date", entry.date.toString())
                    .put("kind", entry.kind.name)
                    .put("status", entry.status?.name ?: JSONObject.NULL)
                    .put("title", entry.title ?: JSONObject.NULL),
            )
        }
        return JSONObject()
            .put("version", 1)
            .put("trackingStart", backup.trackingStart.toString())
            .put("entries", rows)
            .toString(2)
    }

    /** Throws on anything malformed, so a bad file never replaces existing data. */
    fun decode(text: String): Backup {
        val root = JSONObject(text)
        val rows = root.getJSONArray("entries")
        val entries = (0 until rows.length()).map { index ->
            val row = rows.getJSONObject(index)
            Entry(
                date = LocalDate.parse(row.getString("date")),
                kind = Kind.valueOf(row.getString("kind")),
                status = if (row.isNull("status")) null else Status.valueOf(row.getString("status")),
                title = if (row.isNull("title")) null else row.getString("title"),
            )
        }
        return Backup(LocalDate.parse(root.getString("trackingStart")), entries)
    }
}
