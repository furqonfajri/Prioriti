package com.fajri.prioriti

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.fajri.prioriti.data.local.AppDatabase
import com.fajri.prioriti.data.model.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            resetAlarms(context)
        }
    }

    private fun resetAlarms(context: Context) {
        val taskDao = AppDatabase.getInstance(context).taskDao()
        val alarmHandler = AlarmHandler(context)

        // Mengambil waktu saat ini untuk filter tugas mendatang
        val currentTime = System.currentTimeMillis()

        // Mengambil tugas mendatang dari Room database
        // Pastikan fungsi getTasksAfter adalah suspend function dan dipanggil di coroutine
        CoroutineScope(Dispatchers.IO).launch {
            val futureTasks = taskDao.getTaskAfter(currentTime)
            futureTasks.forEach { task ->
                when(task.priority) {
                    "Low" -> {
                        alarmHandler.scheduleAlarm(task, isJustNotification = true)
                    }
                    "Medium" -> {
                        alarmHandler.scheduleAlarm(task)
                    }
                    "High" -> {
                        alarmHandler.scheduleAlarm(task)
                        alarmHandler.scheduleAlarm(task, offsetMillis = 60_000)
                    }
                }
            }
        }
    }
}