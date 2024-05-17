package com.masss.smartwatchapp.presentation

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.masss.smartwatchapp.R
import kotlin.properties.Delegates

class MainActivity : AppCompatActivity() {

    private val tag: String = "HanDomotic"

    private val requiredPermissions = arrayOf(
        android.Manifest.permission.BODY_SENSORS,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //TODO: show app name following device's screen curvature ?

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
            startAppWork()
        }
    }

    private fun startAppWork() {

    }

    private fun onPermissionsDenied() {
        /*
        TODO: make msg appear saying what are the permissions still needed
            for the app to work
            Show button to request those missing permissions
        */
    }
}