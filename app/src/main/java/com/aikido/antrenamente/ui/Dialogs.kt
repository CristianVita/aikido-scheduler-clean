package com.aikido.antrenamente.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.aikido.antrenamente.domain.Entry
import com.aikido.antrenamente.domain.Kind
import com.aikido.antrenamente.domain.Status
import com.aikido.antrenamente.domain.formatDay
import java.time.LocalDate

@Composable
fun DayDialog(
    date: LocalDate,
    entry: Entry?,
    onDismiss: () -> Unit,
    onAnswer: (LocalDate, Status?) -> Unit,
    onEventRemoved: (LocalDate) -> Unit,
    onEventSaved: (LocalDate, LocalDate, Kind, String) -> Unit,
    onVacationRemoved: (LocalDate) -> Unit,
) {
    if (entry?.kind == Kind.VACATION) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(formatDay(date)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Vacanță — nu primești întrebări în această perioadă.")
                    TextButton(onClick = {
                        onVacationRemoved(date)
                        onDismiss()
                    }) { Text("Șterge vacanța") }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Gata") } },
        )
        return
    }

    var editingEvent by remember { mutableStateOf(false) }
    if (editingEvent) {
        EventDialog(
            initialDate = date,
            existing = entry?.takeIf { it.title != null },
            onDismiss = { editingEvent = false },
            onSave = { from, to, kind, title ->
                onEventSaved(from, to, kind, title)
                editingEvent = false
            },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(formatDay(date)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                entry?.title?.let { Text(it) }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip("Am fost", entry?.status == Status.PRESENT) { onAnswer(date, Status.PRESENT) }
                    StatusChip("N-am fost", entry?.status == Status.ABSENT) { onAnswer(date, Status.ABSENT) }
                }
                StatusChip("Nu s-a ținut", entry?.status == Status.NOT_HELD) { onAnswer(date, Status.NOT_HELD) }
                if (entry?.status != null) {
                    TextButton(onClick = { onAnswer(date, null) }) { Text("Șterge răspunsul") }
                }
                OutlinedButton(onClick = { editingEvent = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (entry?.title != null) "Editează stagiul sau examenul" else "Adaugă stagiu sau examen")
                }
                if (entry?.title != null) {
                    TextButton(onClick = {
                        onEventRemoved(date)
                        onDismiss()
                    }) { Text("Șterge stagiul sau examenul") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Gata") } },
    )
}

@Composable
private fun StatusChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
}

@Composable
fun EventDialog(
    initialDate: LocalDate,
    existing: Entry? = null,
    onDismiss: () -> Unit,
    onSave: (LocalDate, LocalDate, Kind, String) -> Unit,
) {
    var kind by remember { mutableStateOf(existing?.kind?.takeIf { it != Kind.TRAINING } ?: Kind.STAGE) }
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var from by remember { mutableStateOf(initialDate) }
    var to by remember { mutableStateOf(initialDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Editează" else "Adaugă") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(kind == Kind.STAGE, { kind = Kind.STAGE }, label = { Text("Stagiu") })
                    FilterChip(kind == Kind.EXAM, { kind = Kind.EXAM }, label = { Text("Examen") })
                    if (existing == null) {
                        FilterChip(kind == Kind.VACATION, { kind = Kind.VACATION }, label = { Text("Vacanță") })
                    }
                }
                if (kind != Kind.VACATION) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nume, de ex. Stagiu Perpelici") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Text("Nu primești întrebări zilnice în acest interval. Reîncepe automat după ultima zi.")
                }
                if (existing == null) {
                    DateButton("De la", from) {
                        from = it
                        if (to.isBefore(it)) to = it
                    }
                    DateButton("Până la", to) { to = if (it.isBefore(from)) from else it }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = kind == Kind.VACATION || title.isNotBlank(),
                onClick = { onSave(from, to, kind, title.trim()) },
            ) { Text("Salvează") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Renunță") } },
    )
}

@Composable
private fun DateButton(label: String, date: LocalDate, onPicked: (LocalDate) -> Unit) {
    val context = LocalContext.current
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            DatePickerDialog(
                context,
                { _, year, month, day -> onPicked(LocalDate.of(year, month + 1, day)) },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth,
            ).show()
        },
    ) { Text("$label: ${formatDay(date)} ${date.year}") }
}
