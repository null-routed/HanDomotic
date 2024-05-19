package com.masss.handomotic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.masss.handomotic.databinding.HomeBinding

class MainActivity : ComponentActivity() {
    private lateinit var binding: HomeBinding
    private lateinit var beaconManager: BTBeaconManager

    private lateinit var updateHandler: Handler
    private lateinit var updateRunnable: Runnable

    companion object {
        const val REQUEST_CODE_PERMISSIONS: Int = 100
        const val TAG = "BTBeaconManager"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.home)
        binding.greeting.text = "Wake up, samurai!"
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
                Log.i(TAG, "Stop scanning")
                binding.greeting.text = "Not scanning"
                binding.scanningProgress.visibility = View.GONE
                beaconManager.stopScanning()
                updateHandler.removeCallbacks(updateRunnable)
            } else {
                Log.i(TAG, "Start scanning")
                binding.greeting.text = "Scanning..."
                binding.scanningProgress.visibility = View.VISIBLE
                beaconManager.startScanning()
                updateHandler.post(updateRunnable)
            }
        }
    }

    private fun updateRecyclerView() {
        // Obtain the updated list of beacons
        val beacons = beaconManager.getBeacons()
        // Update the adapter of the RecyclerView
        binding.beaconsRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.beaconsRecycler.adapter = BeaconsAdapter(beacons)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        beaconManager.stopScanning()
        super.onStop()
    }

    override fun onDestroy() {
        beaconManager.destroy()
        super.onDestroy()
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

    class BeaconsAdapter(private val beaconsList: List<Beacon>) : RecyclerView.Adapter<BeaconsAdapter.BeaconsHolder>() {
        class BeaconsHolder(private val row: View) : RecyclerView.ViewHolder(row) {
            val beaconAddress: TextView = row.findViewById(R.id.beacon_address)
            val beaconUuid: TextView = row.findViewById(R.id.beacon_uuid)
            val beaconRssi: TextView = row.findViewById(R.id.beacon_rssi)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconsHolder {
            val layout = LayoutInflater.from(parent.context).inflate(R.layout.beacon_item, parent, false)
            return BeaconsHolder(layout)
        }

        override fun onBindViewHolder(holder: BeaconsHolder, position: Int) {
            holder.beaconAddress.text = beaconsList[position].address
            holder.beaconUuid.text = beaconsList[position].id
            holder.beaconRssi.text = beaconsList[position].rssi.toString()
        }

        override fun getItemCount(): Int = beaconsList.size
    }





















}