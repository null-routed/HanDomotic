package com.masss.handomotic

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.masss.handomotic.databinding.ScanActivityBinding
import com.masss.handomotic.models.Beacon

class ScanActivity : AppCompatActivity() {

    private lateinit var beaconAdapter: BeaconsAdapter
    private lateinit var binding: ScanActivityBinding
    private lateinit var beaconManager: BTBeaconManager
    private var wasScanning = false

    companion object {
        const val REQUEST_CODE_PERMISSIONS: Int = 100
        const val TAG = "BTBeaconManager"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.title = "Beacons pairing"

        binding = ScanActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val knownBeaconsList: List<Beacon> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra("beacons", Beacon::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra<Beacon>("beacons").orEmpty()
        }
        val knownBeaconsMap = knownBeaconsList.associateBy { it.address }.toMutableMap()

        // create the beacon manager instance for Bluetooth scanning
        beaconManager = BTBeaconManager(this, knownBeaconsMap, ::updateRecyclerView)

        beaconManager.stopScanning()

        binding.scanningSwitch.setOnClickListener {
            if (beaconManager.isScanning()) {
                stopBeaconScan()
            } else {
                startBeaconScan()
            }
        }
    }

    private fun updateRecyclerView() {
        // Update the adapter of the RecyclerView
        binding.beaconsRecycler.layoutManager =
            androidx.recyclerview.widget.LinearLayoutManager(this)

        // here you have to pass only beacons to add! Not already added beacons..
        beaconAdapter = BeaconsAdapter(beaconManager, this)
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
        binding.greeting.text = getString(R.string.startBeaconScan)
        binding.scanningProgress.visibility = View.VISIBLE
        beaconManager.startScanning()
    }

    private fun stopBeaconScan() {
        Log.i(TAG, "Stop scanning")
        binding.greeting.text = getString(R.string.stopBeaconScan)
        binding.scanningProgress.visibility = View.INVISIBLE
        beaconManager.stopScanning()
    }
}