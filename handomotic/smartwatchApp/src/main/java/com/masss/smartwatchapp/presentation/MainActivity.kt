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
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.masss.handomotic.BTBeaconManager
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.viewmodels.ConfigurationViewModel
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerManager
import com.masss.smartwatchapp.presentation.btsocket.ServerSocket
import com.masss.smartwatchapp.presentation.classifier.SVMClassifierService
import com.masss.smartwatchapp.presentation.utilities.PermissionHandler
import com.masss.smartwatchapp.presentation.utilities.UIManager
import kotlinx.coroutines.*
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()

    private val LOG_TAG: String = "HanDomotic"

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

    // ACCELEROMETER MANAGER
    private lateinit var accelerometerManager: AccelerometerManager

    // BT MANAGEMENT AND SCANNING
    private lateinit var btBeaconManager: BTBeaconManager
    private lateinit var knownBeacons: List<Beacon>
    private var serverSocketUUID: UUID = UUID.fromString("bffdf9d2-048d-45cb-b621-3025760dc306")
    private lateinit var beaconsUpdateThread: ServerSocket
    private var isBeaconsUpdateReceiverRegistered: Boolean = false

    // TEST
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            Log.d("MAIN_ACTIVITY", "Main activity is running")
            handler.postDelayed(this, 3000)
        }
    }


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

        // initializing the server socket for beacon updates
        beaconsUpdateThread = ServerSocket(this, serverSocketUUID, configurationViewModel)

        handler.post(runnable)

        // permissions are requested in onResume() at first app launch
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {      // gets called after onCreate() and every time the app is back to foreground
        super.onResume()

        Log.i("MAIN_ACTIVITY", "onResume() was called. First app launch? $firstAppLaunch")

        if (firstAppLaunch) {     // first app launch, just request permissions and call the appropriate functions
            firstAppLaunch = false
            permissionHandler = PermissionHandler(this)     // requesting permissions
            if (!permissionHandler.requestPermissionsAndCheck())
                onPermissionsDenied()
            else
                onAllPermissionsGranted()
        } else {
            if (permissionHandler.arePermissionsGranted()) {
                uiManager.toggleSettingsNavigationUI(visible = false)         // invisible if all permissions are granted
                uiManager.setupWhereAmIButton(missingRequiredPermissionsView = false, btBeaconManager, knownBeacons)

                if (appIsRecording) {
                    uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = true)
                    uiManager.setupMainButtonOnClickListener(appIsRecording = true, missingRequiredPermissionsView = false)
                } else {
                    uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = false)
                    uiManager.setupMainButtonOnClickListener(appIsRecording = false, missingRequiredPermissionsView = false)
                }
            } else
                Toast.makeText(this, "Some needed permissions still have to be granted", Toast.LENGTH_SHORT).show()
        }

//        if (permissionHandler.arePermissionsGranted()) {
//            // making text and button to go to settings invisible if when coming back all the permissions have been granted
//            uiManager.toggleSettingsNavigationUI(false)
//
//            // setup whereAmIButton for sure since all permissions are granted if here
//            uiManager.setupWhereAmIButton(false, btBeaconManager, knownBeacons)
//
//            // restoring main button's style and behavior if it was disabled because of some missing permissions
//            if (!appIsRecording) {
//                uiManager.setupMainButton(false)
//                uiManager.setupMainButtonOnClickListener(appIsRecording, false)
//            }
//
//            // Registering accelerometer receiver
//            registerReceiver(accelerometerManager.accelerometerReceiver, IntentFilter("AccelerometerData"), RECEIVER_NOT_EXPORTED)
//
//            // Registering SVM BroadcastReceiver
//            svmClassifier.registerReceiver()
//
//            //Registering known gesture receiver
////            registerReceiver(knownGestureReceiver, IntentFilter("SVMClassifierService_RecognizedGesture"), RECEIVER_NOT_EXPORTED)
//            val gestureReceiverFilter = IntentFilter("SVMClassifierService_RecognizedGesture")
//            LocalBroadcastManager.getInstance(this).registerReceiver(knownGestureReceiver, gestureReceiverFilter)
//        } else
//            Toast.makeText(this, "Some needed permissions still have to be granted", Toast.LENGTH_SHORT).show()
    }

    private fun registerReceivers() {
        Log.i("MAIN_ACTIVITY", "registerReceivers() was called. Registering receivers...")
        if (::accelerometerManager.isInitialized)
            registerReceiver(
                accelerometerManager.accelerometerReceiver,
                IntentFilter("AccelerometerData"),
                RECEIVER_NOT_EXPORTED
            )

        if (::svmClassifier.isInitialized)
            svmClassifier.registerReceiver()

        if (!isGestureReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).registerReceiver(
                knownGestureReceiver,
                IntentFilter("SVMClassifierService_RecognizedGesture")
            )
            isGestureReceiverRegistered = true
        }

        if (!isBeaconsUpdateReceiverRegistered) {
            val beaconUpdatesFilter = IntentFilter("com.masss.smartwatchapp.BEACON_UPDATE")
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
            onAllPermissionsGranted()
        else
            onPermissionsDenied()
    }

    private fun onAllPermissionsGranted() {
        // starting BT beacon scanning for nearby beacons
        btBeaconManager.startScanning()

        Log.i("MAIN_ACTIVITY", "All permissions have been granted")

        // initializing the list of known beacons from file on device persistent memory
        configurationViewModel.initialize(this)
        knownBeacons = configurationViewModel.getBeacons()
        Log.i("MAIN_ACTIVITY", "Found ${knownBeacons.size} known beacons")

        // setting up onclick listeners on activity creation for main and whereAmI buttons for the first time
        uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = false)
        uiManager.setupMainButtonOnClickListener(appIsRecording = false, missingRequiredPermissionsView = false)
        uiManager.setupWhereAmIButton(missingRequiredPermissionsView = false, btBeaconManager, knownBeacons)

        // Start listening for config updates from companion app
        beaconsUpdateThread.start()
    }

    private val knownGestureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                temporarilyStopKnownGestureReceiver()           // avoid receiving the following gestures for x seconds defined in 'delayGestureBroadcast'

                val recognizedGesture = it.getStringExtra("ClassificationResult") ?: "No prediction"
                Log.i(LOG_TAG, "Received gesture from SVMClassifierService: $recognizedGesture")

                if (AppLifecycleManager.isAppInForeground()) {
                    Log.i("MAIN_ACTIVITY", "Recognized gesture: $recognizedGesture in foreground")
                    uiManager.showGestureRecognizedScreen(recognizedGesture, btBeaconManager, knownBeacons, 500)
                } else {
                    Log.i("MAIN_ACTIVITY", "Recognized gesture: $recognizedGesture in background")
                    uiManager.notifyGestureRecognizedOnAppBackground(1000)
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
        Log.i("MAIN_ACTIVITY", "Starting app services, app is recording")

        Toast.makeText(this, "Gesture recognition is active", Toast.LENGTH_SHORT).show()
//        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
//        startService(accelRecordingIntent)

        registerReceivers()
        startForegroundServices()

        // Registering SVM BroadcastReceiver
//        svmClassifier.registerReceiver()

        // Registering beacon updates receiver
//        val beaconUpdatesFilter = IntentFilter("com.masss.smartwatchapp.BEACON_UPDATE")
//        registerReceiver(beaconsUpdateReceiver, beaconUpdatesFilter, RECEIVER_NOT_EXPORTED)

        // Registering the gesture recognition broadcast receiver
//        val gestureReceiverFilter = IntentFilter("SVMClassifierService_RecognizedGesture")
//        registerReceiver(knownGestureReceiver, gestureReceiverFilter, RECEIVER_NOT_EXPORTED)

        uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = false)
        uiManager.setupMainButtonOnClickListener(appIsRecording = true, missingRequiredPermissionsView = false)

//        val accelerometerServiceIntent = Intent(this, AccelerometerManager::class.java)
//        ContextCompat.startForegroundService(this, accelerometerServiceIntent)

//        val classifierIntent = Intent(this, SVMClassifierService::class.java)
//        ContextCompat.startForegroundService(this, classifierIntent)
    }

    private fun startForegroundServices() {         // starting the services that need to keep on running also when the app is in the background
        Log.i("MAIN_ACTIVITY", "startForegroundServices() was called. Starting foreground services...")

        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        startService(accelRecordingIntent)

        val accelerometerServiceIntent = Intent(this, AccelerometerManager::class.java)
        ContextCompat.startForegroundService(this, accelerometerServiceIntent)

        val classifierIntent = Intent(this, SVMClassifierService::class.java)
        ContextCompat.startForegroundService(this, classifierIntent)
    }

    private fun stopForegroundServices() {
        Log.i("MAIN_ACTIVITY", "stopForegroundServices() was called. Stopping foreground services...")
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        stopService(accelRecordingIntent)

        val accelerometerServiceIntent = Intent(this, AccelerometerManager::class.java)
        stopService(accelerometerServiceIntent)

        val classifierIntent = Intent(this, SVMClassifierService::class.java)
        stopService(classifierIntent)
    }

    private fun unregisterReceivers() {
        Log.i("MAIN_ACTIVITY", "unRegisterReceivers() was called. Unregistering receivers...")

        if (::accelerometerManager.isInitialized)
            unregisterReceiver(accelerometerManager.accelerometerReceiver)
        if (::svmClassifier.isInitialized)
            svmClassifier.unregisterReceiver()
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
        Log.i(LOG_TAG, "Stopping app services, setting appIsRecording to false")

        Toast.makeText(this, "Gesture recognition is off", Toast.LENGTH_SHORT).show()

        unregisterReceivers()
        stopForegroundServices()

        uiManager.setupMainButton(missingRequiredPermissionsView = false, appWasRunningWhenResumed = false)
        uiManager.setupMainButtonOnClickListener(appIsRecording = false, missingRequiredPermissionsView = false)
    }

    private fun onPermissionsDenied() {
        Log.i("MAIN_ACTIVITY", "onPermissionsDenied() was called")

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