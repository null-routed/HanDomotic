package com.masss.smartwatchapp.presentation.accelerometermanager

import android.app.Service
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.IBinder
import android.util.Log

class AccelerometerRecordingService : Service() {

    private var LOG_TAG: String = "HanDomotic@AccelerometerRecordingService"

    private lateinit var sensorManager: SensorManager
    private var accelerometerSensor: Sensor? = null

    private var lastXValue: Float = 0f
    private var lastYValue: Float = 0f
    private var lastZValue: Float = 0f

    override fun onCreate() {
        Log.d(LOG_TAG, "Recording service has started.")
        super.onCreate()

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        Log.d(LOG_TAG, "Obtained an accelerometer reference: " + accelerometerSensor.toString())

        startRecording()
    }

    override fun onDestroy() {
        Log.d(LOG_TAG, "RecordingService has stopped.")
        stopRecording()         // stop recording data before destroying the service
        super.onDestroy()
    }

    // Start recording accelerometer data
    private fun startRecording() {
        accelerometerSensor?.also { sensor ->       // Registering sensor listener
            sensorManager.registerListener(
                accelerometerListener,
                sensor,
                SensorManager.SENSOR_DELAY_GAME       // how often the sensor events are delivered (10 samples per second)
            )
        }
    }

    // Stop recording accelerometer data
    private fun stopRecording() {
        sensorManager.unregisterListener(accelerometerListener)     // Unregistering sensor listener
    }

    private fun broadcastAccelerometerData(xValue: Float, yValue: Float, zValue: Float, timestamp: Long) {
        val intent = Intent("AccelerometerData")
        intent.putExtra("xValue", xValue)
        intent.putExtra("yValue", yValue)
        intent.putExtra("zValue", zValue)
        intent.putExtra("timestamp", timestamp)
        sendBroadcast(intent)
    }

    private val accelerometerListener = object : SensorEventListener {

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
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}