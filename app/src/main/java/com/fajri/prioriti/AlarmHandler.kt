package com.fajri.prioriti

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.fajri.prioriti.data.model.Task

class AlarmHandler(private val context: Context) {
    private val alarmHandler = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun scheduleAlarm(task: Task, offsetMillis: Long = 0L, isJustNotification: Boolean = false) {
        val alarmTime = task.timestamp + offsetMillis

        if (alarmTime <= System.currentTimeMillis()) {
            return
        }

        val requestCode = if (offsetMillis == 0L) task.id else task.id + 1000

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("taskId", requestCode)
            putExtra("title", task.title)
            putExtra("description", task.description)
            putExtra("isJustNotification", isJustNotification)
        }

        Log.d("AlarmHandler", "Scheduling alarm for task: ${requestCode} - ${task.title}")
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmHandler.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            alarmTime,
            pendingIntent
        )

//        alarmHandler.setRepeating(
//            AlarmManager.RTC_WAKEUP,
//            SystemClock.elapsedRealtime() + 5000,
//            1000 * 60,
//            pendingIntent
//        )
    }

    fun cancelAlarm(task: Task) {
        // To cancel alarms, you need to cancel each PendingIntent with its unique request code
        cancelExactAlarm(task, 0L)
        if (task.priority == "High") {
            cancelExactAlarm(task, 60_000)
        }
    }

    private fun cancelExactAlarm(task: Task, offsetMillis: Long = 0L) {
//        val alarmTime = task.timestamp + offsetMillis

        val intent = Intent(context, AlarmReceiver::class.java)

        val requestCode = if (offsetMillis == 0L) task.id else task.id + 1000

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmHandler.cancel(pendingIntent)
    }


}