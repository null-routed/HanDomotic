package com.masss.smartwatchapp.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.masss.smartwatchapp.R
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerRecordingService
import kotlinx.coroutines.delay
import java.util.LinkedList
import java.util.Queue


class MainActivity : AppCompatActivity() {

    private val tag: String = "HanDomotic"
    // Defining how many sample to listen before passing the features to the classifier
    private val numListeningSamples: Int = 5
    // Defining the lenght of the windowing buffer array
    private val bufferSize: Int = 200
    private var counter: Int = 0

    private var appIsRecording: Boolean = false

    private val requiredPermissions = arrayOf(
        android.Manifest.permission.BODY_SENSORS,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    private val xTimeSeries = mutableListOf<Float>()
    private var yTimeSeries = mutableListOf<Float>()
    private var zTimeSeries = mutableListOf<Float>()
    private var recordingTimestamps = mutableListOf<Long>()

    /* Windowing buffering arrays */
    private val xWindow : LinkedList<Float> = LinkedList()
    private val yWindow: Queue<Float> = LinkedList()
    private val zWindow: Queue<Float> = LinkedList()

    // From the index 0 to the index numListeningSamples - 1 we have the incoming samples
    // From the index numListeningSamples to the end we have the old samples
    private fun addSample(xValue: Float, yValue: Float, zValue: Float){
        if(xWindow.size >= bufferSize){
            xWindow.removeFirst() // removing at the head
        }
        // The tail is added in any case
        xWindow.addLast(xValue)
    }

    private val accelerometerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
                        val xValue = intent?.getFloatExtra("xValue", 0f)
                        val yValue = intent?.getFloatExtra("yValue", 0f)
                        val zValue = intent?.getFloatExtra("zValue", 0f)
                        val timestamp = intent?.getLongExtra("timestamp", 0)

                        if (xValue != null && yValue != null && zValue != null && timestamp != null) {
                            // xTimeSeries.add(xValue)
                            // yTimeSeries.add(yValue)
                            // zTimeSeries.add(zValue)
                            // recordingTimestamps.add(timestamp)
                            Log.d(tag, "At $timestamp -> X: $xValue \t Y: $yValue \t Z: $zValue")
            }
                /*xTimeSeries.clear()
                yTimeSeries.clear()
                zTimeSeries.clear()*/
                addSample(xValue!!, yValue!!, zValue!!)
                if(counter < numListeningSamples)
                    counter++
                else{
                    Log.i("COUNTER", "Fine giro")
                    Log.i("PIPPO","xWindow contents: ${xWindow.joinToString(", ")}")
                    Log.i("PIPPO","xWindow size: ${xWindow.size}")
                    counter = 0
                }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //TODO: show app name following device's screen curvature ?
        val mainButton: Button = findViewById<Button>(R.id.mainButton)
        mainButton.text = resources.getString(R.string.start)

        Log.i("PIPPO","TEST MESSAGE")

        checkAndRequestPermissions()
    }


    private fun checkAndRequestPermissions() {
        val permissionsToBeRequested = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToBeRequested.isNotEmpty())
            requestPermissionsLauncher.launch(permissionsToBeRequested.toTypedArray())
        else
            onAllPermissionsGranted()
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.entries.all { it.value }
        if (allPermissionsGranted)
            onAllPermissionsGranted()
        else
            onPermissionsDenied()
    }

    private fun onAllPermissionsGranted() {
        val mainButton: Button = findViewById<Button>(R.id.mainButton)

        // setup onclick listeners
        /*
        TODO:
            -> if app is working, onclick makes it stop
            -> if app is not working, onclick makes it start
         */
        mainButton.setOnClickListener() {
            if (appIsRecording) {
                Log.d(tag, "Clicked. Stopping all main functionalities...")
                mainButton.text = resources.getString(R.string.stop)
                stopAppServices()
            } else {
                Log.d(tag, "Clicked. Starting all main functionalities...")
                mainButton.text = resources.getString(R.string.start)
                startAppServices()
            }
        }
    }

    private fun logAllTimeSeries() {
        Log.d("TIME_SERIES", "X: $xTimeSeries")
        Log.d("TIME_SERIES", "Y: $xTimeSeries")
        Log.d("TIME_SERIES", "Z: $xTimeSeries")
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(accelerometerReceiver, IntentFilter("AccelerometerData"),
            RECEIVER_EXPORTED)
        Log.d(tag, "An accelerometer receiver has been registered.")
    }

    private fun startAppServices() {
        // enable accelerometer data gathering
        appIsRecording = true
        Toast.makeText(this, "Started data gathering...", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        startService(accelRecordingIntent)
        Log.d(tag, "Started accelerometer data gathering")
    }

    private fun stopAppServices() {
        // stop accelerometer data gathering
        appIsRecording = false
        Toast.makeText(this, "Stopped data gathering.", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        stopService(accelRecordingIntent)
        Log.d(tag, "Stopped accelerometer data gathering")

        // stop classifier
    }

    private fun onPermissionsDenied() {
        /*
        TODO: make msg appear saying what are the permissions still needed
            for the app to work
            Show button to request those missing permissions
        */
    }
}