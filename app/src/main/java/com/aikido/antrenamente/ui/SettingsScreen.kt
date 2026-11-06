package com.aikido.antrenamente.ui

import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.aikido.antrenamente.data.AutoBackup
import com.aikido.antrenamente.data.Store
import com.aikido.antrenamente.domain.formatDay
import com.aikido.antrenamente.reminder.Reminders
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Composable
fun SettingsScreen(store: Store, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var reminderTime by remember { mutableStateOf(store.reminderTime) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    val backup by store.backupStatus.collectAsState()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use {
                    it.write(store.exportBackup())
                }
            }.isSuccess
            Toast.makeText(context, if (saved) "Copia a fost salvată" else "Salvarea a eșuat", Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> importUri = uri }
    val autoBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val kept = runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
            }.isSuccess
            if (kept) {
                store.setBackupTarget(uri.toString())
                AutoBackup.start(context)
            } else {
                Toast.makeText(context, "Locul ales nu permite copii automate", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Setări", style = MaterialTheme.typography.headlineMedium)

        Text("Întrebarea zilnică", style = MaterialTheme.typography.titleMedium)
        Text("La ora asta te întreabă despre ziua de azi și despre zilele rămase fără răspuns.")
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val time = LocalTime.of(hour, minute)
                        store.reminderTime = time
                        reminderTime = time
                        Reminders.schedule(context)
                    },
                    reminderTime.hour,
                    reminderTime.minute,
                    true,
                ).show()
            },
        ) { Text("Ora: $reminderTime") }

        Text("Copie automată", style = MaterialTheme.typography.titleMedium)
        if (backup.uri == null) {
            Text("O dată pe săptămână, aplicația actualizează singură un fișier. Alege Google Drive când se deschide selectorul.")
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { autoBackupLauncher.launch("antrenamente-aikido-auto.json") },
            ) { Text("Pornește copia automată") }
        } else {
            val last = backup.lastSuccess
            Text(
                when {
                    backup.failed -> "Ultima încercare nu a reușit. Dacă mesajul rămâne, oprește copia automată și pornește-o din nou."
                    last != null ->
                        "Ultima copie: ${formatDay(last.toLocalDate())}, " +
                            "ora ${last.toLocalTime().truncatedTo(ChronoUnit.MINUTES)}"
                    else -> "Se salvează prima copie…"
                },
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    AutoBackup.stop(context)
                    runCatching {
                        context.contentResolver.releasePersistableUriPermission(
                            Uri.parse(backup.uri),
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                        )
                    }
                    store.setBackupTarget(null)
                },
            ) { Text("Oprește copia automată") }
        }

        Text("Copie manuală", style = MaterialTheme.typography.titleMedium)
        Text("Datele stau pe telefon. O copie te ajută să le recuperezi dacă schimbi telefonul.")
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { exportLauncher.launch("antrenamente-aikido.json") },
        ) { Text("Salvează o copie") }
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            // Not filtered by type: Drive may not report a backup as JSON. Invalid files are rejected on read.
            onClick = { importLauncher.launch(arrayOf("*/*")) },
        ) { Text("Restaurează dintr-o copie") }
    }

    importUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { importUri = null },
            title = { Text("Restaurezi copia?") },
            text = { Text("Toate datele din aplicație vor fi înlocuite cu cele din fișier.") },
            confirmButton = {
                TextButton(onClick = {
                    val restored = runCatching {
                        val text = context.contentResolver.openInputStream(uri)!!.bufferedReader().use { it.readText() }
                        store.importBackup(text)
                        Reminders.syncActive(context)
                    }.isSuccess
                    Toast.makeText(
                        context,
                        if (restored) "Datele au fost restaurate" else "Fișierul nu este o copie validă",
                        Toast.LENGTH_LONG,
                    ).show()
                    importUri = null
                }) { Text("Înlocuiește") }
            },
            dismissButton = { TextButton(onClick = { importUri = null }) { Text("Renunță") } },
        )
    }
}
