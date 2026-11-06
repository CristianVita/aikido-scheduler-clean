package com.aikido.antrenamente

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aikido.antrenamente.reminder.Reminders
import com.aikido.antrenamente.ui.AikidoApp

class MainActivity : ComponentActivity() {
    private var resumedAt by mutableStateOf(0L)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Reminders.schedule(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { AikidoApp(resumedAt) }
    }

    override fun onResume() {
        super.onResume()
        resumedAt = System.currentTimeMillis()
    }
}
