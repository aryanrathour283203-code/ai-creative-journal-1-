package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int, // 0 - 23
    val minute: Int, // 0 - 59
    val label: String,
    val isEnabled: Boolean = true,
    val isVibrate: Boolean = true,
    val ringtoneUri: String? = null,
    val daysOfWeek: String = "" // e.g. "1,2,3,4,5" (1=Mon, 7=Sun) or empty for once
) {
    val formattedTime: String
        get() {
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format("%02d:%02d %s", displayHour, minute, amPm)
        }

    val repeatingDaysText: String
        get() {
            if (daysOfWeek.isBlank()) return "Once"
            val days = daysOfWeek.split(",").mapNotNull { it.toIntOrNull() }.sorted()
            if (days.size == 7) return "Every day"
            if (days == listOf(1, 2, 3, 4, 5)) return "Weekdays"
            if (days == listOf(6, 7)) return "Weekends"
            
            val dayNames = listOf("", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            return days.joinToString(", ") { dayNames.getOrElse(it) { "" } }
        }
}
