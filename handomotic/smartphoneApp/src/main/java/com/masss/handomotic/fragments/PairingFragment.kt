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
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.masss.handomotic.R
import com.masss.handomotic.ScanActivity

class PairingFragment : Fragment() {

    // ================ THESE CODE HAS TO BE MOVED IN A PROPER PACKAGE, AS IT'S ALREADY
    // USED IN ScanActivity.kt =======================================================
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
                requireActivity(),
                requiredPermissions,
                ScanActivity.REQUEST_CODE_PERMISSIONS
            )
        }
    }
    private fun isAnyOfPermissionsNotGranted(requiredPermissions: Array<String>): Boolean {
        for (permission in requiredPermissions) {
            val checkSelfPermissionResult = ContextCompat.checkSelfPermission(requireContext(), permission)
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult) {
                return true
            }
        }
        return false
    }

    private val ACTION_BLUETOOTH_SELECTED =
        "android.bluetooth.devicepicker.action.DEVICE_SELECTED"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        checkPermissions()
        return inflater.inflate(R.layout.fragment_pairing, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private var bluetoothReceiver = object : BroadcastReceiver(){
        @SuppressLint("MissingPermission") // already checked when entered on the fragment
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                val device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                if (device != null) {
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
        showBluetoothSearch(requireActivity())
    }
}

