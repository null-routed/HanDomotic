package com.masss.handomotic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.masss.handomotic.databinding.ScanActivityBinding

class ScanActivity : ComponentActivity() {

    // =====================================
    // TO REMOVE: FOR TESTING PURPOSES ONLY
    private val testing = 1
    // =====================================

    private lateinit var beaconAdapter: BeaconsAdapter
    private lateinit var binding: ScanActivityBinding
    private lateinit var beaconManager: BTBeaconManager
    private var wasScanning = false

    private lateinit var updateHandler: Handler
    private lateinit var updateRunnable: Runnable

    companion object {
        const val REQUEST_CODE_PERMISSIONS: Int = 100
        const val TAG = "BTBeaconManager"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScanActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.greeting.text = " Wake up, samurai! "
        checkPermissions()

        // create the beacon manager instance for Bluetooth scanning
        beaconManager = BTBeaconManager(this)
        beaconManager.stopScanning()

        // set up the recycler view
        updateHandler = Handler(Looper.getMainLooper())
        updateRunnable = Runnable {
            // Update the RecyclerView
            updateRecyclerView()
            // Schedule the next update
            updateHandler.postDelayed(updateRunnable, 1000) // Aggiorna ogni secondo
        }

        binding.scanningSwitch.setOnClickListener {
            if (beaconManager.isScanning()) {
                stopBeaconScan()
            } else {
                startBeaconScan()
            }
        }
    }

    private fun updateRecyclerView() {
        // === TO EDIT: TESTING PURPOSES === //

        // ================================ //
        // Update the adapter of the RecyclerView
        binding.beaconsRecycler.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)

        // here you have to pass only beacons to add! Not already added beacons..
        beaconAdapter = BeaconsAdapter(beaconManager)
        binding.beaconsRecycler.adapter = beaconAdapter
    }

    override fun onStart() {
        super.onStart()
        if (wasScanning) {
            startBeaconScan()
        }
    }

    override fun onStop() {
        if (beaconManager.isScanning()) {
            wasScanning = true
            stopBeaconScan()
        }
        super.onStop()
    }


    override fun onResume() {
        super.onResume()
        if (wasScanning) {
            startBeaconScan()
        }
    }

    override fun onPause() {
        super.onPause()
        if (beaconManager.isScanning()) {
            wasScanning = true
            stopBeaconScan()
        }
    }

    override fun onDestroy() {
        beaconManager.destroy()
        super.onDestroy()
    }

    private fun startBeaconScan() {
        Log.i(TAG, "Start scanning")
        binding.greeting.text = "Scanning..."
        binding.scanningProgress.visibility = View.VISIBLE
        beaconManager.startScanning()
        updateHandler.post(updateRunnable)
    }

    private fun stopBeaconScan() {
        Log.i(TAG, "Stop scanning")
        binding.greeting.text = "Not scanning"
        binding.scanningProgress.visibility = View.GONE
        beaconManager.stopScanning()
        updateHandler.removeCallbacks(updateRunnable)
    }

    private fun checkPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        else arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (isAnyOfPermissionsNotGranted(requiredPermissions)) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                ScanActivity.REQUEST_CODE_PERMISSIONS
            )
        }
    }
    private fun isAnyOfPermissionsNotGranted(requiredPermissions: Array<String>): Boolean {
        for (permission in requiredPermissions) {
            val checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, permission)
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult) {
                return true
            }
        }
        return false
    }
}