package com.masss.smartwatchapp.presentation.accelerometermanager

import android.app.Notification
import android.app.NotificationChannel
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
import androidx.core.app.NotificationCompat
import com.masss.smartwatchapp.R


class AccelerometerRecordingService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null

    private var lastXValue: Float = 0f
    private var lastYValue: Float = 0f
    private var lastZValue: Float = 0f

    override fun onCreate() {
        Log.d("ACCELEROMETER_RECORDING_SERVICE", "AccelerometerRecordingService has started.")
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        startRecording()
    }

    override fun onDestroy() {
        Log.d("ACCELEROMETER_RECORDING_SERVICE", "AccelerometerRecordingService has stopped.")
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
                SensorManager.SENSOR_DELAY_GAME       // how often the sensor events are delivered (10 samples per second)
            )
        }
    }

    private fun startForegroundService() {
        val notificationChannelId = "ACCELEROMETER_SERVICE_CHANNEL"

        val channel = NotificationChannel(
            notificationChannelId,
            "Accelerometer Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("Accelerometer Service")
            .setContentText("Recording accelerometer data")
            .setSmallIcon(R.drawable.handomotic_notification)
            .build()

        startForeground(1, notification)
    }

    // Stop recording accelerometer data
    private fun stopRecording() {
        sensorManager.unregisterListener(this)     // Unregistering sensor listener
    }

    fun stopService() {
        stopForeground(true)
        stopSelf()
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