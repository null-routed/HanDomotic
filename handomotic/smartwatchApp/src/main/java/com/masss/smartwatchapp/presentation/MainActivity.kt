package com.masss.smartwatchapp.presentation

import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.masss.smartwatchapp.R
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerRecordingService
import com.masss.smartwatchapp.presentation.classifier.SVMClassifier
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.gson.Gson
import com.masss.smartwatchapp.presentation.accelerometermanager.AccelerometerManager
import com.masss.smartwatchapp.presentation.btbeaconmanager.BTBeaconManager
import com.masss.smartwatchapp.presentation.btbeaconmanager.Beacon
import java.io.File


class MainActivity : AppCompatActivity() {

    /*
        TODO:
        put all button logic in a separate class ?
        put all permissions logic in a separate class ?
        integrate class for watch-mobile interaction

        make app keep running when screen is off (if mainButton is pressed)
    */

    private val LOG_TAG: String = "HanDomotic"

    // Tracker for the app state
    private var appIsRecording: Boolean = false
    private var missingRequiredPermissionsView: Boolean = false
    private val gestureReceiverHandler = Handler(Looper.getMainLooper())
    private val delayGestureBroadcast = 5000L       // seconds delay between two consecutive gesture recognitions

    // NEEDED PERMISSIONS
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.BODY_SENSORS,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    // CLASSIFIER
    private lateinit var svmClassifier: SVMClassifier

    // ACCELEROMETER MANAGER
    private lateinit var accelerometerManager: AccelerometerManager

    // BT MANAGEMENT AND SCANNING
    private lateinit var btBeaconManager: BTBeaconManager
    private lateinit var knownBeacons: List<BeaconTest>
    private val knownBeaconsFilename: String = "known-beacons.json"
    data class BeaconTest(val macAddress: String, val room: String)         // TEST

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()

        // TEST
        createTestBeaconsFile()

        // initializing the list of known beacons from file on device persistent memory
        knownBeacons = initializeOrUpdateKnownBeaconsList()
    }

    //TEST
    private fun createTestBeaconsFile() {
        val knownBeaconsTest = mapOf(
            "C9:8E:A0:EF:D5:77" to "Bedroom",
            "EE:89:D2:1D:90:51" to "Living Room"
        )

        val knownBeacons = knownBeaconsTest.map { (macAddress, room) ->
            BeaconTest(macAddress, room)
        }

        val gson = Gson()
        val jsonString = gson.toJson(knownBeacons)

        val file = File(this.filesDir, "known-beacons.json")
        file.writeText(jsonString)

        Log.d(LOG_TAG, "createTestBeaconsFile(): test file has been created")
    }

    // since the file will realistically have few entries we can use this method both to initialize the list and to update it when it changes
    private fun initializeOrUpdateKnownBeaconsList(): List<BeaconTest> {
        val file = File(this.filesDir, knownBeaconsFilename)
        if (!file.exists())
            return emptyList()

        val jsonString = file.readText()
        val gson = Gson()

        return gson.fromJson(jsonString, Array<BeaconTest>::class.java).toList()
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
        val deniedPermissions = permissions.filter { !it.value }.keys
        if (deniedPermissions.isEmpty())
            onAllPermissionsGranted()
        else {
            onPermissionsDenied(deniedPermissions)
        }
    }

    private fun toggleSettingsNavigationUI(visible: Boolean) {
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

    override fun onResume() {           // TODO: REFACTOR THIS
        super.onResume()
        Log.d(LOG_TAG, "onResume(): has been called")
        if (allPermissionsGranted()) {
            missingRequiredPermissionsView = false

            // making text and button to go to settings invisible
            toggleSettingsNavigationUI(false)

            // restoring main button's style and behavior if it was disabled because of some missing permissions
            setupMainButton()

            setupWhereAmIButton()

            // Registering accelerometer receiver
            registerReceiver(accelerometerManager.accelerometerReceiver, IntentFilter("AccelerometerData"), RECEIVER_EXPORTED)

            // instantiating the SVM classifier if it hasn't been yet and same goes for the BT manager
            if (!::svmClassifier.isInitialized)
                svmClassifier = SVMClassifier(this)
            if (!::btBeaconManager.isInitialized)
                btBeaconManager = BTBeaconManager(this)


            // Registering SVM BroadcastReceiver
            svmClassifier.registerReceiver()
        } else
            Toast.makeText(this, "Some needed permissions still have to be granted", Toast.LENGTH_LONG).show()
    }

    private fun allPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
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
        btBeaconManager = BTBeaconManager(this)
        btBeaconManager.startScanning()

        // setting up onclick listeners on activity creation
        setupMainButton()

        // setting up where am i button
        setupWhereAmIButton()

        // initializing the accelerometer manager
        accelerometerManager = AccelerometerManager(this)
    }

    private fun setupWhereAmIButton() {
        val whereAmIButton: Button = findViewById(R.id.whereAmIButton)

        whereAmIButton.setOnClickListener {
            val closeBTBeacons = btBeaconManager.getBeacons()

            if (knownBeacons.isNotEmpty()) {                // some known beacons have been registered
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

    private fun getCurrentRoom(closeBTBeacons: List<Beacon>, knownBeacons: List<BeaconTest>): String? {
        val knownBeaconsMap = knownBeacons.associateBy { it.macAddress }

        for (closeBeacon in closeBTBeacons) {
            val matchingBeacon = knownBeaconsMap[closeBeacon.address]
            if (matchingBeacon != null)
                return matchingBeacon.room
        }

        return null
    }

    private val knownGestureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                temporarilyStopKnownGestureReceiver()           // avoid receiving the following gestures for x seconds defined in 'delayGestureBroadcast'

                val recognizedGesture = it.getStringExtra("prediction") ?: "No prediction"

                showGestureRecognizedScreen(recognizedGesture)
            }
        }
    }

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
            messageTextView.text = "NO KNOWN BEACONS ARE NEAR YOU!"
        } else {
            popupMainView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
            messageTextView.text = "GESTURE RECOGNIZED: $recognizedGesture"
        }

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
                IntentFilter("com.masss.smartwatchapp.GESTURE_RECOGNIZED")
            )
        }, delayGestureBroadcast)
    }

    private fun startAppServices() {
        // enable accelerometer data gathering
        appIsRecording = true
        Toast.makeText(this, "Gesture recognition is active", Toast.LENGTH_SHORT).show()
        val accelRecordingIntent = Intent(this, AccelerometerRecordingService::class.java)
        startService(accelRecordingIntent)

        // Registering SVM BroadcastReceiver
        svmClassifier.registerReceiver()

        // Registering the gesture recognition broadcast receiver
        val filter = IntentFilter("com.masss.smartwatchapp.GESTURE_RECOGNIZED")
        registerReceiver(knownGestureReceiver, filter)
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
    }

    private fun onPermissionsDenied(deniedPermissions: Set<String>) {
        missingRequiredPermissionsView = true

        Toast.makeText(this, "App functionalities may be limited.", Toast.LENGTH_SHORT).show()

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
        val intent = Intent()
        intent.action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri

        startActivity(intent)
    }
}