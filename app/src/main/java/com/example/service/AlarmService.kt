package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.receiver.AlarmScheduler
import com.example.ui.AlarmRingActivity

class AlarmService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var volumeTimer: java.util.Timer? = null

    companion object {
        const val CHANNEL_ID = "alarm_channel"
        const val NOTIFICATION_ID = 4444
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("ALARM_ID", -1) ?: -1
        val label = intent?.getStringExtra("ALARM_LABEL") ?: "Alarm"
        val isVibrate = intent?.getBooleanExtra("ALARM_VIBRATE", true) ?: true
        val isSnooze = intent?.getBooleanExtra("IS_SNOOZE", false) ?: false

        Log.d("AlarmService", "Starting foreground service for alarm ID $alarmId, isSnooze: $isSnooze")

        // Read premium preferences
        val sharedPrefs = getSharedPreferences("wakecall_prefs", Context.MODE_PRIVATE)
        val selectedRingtone = sharedPrefs.getString("custom_ringtone", "Classic Alarm") ?: "Classic Alarm"
        val vibrationPattern = sharedPrefs.getString("vibration_pattern", "Continuous") ?: "Continuous"
        val isGradualVolume = sharedPrefs.getBoolean("gradual_volume", true)

        // Play continuous ringtone
        startRingtone(isGradualVolume, selectedRingtone)

        // Play continuous vibration
        if (isVibrate) {
            startVibration(vibrationPattern)
        }

        // Prepare full-screen intent activity
        val ringActivityIntent = Intent(this, AlarmRingActivity::class.java).apply {
            putExtra("ALARM_ID", alarmId)
            putExtra("ALARM_LABEL", label)
            putExtra("ALARM_VIBRATE", isVibrate)
            putExtra("IS_SNOOZE", isSnooze)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            alarmId,
            ringActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build high priority notifications
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(if (isSnooze) "Snoozed Alarm Ringing" else "Alarm Ringing")
            .setContentText(label)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Also launch the activity directly to wake screen up
        startActivity(ringActivityIntent)

        return START_NOT_STICKY
    }

    private fun startRingtone(isGradualVolume: Boolean, selectedRingtone: String) {
        try {
            var ringtoneUri: Uri? = null
            when (selectedRingtone) {
                "Gentle Melody" -> ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
                "Simple Alert" -> ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_NOTIFICATION)
                else -> ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            }
            if (ringtoneUri == null) {
                ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, ringtoneUri!!)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                prepare()
                
                if (isGradualVolume) {
                    setVolume(0.05f, 0.05f)
                } else {
                    setVolume(1.0f, 1.0f)
                }
                start()
            }

            if (isGradualVolume) {
                var currentVolume = 0.05f
                volumeTimer = java.util.Timer().apply {
                    scheduleAtFixedRate(object : java.util.TimerTask() {
                        override fun run() {
                            if (currentVolume < 1.0f) {
                                currentVolume += 0.05f
                                if (currentVolume > 1.0f) currentVolume = 1.0f
                                mediaPlayer?.setVolume(currentVolume, currentVolume)
                                Log.d("AlarmService", "Gradual volume: $currentVolume")
                            } else {
                                cancel()
                            }
                        }
                    }, 1000, 2000) // Increase volume every 2 seconds
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to start ringtone: ${e.message}")
        }
    }

    private fun startVibration(patternName: String) {
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val timings = when (patternName) {
                "Heartbeat" -> longArrayOf(0, 150, 100, 150, 600)
                "Staccato" -> longArrayOf(0, 80, 80, 80, 80, 80, 400)
                "Zen Wave" -> longArrayOf(0, 1200, 400)
                else -> longArrayOf(0, 600, 400) // "Continuous"
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = when (patternName) {
                    "Heartbeat" -> intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
                    "Staccato" -> intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
                    "Zen Wave" -> intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE / 2, 0)
                    else -> intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0)
                }
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(timings, 0)
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to start vibration: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("AlarmService", "Destroying foreground service")
        volumeTimer?.cancel()
        volumeTimer = null

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alarm Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Urgent notifications for ringing alarms"
                setSound(null, null)
                enableVibration(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
