package com.masss.smartwatchapp.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.masss.smartwatchapp.R
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerRecordingService
import com.masss.smartwatchapp.presentation.classifier.SVMClassifier
import java.util.LinkedList
import android.widget.TextView


class MainActivity : AppCompatActivity() {

    /*
        TODO:
        put all accelerometer logic in a separate class
        put all button logic in a separate class ?

        integrate class for watch-mobile interaction
    */

    private val LOG_TAG: String = "HanDomotic"
    private var missingRequiredPermissionsView: Boolean = false

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

            if (xValue != null && yValue != null && zValue != null && timestamp != null)
                addSample(xValue, yValue, zValue)       // Makes the window slide: writes in xWindow, yWindow, zWindow

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

        checkAndRequestPermissions()
    }

    private fun toggleButtonBackground(button: Button) {
        if (appIsRecording)
            button.background = ContextCompat.getDrawable(this, R.drawable.power_off)
        else
            button.background = ContextCompat.getDrawable(this, R.drawable.power_on)
    }

    private fun checkAndRequestPermissions() {
        Log.d(LOG_TAG, "checkAndRequestPermissions(): has been called")
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
        Log.d(LOG_TAG, "requestPermissionsLauncher(): has been called")
        val deniedPermissions = permissions.filter { !it.value }.keys
        if (deniedPermissions.isEmpty())
            onAllPermissionsGranted()
        else {
            onPermissionsDenied(deniedPermissions)
        }
    }

    private fun toggleSettingsNavigationUI(visible: Boolean) {
        Log.d(LOG_TAG, "toggleSettingsNavigationUI(): updating UI...")

        val deniedPermissionAcceptText = findViewById<TextView>(R.id.permissionsText)
        val permissionsActivityButton = findViewById<Button>(R.id.grantMissingPermissionsButton)
        val buttonSpacer = findViewById<View>(R.id.permissionsButtonSpacer)

        if (visible) {
            deniedPermissionAcceptText.visibility = View.VISIBLE
            permissionsActivityButton.visibility = View.VISIBLE
            buttonSpacer.visibility = View.VISIBLE
        } else {
            deniedPermissionAcceptText.visibility = View.GONE
            permissionsActivityButton.visibility = View.GONE
            buttonSpacer.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            missingRequiredPermissionsView = false

            // making text and button to go to settings invisible
            toggleSettingsNavigationUI(false)

            // restoring main button's style and behavior if it was disabled because of some missing permissions
            setupMainButton()

            // Registering accelerometer receiver
            registerReceiver(
                accelerometerReceiver,
                IntentFilter("AccelerometerData"),
                RECEIVER_EXPORTED
            )
            Log.d(LOG_TAG, "onResume(): an accelerometer receiver has been registered.")

            // instantiating the SVM classifier if it hasn't been yet
            if (!::svmClassifier.isInitialized)
                svmClassifier = SVMClassifier(this)


            // Registering SVM BroadcastReceiver
            svmClassifier.registerReceiver()
        } else {
            Toast.makeText(this, "Some needed permissions still have to be granted", Toast.LENGTH_LONG).show()
            Log.d(LOG_TAG, "onResume(): permissions from settings are still not granted")
        }
    }

    private fun allPermissionsGranted(): Boolean {
        Log.d(LOG_TAG, "allPermissionsGranted(): checking if all the necessary permissions have been granted...")

        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun setupMainButton() {
        Log.d(LOG_TAG, "setupMainButton(): setting up the main app's button...")

        val mainButton: Button = findViewById(R.id.mainButton)

        if (missingRequiredPermissionsView)            // graying out the button to make it look disabled
            mainButton.background = ContextCompat.getDrawable(this, R.drawable.power_disabled)
        else           // restoring the button to its main style (power on background)
            mainButton.background = ContextCompat.getDrawable(this, R.drawable.power_on)

        mainButton.setOnClickListener {
            if (missingRequiredPermissionsView)
                Toast.makeText(this, "Some needed permissions are still required", Toast.LENGTH_LONG).show()
            else {
                if (appIsRecording) {
                    Log.d(LOG_TAG, "Stopping all main functionalities...")
                    stopAppServices()
                } else {
                    Log.d(LOG_TAG, "Starting all main functionalities...")
                    startAppServices()
                }
                toggleButtonBackground(mainButton)
            }
        }
    }

    private fun onAllPermissionsGranted() {
        Log.d(LOG_TAG, "onAllPermissionsGranted(): has been called")

        missingRequiredPermissionsView = false

        // initializing the classifier
        svmClassifier = SVMClassifier(this)

        // setting up onclick listeners on activity creation
        setupMainButton()
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

    private fun onPermissionsDenied(deniedPermissions: Set<String>) {
        missingRequiredPermissionsView = true

        Log.d(LOG_TAG, "onPermissionsDenied(): has been called")
        Toast.makeText(this, "App functionalities may be limited.", Toast.LENGTH_SHORT).show()

        val deniedPermissionsString = deniedPermissions.joinToString(", ")
        Log.d(LOG_TAG, "onPermissionsDenied(): denied permissions: $deniedPermissionsString")

        // changing main button's style and behavior
        setupMainButton()

        // making text and button to go to settings visible
        toggleSettingsNavigationUI(true)

        val grantMissingPermissionsButton: Button = findViewById(R.id.grantMissingPermissionsButton)
        grantMissingPermissionsButton.setOnClickListener {
            openAppSettings()
        }
    }

    private fun openAppSettings() {
        Log.d(LOG_TAG, "openAppSettings(): opening app settings...")

        val intent = Intent()
        intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri

        startActivity(intent)
    }
}