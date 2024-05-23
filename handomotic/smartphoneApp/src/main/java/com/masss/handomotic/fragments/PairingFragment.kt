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
import androidx.fragment.app.activityViewModels
import com.masss.handomotic.R
import com.masss.handomotic.ScanActivity
import com.masss.handomotic.models.PairedDevice
import com.masss.handomotic.viewmodels.ConfigurationViewModel

class PairingFragment : Fragment() {

    private var lastWatchName : String = ""
    private var lastWatchAddress: String = ""
    private lateinit var watchPairingButton : Button
    private lateinit var noWatchesPaired : TextView
    private lateinit var watchName : TextView

    private val configurationViewModel: ConfigurationViewModel by activityViewModels()

    private val ACTION_BLUETOOTH_SELECTED =
        "android.bluetooth.devicepicker.action.DEVICE_SELECTED"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pairing, container, false)
    }


    private var bluetoothReceiver = object : BroadcastReceiver(){
        @SuppressLint("MissingPermission") // already checked when entered on the fragment
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                }
                if (device != null) {
                    lastWatchName = device.name
                    lastWatchAddress = device.address

                   if(lastWatchName == ""){
                       watchName.visibility = View.GONE
                       noWatchesPaired.visibility = View.VISIBLE
                    } else {
                        Log.i("DEVICE_ADDRESS", lastWatchAddress)
                        Log.i("DEVICE_NAME", lastWatchName)
                        noWatchesPaired.visibility = View.GONE
                        val msg = getString(R.string.pairedWatchMsg, lastWatchName)
                        watchName.text = msg

                       // Writing to file
                       val pairedDevice = PairedDevice(lastWatchName, lastWatchAddress)
                       configurationViewModel.addPairedDevice(pairedDevice, requireContext())
                    }
                }
            }
        }
    }

    fun showBluetoothSearch(activity: Activity) {
        activity.registerReceiver(bluetoothReceiver, IntentFilter(ACTION_BLUETOOTH_SELECTED))
        val bluetoothPicker = Intent("android.bluetooth.devicepicker.action.LAUNCH")
        activity.startActivity(bluetoothPicker)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        // We have the need from the persistent storage, if any, the
        // address and name of the last paired smartwatch
        noWatchesPaired = view.findViewById(R.id.noWatches)
        watchName = view.findViewById(R.id.watchName)
        watchPairingButton = view.findViewById(R.id.watchPairingButton)

        val pairedDevice = configurationViewModel.getPairedDevice()
        if(pairedDevice == null) {
            watchName.visibility = View.GONE
            noWatchesPaired.visibility = View.VISIBLE
        } else{
            noWatchesPaired.visibility = View.GONE
            val msg = getString(R.string.pairedWatchMsg, pairedDevice.deviceName)
            watchName.text = msg
            watchName.visibility = View.VISIBLE
        }
        watchPairingButton.setOnClickListener(){
            showBluetoothSearch(requireActivity())
        }

    }
}

