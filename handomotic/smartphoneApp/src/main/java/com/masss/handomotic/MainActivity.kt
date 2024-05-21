package com.masss.handomotic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.masss.handomotic.databinding.HomeBinding

class MainActivity : ComponentActivity() {

    // =====================================
    // TO REMOVE: FOR TESTING PURPOSES ONLY
    private val testing = 1
    // =====================================

    public lateinit var beaconAdapter: BeaconsAdapter
    private lateinit var binding: HomeBinding
    private lateinit var beaconManager: BTBeaconManager
    private var wasScanning = false

    private lateinit var updateHandler: Handler
    private lateinit var updateRunnable: Runnable

    companion object {
        const val REQUEST_CODE_PERMISSIONS: Int = 100
        const val TAG = "BTBeaconManager"
        val checkedItems = HashMap<Int, String>()

        // This function retrieves a key value pair list with everything belonging to the HashMap
        fun getCheckedItems(): List<Pair<Int, String>> {
            return checkedItems.map { it.toPair() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.home)
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
        // Obtain the updated list of beacons
        val beacons = beaconManager.getBeacons(true)
        // ================================ //
        // Update the adapter of the RecyclerView
        binding.beaconsRecycler.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)
        beaconAdapter = BeaconsAdapter(beacons)
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
        // Get checked indices
        val checkItems = getCheckedItems()
        Log.i("CHECKBOX_ITEM", "getCheckedItems called..")
        Log.i("CHECKBOX_ITEM", "${checkItems.size}")
        for(item in checkItems){
            Log.i("CHECKBOX_ITEM", "${item.first}, ${item.second}")
        }
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
                MainActivity.REQUEST_CODE_PERMISSIONS
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

    class BeaconsAdapter(private var beaconsList: List<Beacon>) : RecyclerView.Adapter<BeaconsAdapter.BeaconsHolder>() {

        class BeaconsHolder(private val row: View) : RecyclerView.ViewHolder(row) {
            val beaconAddress: TextView = row.findViewById(R.id.beacon_address)
            val beaconUuid: TextView = row.findViewById(R.id.beacon_uuid)
            val beaconRssi: TextView = row.findViewById(R.id.beacon_rssi)
            val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconsHolder {
            val layout = LayoutInflater.from(parent.context).inflate(R.layout.beacon_item, parent, false)
            return BeaconsHolder(layout)
        }

        override fun onBindViewHolder(holder: BeaconsHolder, position: Int) {
            val beacon = beaconsList[position]
            holder.beaconAddress.text = beacon.address
            holder.beaconUuid.text = beacon.id
            holder.beaconRssi.text = beacon.rssi.toString()
            holder.checkBox.isChecked = checkedItems.containsKey(position) // returns a boolean

            // Handler that is executed when the check is pressed
            // The HashMap contains the address associated to the position in list (used as key)
            holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
                Log.i("BUTTON", "Pressed..")
                if (isChecked) {
                    checkedItems[position] = beacon.address
                } else {
                    checkedItems.remove(position)
                }
            }
        }

        override fun getItemCount(): Int = beaconsList.size

        fun updateBeacons(newBeacons: List<Beacon>) {
            beaconsList = newBeacons
            notifyDataSetChanged()
        }

    }





















}