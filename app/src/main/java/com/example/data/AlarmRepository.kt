package com.example.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val alarmDao: AlarmDao) {
    val allAlarms: Flow<List<Alarm>> = alarmDao.getAllAlarms()
    val allHistory: Flow<List<AlarmHistory>> = alarmDao.getAllHistory()

    suspend fun getAlarmById(id: Int): Alarm? {
        return alarmDao.getAlarmById(id)
    }

    suspend fun insert(alarm: Alarm): Long {
        return alarmDao.insertAlarm(alarm)
    }

    suspend fun update(alarm: Alarm) {
        alarmDao.updateAlarm(alarm)
    }

    suspend fun delete(alarm: Alarm) {
        alarmDao.deleteAlarm(alarm)
    }

    suspend fun deleteById(id: Int) {
        alarmDao.deleteAlarmById(id)
    }

    suspend fun insertHistory(history: AlarmHistory): Long {
        return alarmDao.insertHistory(history)
    }

    suspend fun clearHistory() {
        alarmDao.clearHistory()
    }
}
