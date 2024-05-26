package com.masss.smartwatchapp.presentation.utilities

import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat


class NotificationHelper {

    private val TAG = "NOTIFICATION_HELPER"

    fun getNotificationChannelObject(
        notificationChannelId: String,
        notificationChannelName: String,
        notificationImportance: Int
    ): NotificationChannel {
        Log.i(TAG, "Created a notification channel: (id, name) = ($notificationChannelId, $notificationChannelName)")
        return NotificationChannel(
            notificationChannelId,
            notificationChannelName,
            notificationImportance
        )
    }

    fun createNotification(
        notificationChannelId: String,
        title: String,
        contentText: String,
        iconDrawable: Int,
        context: Context
    ): Notification {
        Log.i(TAG, "Created a notification for channel: $notificationChannelId")
        return NotificationCompat.Builder(context, notificationChannelId)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(iconDrawable)
            .build()
    }
}
