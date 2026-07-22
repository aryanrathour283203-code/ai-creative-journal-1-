package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarm_history")
data class AlarmHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val alarmId: Int,
    val alarmLabel: String,
    val alarmTime: String,
    val timestamp: Long = System.currentTimeMillis(),
    val action: String // "DISMISSED", "SNOOZED", "AUTO_SNOOZED", "TRIGGERED"
) {
    val formattedDate: String
        get() {
            val sdf = java.text.SimpleDateFormat("MMM dd, yyyy - hh:mm a", java.util.Locale.getDefault())
            return sdf.format(java.util.Date(timestamp))
        }

    val actionColorHex: Long
        get() = when (action) {
            "DISMISSED" -> 0xFF14B8A6 // Teal
            "SNOOZED" -> 0xFFF59E0B // Amber
            "AUTO_SNOOZED" -> 0xFFEF4444 // Red
            else -> 0xFF6366F1 // Indigo for triggered
        }

    val actionText: String
        get() = when (action) {
            "DISMISSED" -> "Woke Up / Dismissed"
            "SNOOZED" -> "Snoozed (30s)"
            "AUTO_SNOOZED" -> "Auto-Snoozed (Timeout)"
            else -> "Alarm Ringing"
        }
}
