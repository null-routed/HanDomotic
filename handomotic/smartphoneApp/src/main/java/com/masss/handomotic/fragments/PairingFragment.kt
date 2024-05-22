package com.masss.handomotic.fragments

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.masss.handomotic.R
import com.masss.handomotic.ScanActivity

class PairingFragment : Fragment() {

    private var lastWatchName : String = ""
    private var lastWatchAddress: String = ""
    private lateinit var watchPairingButton : Button
    private lateinit var noWatchesPaired : TextView

    private val ACTION_BLUETOOTH_SELECTED =
        "android.bluetooth.devicepicker.action.DEVICE_SELECTED"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pairing, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private var bluetoothReceiver = object : BroadcastReceiver(){
        @SuppressLint("MissingPermission") // already checked when entered on the fragment
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                if (device != null) {
                    lastWatchName = device.name
                    lastWatchAddress = device.address

                   if(lastWatchName == ""){
                        noWatchesPaired.visibility = View.VISIBLE
                    } else {
                        Log.i("DEVICE_ADDRESS", lastWatchAddress)
                        Log.i("DEVICE_NAME", lastWatchName)
                        noWatchesPaired.visibility = View.GONE
                    }
                    Log.i("DEVICE_ADDRESS", device.address)
                    Log.i("DEVICE_NAME", device.name)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun showBluetoothSearch(activity: Activity) {
        activity.registerReceiver(bluetoothReceiver, IntentFilter(ACTION_BLUETOOTH_SELECTED))
        val bluetoothPicker = Intent("android.bluetooth.devicepicker.action.LAUNCH")
        activity.startActivity(bluetoothPicker)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // We have the need from the persistent storage, if any, the
        // address and name of the last paired smartwatch

        noWatchesPaired = view.findViewById(R.id.noWatches)
        watchPairingButton = view.findViewById(R.id.watchPairingButton)

        watchPairingButton.setOnClickListener(){
            showBluetoothSearch(requireActivity())
        }

    }
}

