package com.fajri.prioriti

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Task Prioriti"
        val description = intent.getStringExtra("description") ?: "You have a task to do!"
        val isJustNotification = intent.getBooleanExtra("isJustNotification", false)
        val taskId = intent.getIntExtra("taskId", System.currentTimeMillis().toInt())

        sendNotifAndAlarm(context, title, description, isJustNotification, taskId)
        Log.d("AlarmAktif", "Alarm triggered")
    }

    private fun sendNotifAndAlarm(context: Context, title: String, description: String, isJustNotification: Boolean, id: Int) {
            val channelId = if (isJustNotification) {
                "TASK_NOTIFICATION_CHANNEL"
            }else {
                "TASK_ALARM_CHANNEL"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val sound: String = if (isJustNotification) {
                "android.resource://${context.packageName}/raw/notif_music"
//                Log.d("AlarmHandler", "notif_music")
            }else {
                "android.resource://${context.packageName}/raw/alarm_music"
//                Log.d("AlarmHandler", "alarm_music")
            }

            Log.d("AlarmHandler", "After ${Uri.parse(sound)}")

            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_check_circle)
                .setContentTitle(title)
                .setContentText(description)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(Uri.parse(sound))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d("AlarmHandler", "Build Version : After ${Uri.parse(sound)}")

                val existingChannel = notificationManager.getNotificationChannel(channelId)
                if (existingChannel != null) {
                    notificationManager.deleteNotificationChannel(channelId)
                }

                val channel = NotificationChannel(channelId, "Task Notifications", NotificationManager.IMPORTANCE_HIGH).apply {
                    setSound(Uri.parse(sound), null)
                }

                notificationManager.createNotificationChannel(channel)
            }
            // Menggunakan taskId sebagai notification ID untuk memastikan unik

            notificationManager.notify(id, notificationBuilder.build())


    }


}