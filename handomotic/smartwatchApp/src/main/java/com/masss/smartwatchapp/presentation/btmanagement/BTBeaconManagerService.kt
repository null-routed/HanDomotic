package com.masss.smartwatchapp.presentation.btmanagement

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.masss.handomotic.BTBeaconManager
import com.masss.smartwatchapp.R

class BTBeaconManagerService : Service() {

    private lateinit var beaconManager: BTBeaconManager

    override fun onCreate() {
        super.onCreate()
        beaconManager = BTBeaconManager(applicationContext, mutableMapOf())
        startForegroundService()
        beaconManager.startScanning()
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "beacon_scanning_service_channel"

        val channel = NotificationChannel(
            channelId,
            "Beacon Scanning Service",
            NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Beacon Scanning")
            .setContentText("Scanning for beacons in the background")
            .setSmallIcon(R.drawable.handomotic_notification)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        beaconManager.stopScanning()
        beaconManager.destroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}