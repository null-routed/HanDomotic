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
import com.masss.smartwatchapp.presentation.classifier.SVMClassifier
import java.util.LinkedList


class MainActivity : AppCompatActivity() {

    private val LOG_TAG: String = "HanDomotic"

    // Defining how many sample to drop from a call to the classifier to another
    private val classificationFrequency: Int = 3

    // Defining the length of the windowing buffer array
    private val bufferSize: Int = 50
    private var counter: Int = 0

    // Tracker for the app state
    private var appIsRecording: Boolean = false

    private val requiredPermissions = arrayOf(
        android.Manifest.permission.BODY_SENSORS,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    /* Windowing buffering arrays */
    private val xWindow : LinkedList<Float> = LinkedList()
    private val yWindow: LinkedList<Float> = LinkedList()
    private val zWindow: LinkedList<Float> = LinkedList()

    /* Classifier*/
    private lateinit var svmClassifier: SVMClassifier

    // From the index 0 to the index numListeningSamples - 1 we have the incoming samples
    // From the index numListeningSamples to the end we have the old samples
    private fun addSample(xValue: Float, yValue: Float, zValue: Float){
        if(xWindow.size >= bufferSize){
            xWindow.removeFirst() // removing at the head
            yWindow.removeFirst() // we can remove also y and z as they are of the same size of x
            zWindow.removeFirst()
        }
        // The tail is added in any case
        xWindow.addLast(xValue)
        yWindow.addLast(yValue)
        zWindow.addLast(zValue)
    }

    // Broadcasts features to the classifier
    private fun broadcastFeatures(featuresList: List<Float>){
        val intent = Intent("FeatureList")
        for(i in 0 until featuresList.size){
            intent.putExtra("feature_$i", featuresList.get(i))
        }
        Log.d(LOG_TAG, "Sending features to the classifier...")
        sendBroadcast(intent)
    }

    private val accelerometerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val xValue = intent?.getFloatExtra("xValue", 0f)
            val yValue = intent?.getFloatExtra("yValue", 0f)
            val zValue = intent?.getFloatExtra("zValue", 0f)
            val timestamp = intent?.getLongExtra("timestamp", 0)

            if (xValue != null && yValue != null && zValue != null && timestamp != null) {
                // Log.d(LOG_TAG, "At $timestamp -> X: $xValue \t Y: $yValue \t Z: $zValue")
                // Makes the window slide: writes in xWindow, yWindow, zWindow
                addSample(xValue, yValue, zValue)
            }
            if(counter < classificationFrequency)
                counter++
            else{           // every 'classificationFrequency' samples features get extracted and sent to the classifier
                val xFeatures = FeatureExtractor.extractFeatures(xWindow.toFloatArray())
                val yFeatures = FeatureExtractor.extractFeatures(yWindow.toFloatArray())
                val zFeatures = FeatureExtractor.extractFeatures(zWindow.toFloatArray())
                val allFeatures  =
                    (xFeatures + yFeatures + zFeatures) +
                            FeatureExtractor.calculateCorrelations(
                                xWindow.toFloatArray(),
                                yWindow.toFloatArray(),
                                zWindow.toFloatArray()
                            )
                Log.i("ALL_FEATURES", allFeatures.joinToString(", "))
                counter = 0
                broadcastFeatures(allFeatures)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // setting up onclick listeners on activity creation
        val mainButton: Button = findViewById(R.id.mainButton)
        mainButton.setOnClickListener() {
            if (appIsRecording) {
                Log.d(LOG_TAG, "Clicked. Stopping all main functionalities...")
                stopAppServices()
            } else {
                Log.d(LOG_TAG, "Clicked. Starting all main functionalities...")
                startAppServices()
            }
            toggleButtonBackground(mainButton)
        }

        checkAndRequestPermissions()
    }

    private fun toggleButtonBackground(button: Button) {
        if (appIsRecording)
            button.background = ContextCompat.getDrawable(this, R.drawable.power_off)
        else
            button.background = ContextCompat.getDrawable(this, R.drawable.power_on)
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
        svmClassifier = SVMClassifier(this)
    }

    override fun onResume() {
        super.onResume()
        // Registering accelerometer receiver
        registerReceiver(accelerometerReceiver, IntentFilter("AccelerometerData"), RECEIVER_EXPORTED)
        Log.d(LOG_TAG, "An accelerometer receiver has been registered.")

        // Registering SVM BroadcastReceiver
        svmClassifier.registerReceiver()
    }

    private fun startAppServices() {
        // enable accelerometer data gathering
        appIsRecording = true
        Toast.makeText(this, "Started data gathering...", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        startService(accelRecordingIntent)
        Log.d(LOG_TAG, "Started accelerometer data gathering")
    }

    private fun stopAppServices() {
        // stop accelerometer data gathering
        appIsRecording = false
        Toast.makeText(this, "Stopped data gathering.", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        stopService(accelRecordingIntent)
        Log.d(LOG_TAG, "Stopped accelerometer data gathering")

        // stop classifier
        svmClassifier.unregisterReceiver()
    }

    private fun onPermissionsDenied() {
        /*
        TODO: make msg appear saying what are the permissions still needed
            for the app to work
            Show button to request those missing permissions
        */
    }
}