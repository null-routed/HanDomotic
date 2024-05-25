package com.masss.handomotic.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.masss.handomotic.R
import com.masss.handomotic.models.PairedDevice
import com.masss.handomotic.viewmodels.ConfigurationViewModel

class PairingFragment : Fragment() {

    private var lastWatchName : String = ""
    private var lastWatchAddress: String = ""
    private lateinit var watchPairingButton : Button
    private lateinit var noWatchesPaired : TextView
    private lateinit var watchName : TextView

    private val configurationViewModel: ConfigurationViewModel by activityViewModels()

    private val actionBluetoothSelected =
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
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                if (device != null) {
                    lastWatchName = device.name
                    lastWatchAddress = device.address

                   if(lastWatchName == ""){
                       watchName.visibility = View.GONE
                       noWatchesPaired.visibility = View.VISIBLE
                    } else {
                        noWatchesPaired.visibility = View.GONE
                        watchName.visibility = View.VISIBLE
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

    private fun showBluetoothSearch(activity: Activity) {
        activity.registerReceiver(bluetoothReceiver, IntentFilter(actionBluetoothSelected))
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
            noWatchesPaired.text = getString(R.string.noWatches)
        } else{
            noWatchesPaired.text = getString(R.string.pairedWatchMsg)
            watchName.text = pairedDevice.deviceName
            watchName.visibility = View.VISIBLE
        }
        watchPairingButton.setOnClickListener {
            showBluetoothSearch(requireActivity())
        }

    }
}

