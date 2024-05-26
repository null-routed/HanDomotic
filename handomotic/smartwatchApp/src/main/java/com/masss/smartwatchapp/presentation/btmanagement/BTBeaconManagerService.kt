package com.masss.smartwatchapp.presentation.btmanagement

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.masss.handomotic.BTBeaconManager
import com.masss.smartwatchapp.R
import com.masss.smartwatchapp.presentation.utilities.NotificationHelper


class BTBeaconManagerService : Service() {

    private lateinit var beaconManager: BTBeaconManager

    private val notificationHelper = NotificationHelper()
    companion object {
        private const val NOTIFICATION_ID = 3
        private const val NOTIFICATION_CHANNEL_ID = "BT_BEACON_SCANNING_SERVICE_CHANNEL"
        private const val NOTIFICATION_CHANNEL_NAME = "BT Beacon Scanning Service Channel"
    }

    override fun onCreate() {
        super.onCreate()
        beaconManager = BTBeaconManager(applicationContext, mutableMapOf())
        startForegroundService()
        beaconManager.startScanning()
    }

    private fun startForegroundService() {
        val notificationChannel = notificationHelper.getNotificationChannelObject(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)

        val notification = notificationHelper.createNotification(
            NOTIFICATION_CHANNEL_ID,
            "BT Beacon Scanning Service",
            "Scanning for Bluetooth beacons...",
            R.drawable.handomotic_notification,
            this
        )

        startForeground(NOTIFICATION_ID, notification)
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