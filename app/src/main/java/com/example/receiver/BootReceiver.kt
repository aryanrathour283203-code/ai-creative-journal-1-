package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Device boot completed, restoring active alarms...")
            
            val pendingResult = goAsync()
            val database = AlarmDatabase.getDatabase(context)
            val scheduler = AlarmScheduler(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = database.alarmDao().getAllAlarms().first()
                    alarms.forEach { alarm ->
                        if (alarm.isEnabled) {
                            scheduler.schedule(alarm)
                            Log.d("BootReceiver", "Restored active alarm ID ${alarm.id} at ${alarm.hour}:${alarm.minute}")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error restoring alarms on boot: ${e.message}")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
