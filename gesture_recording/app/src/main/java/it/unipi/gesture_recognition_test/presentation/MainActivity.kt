package it.unipi.gesture_recognition_test.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import it.unipi.gesture_recognition_test.R
import android.Manifest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class MainActivity : AppCompatActivity() {

    private var isRecording = false
    private var gestureCounter: Int = 0
    private var xTimeSeries = mutableListOf<Float>()
    private var yTimeSeries = mutableListOf<Float>()
    private var zTimeSeries = mutableListOf<Float>()
    private var recordingTimestamps = mutableListOf<Long>()
    private lateinit var file: File
    private val gestures = mutableListOf<JSONObject>()

    private val whatAmIRecording: String = "random_movement_"

    private val accelerometerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {        // extract accelerometer data from intent
            val xValue = intent?.getFloatExtra("xValue", 0f)
            val yValue = intent?.getFloatExtra("yValue", 0f)
            val zValue = intent?.getFloatExtra("zValue", 0f)
            val timestamp = intent?.getLongExtra("timestamp", 0)

            if (xValue != null && yValue != null && zValue != null && timestamp != null) {
                // Log.d("ACCELEROMETER_DATA", "At $timestamp -> X: $xValue \t Y: $yValue \t Z: $zValue")
                xTimeSeries.add(xValue)
                yTimeSeries.add(yValue)
                zTimeSeries.add(zValue)
                recordingTimestamps.add(timestamp)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        file = File(filesDir, whatAmIRecording + "_data.json")
        if(file.exists()) {
            val deleted = file.delete()
            if (deleted) Log.d("MainActivity", "File deleted successfully")
            else Log.d("MainActivity", "Failed to delete file")
        }else
            Log.d("MainActivity", "No files to delete")

        val startRecordingButton: Button = findViewById(R.id.startRecordingButton)
        startRecordingButton.isEnabled = checkPermissions()         // checking all the needed permissions at runtime

        startRecordingButton.setOnClickListener{
            if(isRecording) {
                startRecordingButton.text = "START GESTURE RECORDING"
                stopRecordingService()
            } else {
                startRecordingButton.text = "STOP GESTURE RECORDING"
                startRecordingService()
            }
        }
    }


    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BODY_SENSORS,
        )

        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }


    override fun onResume() {
        super.onResume()                // registering the accelerometer receiver
        Log.d("MainActivity", "onResume() has been triggered!")
        registerReceiver(accelerometerReceiver, IntentFilter("AccelerometerData"))
        Log.d("MainActivity", "Broadcast receiver registered successfully.")
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(accelerometerReceiver)   // Unregister the accelerometer receiver
        Log.d("MainActivity", "Broadcast receiver unregistered successfully.")
    }

    private fun startRecordingService() {
        isRecording = true
//        Toast.makeText(this, "Gesture recording started", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, RecordingService::class.java)
        startService(intent)
    }

    private fun stopRecordingService() {
        isRecording = false

//        Toast.makeText(this, "Gesture recording stopped", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, RecordingService::class.java)
        stopService(intent)

        // saving to file in JSON format
        val jsonObject = JSONObject()
        jsonObject.put("GestureID", whatAmIRecording + "_$gestureCounter")
        appendJSONArray(jsonObject, "timestamps", null, recordingTimestamps)
        appendJSONArray(jsonObject, "xTimeSeries", xTimeSeries, null)
        appendJSONArray(jsonObject, "yTimeSeries", yTimeSeries, null)
        appendJSONArray(jsonObject, "zTimeSeries", zTimeSeries, null)

        gestures.add(jsonObject)
        saveGesturesToFile()

        xTimeSeries.clear()         // flushing previous data for new run
        yTimeSeries.clear()
        zTimeSeries.clear()
        recordingTimestamps.clear()
        gestureCounter += 1

        Log.d("GESTURE", "RECORDED $gestureCounter GESTURES")
    }

    private fun saveGesturesToFile() {
        val jsonArray = JSONArray()
        gestures.forEach { gesture ->
            jsonArray.put(gesture)
        }
        file.writeText(jsonArray.toString())
    }

    // Function to append a JSONArray to a JSONObject
    private fun appendJSONArray(jsonObject: JSONObject, key: String, timeSeries: List<Float>?, timestamps: List<Long>?) {
        val existingData = jsonObject.optJSONArray(key)

        val newData = if (timeSeries != null) {
            JSONArray(timeSeries)
        } else if (timestamps != null) {
            val jsonArray = JSONArray()
            for (timestamp in timestamps) {
                jsonArray.put(timestamp)
            }
            jsonArray
        } else
            JSONArray()

        if (existingData != null) {
            // If the key already exists, append the new data to the existing array
            for (i in 0 until newData.length()) {
                existingData.put(newData[i])
            }
        } else {
            // If the key doesn't exist, create a new array
            jsonObject.put(key, newData)
        }
    }
}
