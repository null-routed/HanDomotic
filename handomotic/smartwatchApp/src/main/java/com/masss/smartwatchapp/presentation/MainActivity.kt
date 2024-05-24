package com.masss.smartwatchapp.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.masss.smartwatchapp.R
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerRecordingService
import com.masss.smartwatchapp.presentation.classifier.SVMClassifier
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import com.masss.handomotic.BTBeaconManager
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.viewmodels.ConfigurationViewModel
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerManager
import com.masss.smartwatchapp.presentation.btsocket.ServerSocket
import com.masss.smartwatchapp.presentation.utilities.PermissionHandler
import com.masss.smartwatchapp.presentation.utilities.UIManager
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()

    private val LOG_TAG: String = "HanDomotic"

    // Tracker for the app state
    private var appIsRecording: Boolean = false
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var uiManager: UIManager

    // GESTURE RECEIVER MANAGEMENT
    private val gestureReceiverHandler = Handler(Looper.getMainLooper())
    private val delayGestureBroadcast = 5000L       // seconds delay between two consecutive gesture recognitions

    // CLASSIFIER
    private lateinit var svmClassifier: SVMClassifier

    // ACCELEROMETER MANAGER
    private lateinit var accelerometerManager: AccelerometerManager

    // BT MANAGEMENT AND SCANNING
    private lateinit var btBeaconManager: BTBeaconManager
    private lateinit var knownBeacons: List<Beacon>
    private var serverSocketUUID: UUID = UUID.fromString("bffdf9d2-048d-45cb-b621-3025760dc306")
    private lateinit var beaconsUpdateThread: ServerSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // initializing the classifier
        svmClassifier = SVMClassifier(this)

        // initializing the BT manager
        btBeaconManager = BTBeaconManager(this, configurationViewModel.getBeacons())

        // initializing the UI manager to handle UI changes
        uiManager = UIManager(this, { startAppServices() }, { stopAppServices() })

        // initializing the accelerometer manager
        accelerometerManager = AccelerometerManager(this)

        // initializing the server socket for beacon updates
        beaconsUpdateThread = ServerSocket(this, serverSocketUUID, configurationViewModel)



        // requesting permissions
        permissionHandler = PermissionHandler(this)
        if (!permissionHandler.requestPermissionsAndCheck())
            onPermissionsDenied()
        else
            onAllPermissionsGranted()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        if (permissionHandler.arePermissionsGranted()) {
            // making text and button to go to settings invisible if when coming back all the permissions have been granted
            uiManager.toggleSettingsNavigationUI(false)

            // setup whereAmIButton for sure since all permissions are granted if here
            uiManager.setupWhereAmIButton(false, btBeaconManager, knownBeacons)

            // restoring main button's style and behavior if it was disabled because of some missing permissions
            if (!appIsRecording) {
                uiManager.setupMainButton(false)
                uiManager.setupMainButtonOnClickListener(appIsRecording, false)
            }

            // Registering accelerometer receiver
            registerReceiver(accelerometerManager.accelerometerReceiver, IntentFilter("AccelerometerData"), RECEIVER_NOT_EXPORTED)

            // Registering SVM BroadcastReceiver
            svmClassifier.registerReceiver()
        } else
            Toast.makeText(this, "Some needed permissions still have to be granted", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()

        // resources cleanup
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        stopService(accelRecordingIntent)
        svmClassifier.unregisterReceiver()
        btBeaconManager.stopScanning()
        unregisterReceiver(knownGestureReceiver)
        unregisterReceiver(beaconsUpdateReceiver)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults))
            onAllPermissionsGranted()
        else
            onPermissionsDenied()
    }

    private fun onAllPermissionsGranted() {
        // starting BT beacon scanning for nearby beacons
        btBeaconManager.startScanning()

        // initializing the list of known beacons from file on device persistent memory
        configurationViewModel.initialize(this)
        knownBeacons = configurationViewModel.getBeacons()
        Log.i(LOG_TAG, "Found ${knownBeacons.size} known beacons")

        // setting up onclick listeners on activity creation for main and whereAmI buttons
        uiManager.setupMainButton(false)
        uiManager.setupMainButtonOnClickListener(appIsRecording, false)
        uiManager.setupWhereAmIButton(false, btBeaconManager, knownBeacons)

        // Start listening for config updates from companion app
        beaconsUpdateThread.start()
    }

    private val knownGestureReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                temporarilyStopKnownGestureReceiver()           // avoid receiving the following gestures for x seconds defined in 'delayGestureBroadcast'

                val recognizedGesture = it.getStringExtra("prediction") ?: "No prediction"

                uiManager.showGestureRecognizedScreen(recognizedGesture, btBeaconManager, knownBeacons)
            }
        }
    }

    // Needed to prevent mainActivity to be flooded with too many messages just for a single recognized gesture
    private fun temporarilyStopKnownGestureReceiver() {
        unregisterReceiver(knownGestureReceiver)

        gestureReceiverHandler.postDelayed({
            registerReceiver(
                knownGestureReceiver,
                IntentFilter("com.masss.smartwatchapp.GESTURE_RECOGNIZED"), RECEIVER_NOT_EXPORTED
            )
        }, delayGestureBroadcast)
    }

    // whenever a beacon update is received from the companion app, update the known beacons list, something might have changed
    private val beaconsUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            knownBeacons = configurationViewModel.getBeacons() // simply reading again the file
           // knownBeacons = btBeaconManager.getKnownBeacons()
        }
    }

    private fun startAppServices() {
        // enable accelerometer data gathering
        appIsRecording = true
        Log.i(LOG_TAG, "Starting app services, setting appIsRecording to true")

        Toast.makeText(this, "Gesture recognition is active", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        startService(accelRecordingIntent)

        // Registering SVM BroadcastReceiver
        svmClassifier.registerReceiver()

        // Registering beacon updates receiver
        val beaconUpdatesFilter = IntentFilter("com.masss.smartwatchapp.BEACON_UPDATE")
        registerReceiver(beaconsUpdateReceiver, beaconUpdatesFilter, RECEIVER_NOT_EXPORTED)

        // Registering the gesture recognition broadcast receiver
        val gestureReceiverFilter = IntentFilter("com.masss.smartwatchapp.GESTURE_RECOGNIZED")
        registerReceiver(knownGestureReceiver, gestureReceiverFilter, RECEIVER_NOT_EXPORTED)

        uiManager.setupMainButton(true)
        uiManager.setupMainButtonOnClickListener(appIsRecording, false)
    }

    private fun stopAppServices() {
        // stop accelerometer data gathering
        appIsRecording = false
        Log.i(LOG_TAG, "Stopping app services, setting appIsRecording to false")

        Toast.makeText(this, "Gesture recognition is off", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        stopService(accelRecordingIntent)

        // stop classifier
        svmClassifier.unregisterReceiver()

        // stop BT beacon scanning
        btBeaconManager.stopScanning()

        // Unregistering the gesture recognition receiver
        unregisterReceiver(knownGestureReceiver)

        // Unregistering beacon updates receiver
        unregisterReceiver(beaconsUpdateReceiver)

        uiManager.setupMainButton(false)
        uiManager.setupMainButtonOnClickListener(appIsRecording, false)
    }

    private fun onPermissionsDenied() {
        Log.i(LOG_TAG, "onPermissionsDenied")

        knownBeacons = emptyList()
        // setting up buttons' styles and behavior
        uiManager.setupMainButton(true)
        uiManager.setupMainButtonOnClickListener(appIsRecording, true)
        uiManager.setupWhereAmIButton(true, btBeaconManager, knownBeacons)

        // making text and button to go to settings visible
        uiManager.toggleSettingsNavigationUI(true)

        val grantMissingPermissionsButton: Button = findViewById(R.id.grantMissingPermissionsButton)
        grantMissingPermissionsButton.setOnClickListener {
            permissionHandler.openAppSettings()
        }
    }
}