package com.masss.smartwatchapp.presentation

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
import android.widget.TextView
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerManager
import com.masss.smartwatchapp.presentation.btbeaconmanager.BTBeaconManager


class MainActivity : AppCompatActivity() {

    /*
        TODO:
        put all button logic in a separate class ?

        integrate class for watch-mobile interaction

        add methods to scan nearby ever x ms and update closeBTBeacons ?
    */

    private val LOG_TAG: String = "HanDomotic"
    private var missingRequiredPermissionsView: Boolean = false

    // test
    private val knownBeacons = mapOf(
        "C9:8E:A0:EF:D5:77" to "Bedroom",
        "EE:89:D2:1D:90:51" to "Living Room"
    )

    // BT MANAGEMENT AND SCANNING
    private lateinit var btBeaconManager: BTBeaconManager

    // Tracker for the app state
    private var appIsRecording: Boolean = false

    private val requiredPermissions = arrayOf(
        android.Manifest.permission.BODY_SENSORS,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    /* Classifier*/
    private lateinit var svmClassifier: SVMClassifier

    // ACCELEROMETER MANAGER
    private lateinit var accelerometerManager: AccelerometerManager

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

            setupWhereAmIButton()

            // Registering accelerometer receiver
            registerReceiver(accelerometerManager.accelerometerReceiver, IntentFilter("AccelerometerData"), RECEIVER_EXPORTED)
            Log.d(LOG_TAG, "onResume(): an accelerometer receiver has been registered.")

            // instantiating the SVM classifier if it hasn't been yet and same goes for the BT manager
            if (!::svmClassifier.isInitialized)
                svmClassifier = SVMClassifier(this)
            if (!::btBeaconManager.isInitialized)
                btBeaconManager = BTBeaconManager(this)


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

        // initializing the BT manager
        btBeaconManager = BTBeaconManager(this)
        btBeaconManager.startScanning()
        Log.d(LOG_TAG, "onAllPermissionsGranted(): started BT beacons scanning...")

        // setting up onclick listeners on activity creation
        setupMainButton()

        // setting up where am i button
        setupWhereAmIButton()

        // initializing the accelerometer manager
        accelerometerManager = AccelerometerManager(this)
    }

    private fun setupWhereAmIButton() {
        Log.d(LOG_TAG, "setupWhereAmIButton(): setting up Where Am I Button...")

        val whereAmIButton: Button = findViewById(R.id.whereAmIButton)

        whereAmIButton.setOnClickListener {
            val closeBTBeacons = btBeaconManager.getBeacons()

            if (closeBTBeacons.isNotEmpty()) {

                var closestBeaconLocation: String? = null

                for (beacon in closeBTBeacons) {
                    if (knownBeacons.containsKey(beacon.address)) {
                        closestBeaconLocation = knownBeacons[beacon.address]
                        break
                    }
                }

                if (closestBeaconLocation != null)
                    Toast.makeText(
                        this,
                        "You are here: ${closestBeaconLocation.uppercase()}", Toast.LENGTH_SHORT).show()
                else
                    Toast.makeText(this, "No close known beacons found", Toast.LENGTH_SHORT).show()
            } else
                Toast.makeText(this, "No close known beacons found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startAppServices() {
        // enable accelerometer data gathering
        appIsRecording = true
        Toast.makeText(this, "Gesture recognition is active", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        startService(accelRecordingIntent)
        Log.d(LOG_TAG, "startAppServices(): started accelerometer data gathering")
        // Registering SVM BroadcastReceiver
        svmClassifier.registerReceiver()
    }

    private fun stopAppServices() {
        // stop accelerometer data gathering
        appIsRecording = false
        Toast.makeText(this, "Gesture recognition is off", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        stopService(accelRecordingIntent)
        Log.d(LOG_TAG, "Stopped accelerometer data gathering")

        // stop classifier
        svmClassifier.unregisterReceiver()

        // stop BT beacon scanning
        btBeaconManager.stopScanning()
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