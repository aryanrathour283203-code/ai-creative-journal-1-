package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("ALARM_ID", -1)
        val label = intent.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val isVibrate = intent.getBooleanExtra("ALARM_VIBRATE", true)
        val isSnooze = intent.getBooleanExtra("IS_SNOOZE", false)

        Log.d("AlarmReceiver", "Alarm broadcast received! ID: $alarmId, label: $label, isSnooze: $isSnooze")

        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            putExtra("ALARM_VIBRATE", isVibrate)
            putExtra("IS_SNOOZE", isSnooze)
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Failed to start foreground service: ${e.message}")
        }
    }
}
