package it.unipi.bt_test_20.presentation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import it.unipi.bt_test_20.R


class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var beaconTextView: TextView

    private val BEACON_1_MAC: String = "EE:89:D2:1D:90:51"
    private val BEACON_2_MAC: String = "C9:8E:A0:EF:D5:77"
    private val BEACON_1_LOCATION: String = "Bedroom"
    private val BEACON_2_LOCATION: String = "Living room"
    private var currentBeacon1Scan: ScanResult? = null
    private var currentBeacon2Scan: ScanResult? = null

    private val REQUIRED_PERMISSIONS = arrayOf(
        android.Manifest.permission.BLUETOOTH_ADMIN,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION,
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.BLUETOOTH_SCAN
    )
    private val REQUEST_CODE_PERMISSIONS = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        beaconTextView = findViewById(R.id.beaconTextView)

        checkPermissions()
    }

    private fun checkPermissions() {
        Log.d("BT_SCAN", "checkPermissions(): requesting permissions...")
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            REQUEST_CODE_PERMISSIONS
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("BT_SCAN", "onRequestPermissionResult(): Permission granted, starting scan...")
                startBluetoothScanning()
            } else {
                Log.d("BT_SCAN", "onRequestPermissionResult(): Permission denied")
                // Handle permission denied case here
                // You can show an error message to the user or take appropriate action
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopScan()
    }



    private fun startBluetoothScanning() {
        // check for the necessary permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("BT_SCAN", "startBluetoothScanning(): permission not granted, returning")
            return
        }

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    private fun stopScan() {
        // check for the necessary permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("BT_SCAN", "stopScan(): permission not granted, returning")
            return
        }
        bluetoothLeScanner.stopScan(scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device
//            val rssi = result?.rssi
//            val data = result?.scanRecord?.bytes

            Log.d("BT_SCAN", "@onScanResult(): completed a scan cycle")

            if (device?.address == BEACON_1_MAC)
                currentBeacon1Scan = result
            else if (device?.address == BEACON_2_MAC)
                currentBeacon2Scan = result

            if (currentBeacon1Scan != null && currentBeacon2Scan != null)
                checkTargetBeaconsProximity(currentBeacon1Scan!!, currentBeacon2Scan!!)

//            val beaconInfo = "Device: ${device?.address}, RSSI: $rssi, Data: ${data?.contentToString()}"
//            Log.d("BT_SCAN", "scanCallback@onScanResult(): $beaconInfo")
        }
    }

    private fun checkTargetBeaconsProximity(currentBeacon1Scan: ScanResult, currentBeacon2Scan: ScanResult) {
        val RSSI1 = currentBeacon1Scan.rssi
        val RSSI2 = currentBeacon2Scan.rssi

        if (RSSI1 > RSSI2) {
            Log.d("BT_SCAN_RESULT", "BEACON ${currentBeacon1Scan.device} IS CLOSER THAN ${currentBeacon2Scan.device}")
            Log.d("BT_SCAN_RESULT", "Smartwatch is here: $BEACON_1_LOCATION")
        } else {
            Log.d("BT_SCAN_RESULT", "BEACON ${currentBeacon2Scan.device} IS CLOSER THAN ${currentBeacon1Scan.device}")
            Log.d("BT_SCAN_RESULT", "Smartwatch is here: $BEACON_2_LOCATION")
        }
    }
}