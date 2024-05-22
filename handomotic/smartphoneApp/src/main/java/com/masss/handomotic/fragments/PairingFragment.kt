package com.masss.handomotic.fragments

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.masss.handomotic.R

class PairingFragment : Fragment() {

    private val ACTION_BLUETOOTH_SELECTED =
        "android.bluetooth.devicepicker.action.DEVICE_SELECTED"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pairing, container, false)
    }

    private var bluetoothReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                if (device != null) {
                    Log.i("DEVICE_ADDRESS", device.address)
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
        showBluetoothSearch(requireActivity())
    }
}

