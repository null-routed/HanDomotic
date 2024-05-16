package it.unipi.bt_test_20.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kontakt.sdk.android.ble.configuration.ScanMode
import com.kontakt.sdk.android.ble.configuration.ScanPeriod
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener
import com.kontakt.sdk.android.ble.manager.ProximityManager
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener
import com.kontakt.sdk.android.ble.rssi.RssiCalculators
import com.kontakt.sdk.android.common.KontaktSDK
import com.kontakt.sdk.android.common.profile.IBeaconDevice
import com.kontakt.sdk.android.common.profile.IBeaconRegion
import it.unipi.bt_test_20.R
import java.util.concurrent.TimeUnit

class MainActivity2 : AppCompatActivity() {
    private var proximityManager: ProximityManager? = null
    private var progressBar: ProgressBar? = null
    private var scanStatus: TextView? = null
    private var startStopScanning: Button? = null

    companion object {
        const val REQUEST_CODE_PERMISSIONS: Int = 100
    }

    private val TAG = "MainActivity2"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        KontaktSDK.initialize("666")

        progressBar = findViewById(R.id.scanning_progress)!!

        checkPermissions()

        Log.i(TAG, "Setup proximity manager")
        setupProximityManager()

        Log.i(TAG, "Setup START / STOP Scan button")
        startStopScanning = findViewById(R.id.start_stop_scanning)
        scanStatus = findViewById(R.id.scan_status)

        // if user pres R.id.start_stop_scanning call startScanning() or stopScanning() like a switch
        startStopScanning!!.setOnClickListener{
            if (proximityManager!!.isScanning) {
                Log.i(TAG, "Stop scanning")
                scanStatus?.text = "Not scanning"
                progressBar?.visibility = View.GONE
                stopScanning()
            } else {
                Log.i(TAG, "Start scanning")
                scanStatus?.text = "Scanning..."
                progressBar?.visibility = View.VISIBLE
                startScanning()
            }
        }

    }

    /*
    GESTIONE PERMESSI
    */

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
                MainActivity2.REQUEST_CODE_PERMISSIONS
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

    private fun setupProximityManager() {
        proximityManager = ProximityManagerFactory.create(this)

        //Configure proximity manager basic options
        proximityManager?.configuration()
            ?.scanMode(ScanMode.BALANCED)
            ?.scanPeriod(ScanPeriod.RANGING)
            //?.activityCheckConfiguration(ActivityCheckConfiguration.DISABLED)
            //?.forceScanConfiguration(ForceScanConfiguration.DISABLED)
            ?.deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(5))
            ?.rssiCalculator(RssiCalculators.DEFAULT)
            //?.cacheFileName("Example")
            ?.resolveShuffledInterval(3)
            ?.monitoringEnabled(true)
            ?.monitoringSyncInterval(10)
        //?.secureProfilePayloadResolvers(Collections.emptyList())
        //?.kontaktScanFilters(KontaktScanFilter.DEFAULT_FILTERS_LIST)

        //Setting up iBeacon and Eddystone listeners
        proximityManager?.setIBeaconListener(createIBeaconListener())
    }

    override fun onStart() {
        super.onStart()
        scanStatus?.text = "Not scanning"
    }

    override fun onStop() {
        proximityManager!!.stopScanning()
        super.onStop()
    }

    override fun onDestroy() {
        proximityManager!!.disconnect()
        proximityManager = null
        super.onDestroy()
    }

    private fun startScanning() {
        //Connect to scanning service and start scanning when ready
        proximityManager!!.connect(OnServiceReadyListener {
            //Check if proximity manager is already scanning
            if (proximityManager!!.isScanning) {
                Toast.makeText(
                    this@MainActivity2,
                    "Already scanning",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnServiceReadyListener
            }
            proximityManager!!.startScanning()
            progressBar!!.visibility = View.VISIBLE
            Toast.makeText(this@MainActivity2, "Scanning started", Toast.LENGTH_SHORT)
                .show()
        })
    }

    private fun stopScanning() {
        //Stop scanning if scanning is in progress
        if (proximityManager!!.isScanning) {
            proximityManager!!.stopScanning()
            progressBar!!.visibility = View.GONE
            Toast.makeText(this, "Scanning stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createIBeaconListener(): IBeaconListener {
        return object : IBeaconListener {
            override fun onIBeaconDiscovered(iBeacon: IBeaconDevice, region: IBeaconRegion) {
                Log.i(TAG, "onIBeaconDiscovered: $iBeacon")
            }

            override fun onIBeaconsUpdated(iBeacons: List<IBeaconDevice>, region: IBeaconRegion) {
                Log.i(TAG, "onIBeaconsUpdated: " + iBeacons.size)
            }

            override fun onIBeaconLost(iBeacon: IBeaconDevice, region: IBeaconRegion) {
                Log.e(TAG, "onIBeaconLost: $iBeacon")
            }
        }
    }
}