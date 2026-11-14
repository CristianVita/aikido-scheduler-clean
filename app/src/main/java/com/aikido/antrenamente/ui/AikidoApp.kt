package com.aikido.antrenamente.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.aikido.antrenamente.data.Store
import com.aikido.antrenamente.domain.Kind
import com.aikido.antrenamente.domain.Status
import com.aikido.antrenamente.reminder.Reminders
import java.time.LocalDate
import java.time.LocalDateTime

val PresentColor = Color(0xFF2E9E5B)
val AbsentColor = Color(0xFFD64545)
val NotHeldColor = Color(0xFF7A7F8C)
val ExamColor = Color(0xFFF2B01E)
val OnExamColor = Color(0xFF1F2A44)
val VacationColor = Color(0xFF3B82C4)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1F2A44),
    secondaryContainer = Color(0xFFDDE3F3),
    surface = Color(0xFFFAFAFC),
    background = Color(0xFFFAFAFC),
)
private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9C6EA),
    onPrimary = Color(0xFF1F2A44),
)

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Acasă", Icons.Filled.Home),
    Calendar("Calendar", Icons.Filled.DateRange),
    Settings("Setări", Icons.Filled.Settings),
}

@Composable
fun AikidoApp(resumedAt: Long) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors) {
        val context = LocalContext.current
        val store = remember { Store.get(context) }
        val entries by store.entries.collectAsState()
        val backup by store.backupStatus.collectAsState()
        var tab by rememberSaveable { mutableStateOf(Tab.Home) }
        // Re-read the clock when returning to the app or switching tabs (e.g. after changing the time).
        val now = remember(resumedAt, tab) { LocalDateTime.now() }
        val today = now.toLocalDate()
        var newEventDate by remember { mutableStateOf<LocalDate?>(null) }

        val answer: (LocalDate, Status?) -> Unit = { date, status ->
            store.setStatus(date, status)
            Reminders.syncActive(context)
        }
        val saveEvent: (LocalDate, LocalDate, Kind, String) -> Unit =
            { from, to, kind, title ->
                if (kind == Kind.VACATION) {
                    store.setVacation(from, to)
                } else {
                    store.putEvent(from, to, kind, title)
                }
                Reminders.syncActive(context)
            }
        val removeVacation: (LocalDate) -> Unit = { date ->
            store.removeVacation(date)
            Reminders.syncActive(context)
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
            floatingActionButton = {
                if (tab != Tab.Settings) {
                    FloatingActionButton(onClick = { newEventDate = today }) {
                        Icon(Icons.Filled.Add, contentDescription = "Adaugă stagiu sau examen")
                    }
                }
            },
        ) { padding ->
            val modifier = Modifier.padding(padding)
            when (tab) {
                Tab.Home -> HomeScreen(entries, store.trackingStart, now, store.reminderTime, backup.failed, answer, modifier)
                Tab.Calendar -> CalendarScreen(
                    entries = entries,
                    trackingStart = store.trackingStart,
                    now = now,
                    questionTime = store.reminderTime,
                    onAnswer = answer,
                    onEventRemoved = { date ->
                        store.removeEvent(date)
                        Reminders.syncActive(context)
                    },
                    onEventSaved = saveEvent,
                    onVacationRemoved = removeVacation,
                    modifier = modifier,
                )
                Tab.Settings -> SettingsScreen(store, modifier)
            }
        }

        newEventDate?.let { date ->
            EventDialog(
                initialDate = date,
                onDismiss = { newEventDate = null },
                onSave = { from, to, kind, title ->
                    saveEvent(from, to, kind, title)
                    newEventDate = null
                },
            )
        }
    }
}
