package com.masss.smartwatchapp.presentation.accelerometermanager

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log
import com.masss.smartwatchapp.R
import com.masss.smartwatchapp.presentation.utilities.NotificationHelper


class AccelerometerRecordingService : Service(), SensorEventListener {

    private val TAG = "ACCELEROMETER_RECORDING_SERVICE"

    private var notificationHelper = NotificationHelper()
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val NOTIFICATION_CHANNEL_ID = "ACCELEROMETER_SERVICE_CHANNEL"
        private const val NOTIFICATION_CHANNEL_NAME = "Accelerometer Service Channel"
    }

    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null

    private var lastXValue: Float = 0f
    private var lastYValue: Float = 0f
    private var lastZValue: Float = 0f

    override fun onCreate() {
        Log.d(TAG, "AccelerometerRecordingService has started.")
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        startRecording()
    }

    override fun onDestroy() {
        Log.d(TAG, "AccelerometerRecordingService has stopped.")
        stopRecording()         // stop recording data before destroying the service
        super.onDestroy()
    }

    // Start recording accelerometer data
    private fun startRecording() {
        startForegroundService()

        accelerometerSensor?.also { sensor ->       // Registering sensor listener
            sensorManager.registerListener(
                this,
                sensor,
                SensorManager.SENSOR_DELAY_GAME       // how often the sensor events are delivered
            )
        }
    }

    private fun startForegroundService() {
        val notificationChannel = notificationHelper.getNotificationChannelObject(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(notificationChannel)

        val notification = notificationHelper.createNotification(
            NOTIFICATION_CHANNEL_ID,
            "Accelerometer Service",
            "Recording accelerometer data...",
            R.drawable.handomotic_notification,
            this
        )

        startForeground(NOTIFICATION_ID, notification)
    }

    // Stop recording accelerometer data
    private fun stopRecording() {
        sensorManager.unregisterListener(this)     // Unregistering sensor listener
    }


    private fun broadcastAccelerometerData(xValue: Float, yValue: Float, zValue: Float, timestamp: Long) {
        val intent = Intent("AccelerometerData")
        intent.putExtra("xValue", xValue)
        intent.putExtra("yValue", yValue)
        intent.putExtra("zValue", zValue)
        intent.putExtra("timestamp", timestamp)
        sendBroadcast(intent)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // Handle accelerometer data
        event?.let {
            val xValue = it.values[0]
            val yValue = it.values[1]
            val zValue = it.values[2]

            val timestamp = System.currentTimeMillis()

            // Store the latest values
            lastXValue = xValue
            lastYValue = yValue
            lastZValue = zValue

            // Broadcast the accelerometer data
            broadcastAccelerometerData(xValue, yValue, zValue, timestamp)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        return
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}