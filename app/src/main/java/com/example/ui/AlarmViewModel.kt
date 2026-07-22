package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Alarm
import com.example.data.AlarmDatabase
import com.example.data.AlarmHistory
import com.example.data.AlarmRepository
import com.example.receiver.AlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AlarmDatabase.getDatabase(application)
    private val repository = AlarmRepository(database.alarmDao())
    private val scheduler = AlarmScheduler(application)
    private val sharedPrefs = application.getSharedPreferences("wakecall_prefs", Context.MODE_PRIVATE)

    val alarms: StateFlow<List<Alarm>> = repository.allAlarms
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val history: StateFlow<List<AlarmHistory>> = repository.allHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Settings States
    private val _snoozeDuration = MutableStateFlow(sharedPrefs.getInt("snooze_duration", 30))
    val snoozeDuration: StateFlow<Int> = _snoozeDuration.asStateFlow()

    private val _voicePersonality = MutableStateFlow(sharedPrefs.getString("voice_personality", "Serene AI Assistant") ?: "Serene AI Assistant")
    val voicePersonality: StateFlow<String> = _voicePersonality.asStateFlow()

    private val _customGreeting = MutableStateFlow(sharedPrefs.getString("custom_greeting", "Rise and shine! Today is a fresh canvas.") ?: "Rise and shine! Today is a fresh canvas.")
    val customGreeting: StateFlow<String> = _customGreeting.asStateFlow()

    private val _themeMode = MutableStateFlow(sharedPrefs.getString("theme_mode", "dark") ?: "dark")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _customRingtone = MutableStateFlow(sharedPrefs.getString("custom_ringtone", "Classic Alarm") ?: "Classic Alarm")
    val customRingtone: StateFlow<String> = _customRingtone.asStateFlow()

    private val _vibrationPattern = MutableStateFlow(sharedPrefs.getString("vibration_pattern", "Continuous") ?: "Continuous")
    val vibrationPattern: StateFlow<String> = _vibrationPattern.asStateFlow()

    private val _gradualVolume = MutableStateFlow(sharedPrefs.getBoolean("gradual_volume", true))
    val gradualVolume: StateFlow<Boolean> = _gradualVolume.asStateFlow()

    fun addAlarm(hour: Int, minute: Int, label: String, isVibrate: Boolean, daysOfWeek: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val alarm = Alarm(
                hour = hour,
                minute = minute,
                label = label.ifBlank { "WakeCall AI" },
                isVibrate = isVibrate,
                daysOfWeek = daysOfWeek,
                isEnabled = true
            )
            val id = repository.insert(alarm).toInt()
            val insertedAlarm = alarm.copy(id = id)
            scheduler.schedule(insertedAlarm)
        }
    }

    fun updateAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(alarm)
            if (alarm.isEnabled) {
                scheduler.schedule(alarm)
            } else {
                scheduler.cancel(alarm)
            }
        }
    }

    fun deleteAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduler.cancel(alarm)
            repository.delete(alarm)
        }
    }

    fun toggleAlarm(alarm: Alarm) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repository.update(updated)
            if (updated.isEnabled) {
                scheduler.schedule(updated)
            } else {
                scheduler.cancel(updated)
            }
        }
    }

    // Alarm History actions
    fun addHistoryEntry(alarmId: Int, label: String, time: String, action: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val historyEntry = AlarmHistory(
                alarmId = alarmId,
                alarmLabel = label,
                alarmTime = time,
                action = action
            )
            repository.insertHistory(historyEntry)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearHistory()
        }
    }

    // Settings actions
    fun updateSnoozeDuration(seconds: Int) {
        sharedPrefs.edit().putInt("snooze_duration", seconds).apply()
        _snoozeDuration.value = seconds
    }

    fun updateVoicePersonality(personality: String) {
        sharedPrefs.edit().putString("voice_personality", personality).apply()
        _voicePersonality.value = personality
    }

    fun updateCustomGreeting(greeting: String) {
        sharedPrefs.edit().putString("custom_greeting", greeting).apply()
        _customGreeting.value = greeting
    }

    fun updateThemeMode(mode: String) {
        sharedPrefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun updateCustomRingtone(ringtone: String) {
        sharedPrefs.edit().putString("custom_ringtone", ringtone).apply()
        _customRingtone.value = ringtone
    }

    fun updateVibrationPattern(pattern: String) {
        sharedPrefs.edit().putString("vibration_pattern", pattern).apply()
        _vibrationPattern.value = pattern
    }

    fun updateGradualVolume(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("gradual_volume", enabled).apply()
        _gradualVolume.value = enabled
    }
}
