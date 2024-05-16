package it.unipi.bt_test_20.presentation

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Group
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import it.unipi.bt_test_20.R
import androidx.fragment.app.Fragment


class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var beaconsList: RecyclerView
    private val beaconsMap = mutableMapOf<String, ScanResult>()


    private val knownBeacons = mapOf(
        "DE:AE:60:94:82:88" to "Bedroom",
        "FD:EA:D9:76:AA:CD" to "Living Room",
        "C4:EC:29:4D:5B:1B" to "Bathroom",
        "FD:EA:D9:76:AA:CD" to "Attic",
    )

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

        // Check if the current device is a watch
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH)) {
            setContentView(R.layout.activity_main)
        } else {
            setContentView(R.layout.smartphone_main)
        }

        checkPermissions()

        val btn_hide = findViewById<Button>(R.id.btn_hide)
        val group = findViewById<Group>(R.id.grp_color_btns)

        btn_hide.setOnClickListener {
            if (group.visibility == View.VISIBLE) {
                group.visibility = View.GONE
            } else {
                group.visibility = View.VISIBLE
            }
        }

        // Inizializza la RecyclerView
        beaconsList = findViewById(R.id.beacons_list)
        beaconsList.layoutManager = LinearLayoutManager(this)
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
        // Initialize the Bluetooth adapter and scanner
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        // Configure the scan settings
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        // Start scanning for BLE devices
        bluetoothLeScanner.startScan(null, scanSettings, scanCallback)
    }

    private fun stopScan() {
        // check for the necessary permissions
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d("BT_SCAN", "stopScan(): permission not granted, returning")
            return
        }
        // Check if bluetoothLeScanner has been initialized before trying to access it
        if (::bluetoothLeScanner.isInitialized) {
            bluetoothLeScanner.stopScan(scanCallback)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            val device = result?.device ?: return

            // Verifica se è un beacon BLE
            val scanRecord = result.scanRecord ?: return
            if (!isBleBeacon(scanRecord)) return

            // Aggiungi o aggiorna il beacon nella mappa
            beaconsMap[device.address] = result

            // Ordina i risultati per RSSI decrescente
            val sortedBeaconsList = beaconsMap.values.sortedByDescending { it.rssi }

            // Aggiorna la RecyclerView
            beaconsList.adapter = BeaconsAdapter(this@MainActivity, listOf())
            runOnUiThread {
                (beaconsList.adapter as BeaconsAdapter).updateBeacons(sortedBeaconsList)
            }


//            if (device?.address == BEACON_1_MAC) {
//                currentBeacon1Scan = result
//                Log.d("BT_SCAN_RESULT", "BEACON 1: $result")
//            }
//            else if (device?.address == BEACON_2_MAC) {
//                currentBeacon2Scan = result
//                Log.d("BT_SCAN_RESULT", "BEACON 2: $result")
//            }
//
//            if (currentBeacon1Scan != null && currentBeacon2Scan != null)
//                checkTargetBeaconsProximity(currentBeacon1Scan!!, currentBeacon2Scan!!)

//            val beaconInfo = "Device: ${device?.address}, RSSI: $rssi, Data: ${data?.contentToString()}"
//            Log.d("BT_SCAN", "scanCallback@onScanResult(): $beaconInfo")
        }

        // Funzione per determinare se il dispositivo è un beacon BLE
        private fun isBleBeacon(scanRecord: ScanRecord): Boolean {
            // Verifica se ci sono dati del produttore nel record di scansione
            val manufacturerData = scanRecord.getManufacturerSpecificData()
            return manufacturerData.size() > 0
        }

        inner class BeaconsAdapter(
            private val context: Context,
            private var beacons: List<ScanResult>
        ) : RecyclerView.Adapter<BeaconsAdapter.ViewHolder>() {

            inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
                val beaconInfo: TextView = view.findViewById(R.id.beacon_info)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
                val view = LayoutInflater.from(context).inflate(R.layout.beacon_item, parent, false)
                return ViewHolder(view)
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                val scanResult = beacons[position]
                val device = scanResult.device
                val rssi = scanResult.rssi

                // Controlla se il beacon è conosciuto e usa il nickname se disponibile
                val displayName = knownBeacons[device.address] ?: device.address
                if(knownBeacons[device.address] != null) {
                    Log.i("BT_SCAN", "Beacon $device.address is known as $displayName")
                }
                holder.beaconInfo.text = "Device: $displayName, RSSI: $rssi"
            }

            override fun getItemCount(): Int {
                return beacons.size
            }

            fun updateBeacons(newBeacons: List<ScanResult>) {
                beacons = newBeacons
                notifyDataSetChanged()
            }
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