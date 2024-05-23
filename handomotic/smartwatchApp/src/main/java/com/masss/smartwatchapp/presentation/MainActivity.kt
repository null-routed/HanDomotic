package com.masss.smartwatchapp.presentation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.masss.smartwatchapp.R
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerRecordingService
import com.masss.smartwatchapp.presentation.classifier.SVMClassifier
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import com.masss.handomotic.BTBeaconManager
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.viewmodels.ConfigurationViewModel
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerManager
import com.masss.smartwatchapp.presentation.btsocket.ServerSocket
import com.masss.smartwatchapp.presentation.utilities.PermissionHandler
import java.util.UUID


class MainActivity : AppCompatActivity() {

    private val configurationViewModel: ConfigurationViewModel by viewModels()

    private val LOG_TAG: String = "HanDomotic"

    // Tracker for the app state
    private var appIsRecording: Boolean = false
    private var missingRequiredPermissionsView: Boolean = false
    private lateinit var permissionHandler: PermissionHandler

    // GESTURE RECEIVER MANAGEMENT
    private val gestureReceiverHandler = Handler(Looper.getMainLooper())
    private val delayGestureBroadcast = 5000L       // seconds delay between two consecutive gesture recognitions
    private lateinit var vibrator: Vibrator

    // CLASSIFIER
    private lateinit var svmClassifier: SVMClassifier

    // ACCELEROMETER MANAGER
    private lateinit var accelerometerManager: AccelerometerManager

    // BT MANAGEMENT AND SCANNING
    private lateinit var btBeaconManager: BTBeaconManager
    private var knownBeacons: MutableMap<String, Beacon>? = null
    private var serverSocketUUID: UUID = UUID.fromString("bffdf9d2-048d-45cb-b621-3025760dc306")
    private lateinit var beaconsUpdateThread: ServerSocket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        configurationViewModel.initialize(this)

        permissionHandler = PermissionHandler(this)
        if (!permissionHandler.requestPermissionsAndCheck())
            onPermissionsDenied()
        else
            onAllPermissionsGranted()
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        Log.d(LOG_TAG, "onResume(): has been called")
        Log.i(LOG_TAG, permissionHandler.arePermissionsGranted().toString())
        if (permissionHandler.arePermissionsGranted()) {
            missingRequiredPermissionsView = false

            // making text and button to go to settings invisible
            toggleSettingsNavigationUI(false)

            // restoring main button's style and behavior if it was disabled because of some missing permissions
            if (!appIsRecording)
                setupMainButton()

            setupWhereAmIButton()

            if (!::svmClassifier.isInitialized) svmClassifier = SVMClassifier(this)
            if (!::btBeaconManager.isInitialized) btBeaconManager = BTBeaconManager(this, configurationViewModel.getBeacons())
            if (!::accelerometerManager.isInitialized) accelerometerManager = AccelerometerManager(this)

            // Registering accelerometer receiver
            registerReceiver(accelerometerManager.accelerometerReceiver, IntentFilter("AccelerometerData"), RECEIVER_NOT_EXPORTED)

            // Registering SVM BroadcastReceiver
            svmClassifier.registerReceiver()
        } else
            Toast.makeText(this, "Some needed permissions still have to be granted", Toast.LENGTH_LONG).show()
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

    private fun toggleButtonBackground(button: Button) {
        if (appIsRecording)
            button.background = ContextCompat.getDrawable(this, R.drawable.power_off)
        else
            button.background = ContextCompat.getDrawable(this, R.drawable.power_on)
    }

    private fun toggleSettingsNavigationUI(visible: Boolean) {
        val deniedPermissionAcceptText = findViewById<TextView>(R.id.permissionsText)
        val permissionsActivityButton = findViewById<Button>(R.id.grantMissingPermissionsButton)

        if (visible) {
            deniedPermissionAcceptText.visibility = View.VISIBLE
            permissionsActivityButton.visibility = View.VISIBLE
        } else {
            deniedPermissionAcceptText.visibility = View.GONE
            permissionsActivityButton.visibility = View.GONE
        }
    }

    private fun setupMainButton() {
        val mainButton: Button = findViewById(R.id.mainButton)

        if (missingRequiredPermissionsView)            // graying out the button to make it look disabled
            mainButton.background = ContextCompat.getDrawable(this, R.drawable.power_disabled)
        else           // restoring the button to its main style (power on background)
            mainButton.background = ContextCompat.getDrawable(this, R.drawable.power_on)

        mainButton.setOnClickListener {
            if (missingRequiredPermissionsView)
                Toast.makeText(this, "Some needed permissions are still required", Toast.LENGTH_LONG).show()
            else {
                if (appIsRecording)
                    stopAppServices()
                else
                    startAppServices()

                toggleButtonBackground(mainButton)
            }
        }
    }

    private fun onAllPermissionsGranted() {
        missingRequiredPermissionsView = false

        // initializing the classifier
        svmClassifier = SVMClassifier(this)

        // initializing the BT manager
        btBeaconManager = BTBeaconManager(this, configurationViewModel.getBeacons())
        btBeaconManager.startScanning()

        // initializing the list of known beacons from file on device persistent memory
        knownBeacons = btBeaconManager.getKnownBeacons()
        Log.i(LOG_TAG, "Found ${knownBeacons?.size} known beacons")

        // setting up onclick listeners on activity creation
        setupMainButton()

        // setting up where am i button
        setupWhereAmIButton()

        // setting up vibration service
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // initializing the accelerometer manager
        accelerometerManager = AccelerometerManager(this)

        // Start listening for config updates from companion app
        beaconsUpdateThread = ServerSocket(this, serverSocketUUID, configurationViewModel)
        beaconsUpdateThread.start()

    }

    private fun setupWhereAmIButton() {
        val whereAmIButton: Button = findViewById(R.id.whereAmIButton)
        val whereAmITextView: TextView = findViewById(R.id.whereAmIText)

        if (missingRequiredPermissionsView) {
            whereAmITextView.visibility = View.GONE
            whereAmIButton.visibility = View.GONE
        } else {
            whereAmITextView.visibility = View.VISIBLE
            whereAmIButton.visibility = View.VISIBLE
        }

        whereAmIButton.setOnClickListener {
            val closeBTBeacons = btBeaconManager.getBeacons()       // getting close beacons

            if (!knownBeacons.isNullOrEmpty()) {                // some known beacons have been registered
                if (closeBTBeacons.isNotEmpty()) {              // some beacons are close by
                    val closestBeaconLocation = getCurrentRoom(closeBTBeacons, knownBeacons)
                    if (closestBeaconLocation != null)
                        Toast.makeText(this, "You are here: ${closestBeaconLocation.uppercase()}", Toast.LENGTH_SHORT).show()
                    else
                        Toast.makeText(this, "No close known beacons found", Toast.LENGTH_SHORT).show()
                } else
                    Toast.makeText(this, "No close known beacons found", Toast.LENGTH_SHORT).show()
            } else
                Toast.makeText(this, "No beacons have been registered yet", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentRoom(closeBTBeacons: MutableMap<String, Beacon>, knownBeacons: MutableMap<String, Beacon>?): String? {
        if (knownBeacons.isNullOrEmpty() || closeBTBeacons.isEmpty())
            return null

        for ((room, knownBeacon) in knownBeacons) {
            if (closeBTBeacons.containsKey(knownBeacon.address))
                return room
        }

        return null
    }

    private val knownGestureReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                temporarilyStopKnownGestureReceiver()           // avoid receiving the following gestures for x seconds defined in 'delayGestureBroadcast'

                val recognizedGesture = it.getStringExtra("prediction") ?: "No prediction"

                showGestureRecognizedScreen(recognizedGesture)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showGestureRecognizedScreen(recognizedGesture: String) {
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val popupView = inflater.inflate(R.layout.popup_gesture_recognized, null)

        val popupWindow = PopupWindow(
            popupView,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            true
        )

        // Set background to semi-transparent black
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.parseColor("#80000000")))
        popupWindow.isOutsideTouchable = false

        // get close beacons. if no known beacon is close by, the gesture is useless, it doesn't activate anything
        val closeBTBeacons = btBeaconManager.getBeacons()
        val closestBeaconLocation = getCurrentRoom(closeBTBeacons, knownBeacons)
        val popupMainView: LinearLayout = popupView.findViewById(R.id.popup_main_parent)
        val messageTextView: TextView = popupView.findViewById(R.id.gesture_recognized_text)
        if (closestBeaconLocation.isNullOrEmpty()) {
            popupMainView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
            messageTextView.text = getString(R.string.no_known_beacons_are_near_you)
        } else {
            popupMainView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            messageTextView.text =
                getString(
                    R.string.gesture_in_room,
                    getString(R.string.gesture_recognized, recognizedGesture),
                    closestBeaconLocation
                )
        }

        val vibrationEffect = VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(vibrationEffect)

        popupWindow.showAtLocation(popupView, Gravity.CENTER, 0, 0)

        val fadeAfterMillis = 3000L
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            popupWindowFadeOut(popupWindow, popupView)
        }, fadeAfterMillis)
    }

    private fun popupWindowFadeOut(popupWindow: PopupWindow, popupView: View) {
        val animationDuration = 1000L // 1 second fadeout duration
        val fadeOut = ObjectAnimator.ofFloat(popupView, "alpha", 1f, 0f)
        fadeOut.duration = animationDuration
        fadeOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                popupWindow.dismiss()
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        fadeOut.start()
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
            knownBeacons = btBeaconManager.getKnownBeacons()            // simply reading again the file
        }
    }

    private fun startAppServices() {
        // enable accelerometer data gathering
        appIsRecording = true
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
    }

    private fun stopAppServices() {
        // stop accelerometer data gathering
        appIsRecording = false
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
    }

    private fun onPermissionsDenied() {
        Log.i(LOG_TAG, "onPermissionsDenied")

        missingRequiredPermissionsView = true

        // changing main button's style and behavior
        setupMainButton()

        // making where am i button invisible
        setupWhereAmIButton()

        // making text and button to go to settings visible
        toggleSettingsNavigationUI(true)

        val grantMissingPermissionsButton: Button = findViewById(R.id.grantMissingPermissionsButton)
        grantMissingPermissionsButton.setOnClickListener {
            permissionHandler.openAppSettings()
        }
    }
}