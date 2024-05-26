package com.masss.smartwatchapp.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
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
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.masss.handomotic.BTBeaconManager
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.viewmodels.ConfigurationViewModel
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerManager
import com.masss.smartwatchapp.presentation.btmanagement.BTBeaconManagerService
import com.masss.smartwatchapp.presentation.btsocket.ServerSocket
import com.masss.smartwatchapp.presentation.classifier.SVMClassifierService
import com.masss.smartwatchapp.presentation.utilities.PermissionHandler
import com.masss.smartwatchapp.presentation.utilities.UIManager
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()

    private val TAG = "MAIN_ACTIVITY"

    // Tracker for the app state
    private var appIsRecording: Boolean = false
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var uiManager: UIManager
    private var firstAppLaunch: Boolean = true

    object AppLifecycleManager {
        private var foregroundActivityCount = 0

        fun onActivityStarted() {
            foregroundActivityCount++
        }

        fun onActivityStopped() {
            foregroundActivityCount--
        }

        fun isAppInForeground(): Boolean {
            return foregroundActivityCount > 0
        }
    }

    // GESTURE RECEIVER MANAGEMENT
    private val gestureReceiverHandler = Handler(Looper.getMainLooper())
    private val delayGestureBroadcast = 5000L       // seconds delay between two consecutive gesture recognitions
    private var isGestureReceiverRegistered: Boolean = false

    // CLASSIFIER
    private lateinit var svmClassifier: SVMClassifier
    private var isSVMClassifierRegistered: Boolean = false

    // ACCELEROMETER MANAGER
    private lateinit var accelerometerManager: AccelerometerManager
    private var isAccelerometerManagerRegistered: Boolean = false

    // BT MANAGEMENT AND SCANNING
    private lateinit var btBeaconManager: BTBeaconManager
    private lateinit var knownBeacons: List<Beacon>
    private var serverSocketUUID: UUID = UUID.fromString("bffdf9d2-048d-45cb-b621-3025760dc306")
    private lateinit var beaconsUpdateThread: ServerSocket
    private var isBeaconsUpdateReceiverRegistered: Boolean = false
    private var isBeaconThreadRunning: Boolean = false

    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.Q)
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

        Log.i(TAG, "onCreate() was called. First app launch? $firstAppLaunch, appIsRecording = $appIsRecording")

        permissionHandler = PermissionHandler(this)

        sharedPreferences = getSharedPreferences("com.masss.smartwatchapp", Context.MODE_PRIVATE)

        // initializing the server socket for beacon updates
        beaconsUpdateThread = ServerSocket(this, serverSocketUUID, configurationViewModel)

        // permissions are requested in onResume() at first app launch
    }

    private fun updateSharedPreferences(appIsRecordingValue: Boolean, firstAppLaunchValue: Boolean) {
        with(sharedPreferences.edit()) {
            putBoolean("appIsRecording", appIsRecordingValue)
            putBoolean("firstAppLaunch", firstAppLaunchValue)
            apply()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.i(TAG, "onSaveInstanceState() was called: appIsRecording = $appIsRecording, firstAppLaunch = $firstAppLaunch")
        updateSharedPreferences(appIsRecording, firstAppLaunch)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        super.onDestroy()
        updateSharedPreferences(false, true)
        unregisterReceivers()
        stopForegroundServices()
        Log.i(TAG, "onDestroy() was called")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()

        appIsRecording = sharedPreferences.getBoolean("appIsRecording", false)
        firstAppLaunch = sharedPreferences.getBoolean("firstAppLaunch", true)

        Log.d(TAG, "onResume() was called: firstAppLaunch = $firstAppLaunch, appIsRecording = $appIsRecording")

        if (firstAppLaunch)
            handleFirstAppLaunch()      // First app launch ever or launch after destruction
        else
            handleSubsequentLaunches()      // Subsequent launches from background to foreground without destruction
    }

    private fun handleFirstAppLaunch() {
        Log.i(TAG, "First app launch detected.")
        firstAppLaunch = false
        if (!permissionHandler.requestPermissionsAndCheck()) {
            onPermissionsDenied()
        } else {
            onAllPermissionsGranted(true)
        }
    }

    private fun handleSubsequentLaunches() {
        Log.i(TAG, "Subsequent app launch detected.")
        if (permissionHandler.arePermissionsGranted()) {
            onAllPermissionsGranted(false)

            uiManager.toggleSettingsNavigationUI(visible = false)
            uiManager.setupWhereAmIButton(missingRequiredPermissionsView = false, btBeaconManager, knownBeacons)
            if (appIsRecording) {
                uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = true)
                uiManager.setupMainButtonOnClickListener(appIsRecording = true, missingRequiredPermissionsView = false)
            } else {
                uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = false)
                uiManager.setupMainButtonOnClickListener(appIsRecording = false, missingRequiredPermissionsView = false)
            }
        } else {
            Toast.makeText(this, "Some needed permissions still have to be granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerReceivers() {
        Log.i(TAG, "registerReceivers() was called. Registering receivers...")
        if (::accelerometerManager.isInitialized && !isAccelerometerManagerRegistered) {
            registerReceiver(
                accelerometerManager.accelerometerReceiver,
                IntentFilter("AccelerometerData"),
                RECEIVER_NOT_EXPORTED
            )
            isAccelerometerManagerRegistered = true
        }

        if (::svmClassifier.isInitialized && !isSVMClassifierRegistered) {
            svmClassifier.registerReceiver()
            isSVMClassifierRegistered = true
        }

        if (!isGestureReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                knownGestureReceiver,
                IntentFilter("SVMClassifierService_RecognizedGesture")
            )
            isGestureReceiverRegistered = true
        }

        if (!isBeaconsUpdateReceiverRegistered) {
            val beaconUpdatesFilter = IntentFilter("CompanionApp_ReceivedBeaconUpdate")
            registerReceiver(beaconsUpdateReceiver, beaconUpdatesFilter, RECEIVER_NOT_EXPORTED)
            isBeaconsUpdateReceiverRegistered = true
        }
    }

    override fun onStart() {
        super.onStart()
        AppLifecycleManager.onActivityStarted()
    }

    override fun onStop() {
        super.onStop()
        AppLifecycleManager.onActivityStopped()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionHandler.onRequestPermissionsResult(requestCode, permissions, grantResults))
            onAllPermissionsGranted(true)
        else
            onPermissionsDenied()
    }

    override fun onPause() {
        super.onPause()

        if (beaconsUpdateThread.isAlive && isBeaconThreadRunning)
            beaconsUpdateThread.stopServer()
    }

    private fun onAllPermissionsGranted(firstLaunch: Boolean) {
        // starting BT beacon scanning for nearby beacons
        btBeaconManager.startScanning()

        Log.i(TAG, "All permissions have been granted")

        // initializing the list of known beacons from file on device persistent memory
        configurationViewModel.initialize(this)
        knownBeacons = configurationViewModel.getBeacons()
        Log.i(TAG, "Found ${knownBeacons.size} known beacons")

        // setting up onclick listeners on activity creation for main and whereAmI buttons for the first time
        if (firstLaunch){
            uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = false)
            uiManager.setupMainButtonOnClickListener(appIsRecording = false, missingRequiredPermissionsView = false)
            uiManager.setupWhereAmIButton(missingRequiredPermissionsView = false, btBeaconManager, knownBeacons)
        }

        // Start listening for config updates from companion app
        if (!isBeaconsUpdateReceiverRegistered) {
            val beaconUpdatesFilter = IntentFilter("BeaconUpdatesFromCompanionApp")
            registerReceiver(beaconsUpdateReceiver, beaconUpdatesFilter, RECEIVER_NOT_EXPORTED)
            isBeaconsUpdateReceiverRegistered = true
        }

        if (!isBeaconThreadRunning) {
            beaconsUpdateThread.start()
            isBeaconThreadRunning = true
        }
    }

    private val knownGestureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                temporarilyStopKnownGestureReceiver()           // avoid receiving the following gestures for x seconds defined in 'delayGestureBroadcast'

                val recognizedGesture = it.getStringExtra("ClassificationResult") ?: "No prediction"
                Log.i(TAG, "Received gesture from SVMClassifierService: $recognizedGesture")

                if (AppLifecycleManager.isAppInForeground()) {
                    Log.i(TAG, "Recognized gesture: $recognizedGesture in foreground")
                    uiManager.showGestureRecognizedScreen(recognizedGesture, btBeaconManager, knownBeacons, 500)
                } else {
                    Log.i(TAG, "Recognized gesture: $recognizedGesture in background")
                    uiManager.notifyGestureRecognizedOnAppBackground(btBeaconManager, knownBeacons, 1000)
                }

            }
        }
    }

    // Needed to prevent mainActivity to be flooded with too many messages just for a single recognized gesture
    private fun temporarilyStopKnownGestureReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(knownGestureReceiver)

        gestureReceiverHandler.postDelayed({
            LocalBroadcastManager.getInstance(this).registerReceiver(
                knownGestureReceiver,
                IntentFilter("SVMClassifierService_RecognizedGesture")
            )
        }, delayGestureBroadcast)
    }

    // whenever a beacon update is received from the companion app, update the known beacons list, something might have changed
    private val beaconsUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            knownBeacons = configurationViewModel.getBeacons() // simply reading again the file
        }
    }

    private fun startAppServices() {
        // enable accelerometer data gathering
        appIsRecording = true
        Log.i(TAG, "Starting app services, app is recording")

        Toast.makeText(this, "Gesture recognition is active", Toast.LENGTH_SHORT).show()

        registerReceivers()
        startForegroundServices()

        uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = false)
        uiManager.setupMainButtonOnClickListener(appIsRecording = true, missingRequiredPermissionsView = false)
    }

    private fun startForegroundServices() {         // starting the services that need to keep on running also when the app is in the background
        Log.i(TAG, "startForegroundServices() was called. Starting foreground services...")

        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        startService(accelRecordingIntent)

        val accelerometerServiceIntent = Intent(this, AccelerometerManager::class.java)
        ContextCompat.startForegroundService(this, accelerometerServiceIntent)

        val classifierIntent = Intent(this, SVMClassifierService::class.java)
        ContextCompat.startForegroundService(this, classifierIntent)

        val btBeaconManagerIntent = Intent(this, BTBeaconManagerService::class.java)
        ContextCompat.startForegroundService(this, btBeaconManagerIntent)
    }

    private fun stopForegroundServices() {
        Log.i(TAG, "stopForegroundServices() was called. Stopping foreground services...")
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        stopService(accelRecordingIntent)

        val accelerometerServiceIntent = Intent(this, AccelerometerManager::class.java)
        stopService(accelerometerServiceIntent)

        val classifierIntent = Intent(this, SVMClassifierService::class.java)
        stopService(classifierIntent)

        val btBeaconManagerIntent = Intent(this, BTBeaconManagerService::class.java)
        stopService(btBeaconManagerIntent)
    }

    private fun unregisterReceivers() {
        Log.i(TAG, "unRegisterReceivers() was called. Unregistering receivers...")

        if (::accelerometerManager.isInitialized && isAccelerometerManagerRegistered) {
            unregisterReceiver(accelerometerManager.accelerometerReceiver)
            isAccelerometerManagerRegistered = false
        }

        if (::svmClassifier.isInitialized && isSVMClassifierRegistered) {
            svmClassifier.unregisterReceiver()
            isSVMClassifierRegistered = false
        }

        if (isGestureReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(knownGestureReceiver)
            isGestureReceiverRegistered = false
        }

        if (isBeaconsUpdateReceiverRegistered) {
            unregisterReceiver(beaconsUpdateReceiver)
            isBeaconsUpdateReceiverRegistered = false
        }
    }

    private fun stopAppServices() {
        // stop accelerometer data gathering
        appIsRecording = false
        Log.i(TAG, "Stopping app services, app is not recording")

        Toast.makeText(this, "Gesture recognition is off", Toast.LENGTH_SHORT).show()

        unregisterReceivers()
        stopForegroundServices()

        uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = false)
        uiManager.setupMainButtonOnClickListener(appIsRecording = false, missingRequiredPermissionsView = false)
    }

    private fun onPermissionsDenied() {
        Log.i(TAG, "onPermissionsDenied() was called")

        knownBeacons = emptyList()

        // setting up buttons' styles and behavior
        uiManager.setupMainButton(missingRequiredPermissionsView = true, appWasRunningWhenResumed = false)
        uiManager.setupMainButtonOnClickListener(appIsRecording = false, missingRequiredPermissionsView = true)
        uiManager.setupWhereAmIButton(missingRequiredPermissionsView = true, btBeaconManager, knownBeacons)

        // making text and button to go to settings visible
        uiManager.toggleSettingsNavigationUI(visible = true)

        val grantMissingPermissionsButton: Button = findViewById(R.id.grantMissingPermissionsButton)
        grantMissingPermissionsButton.setOnClickListener {
            permissionHandler.openAppSettings()
        }
    }
}