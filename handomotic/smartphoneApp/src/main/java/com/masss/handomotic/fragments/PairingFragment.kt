package com.masss.handomotic.fragments

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.EXTRA_DEVICE
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.MacAddress
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.masss.handomotic.R

class PairingFragment : Fragment() {

    private lateinit var deviceManager: CompanionDeviceManager
    private lateinit var deviceFilter: BluetoothDeviceFilter
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private val SELECT_DEVICE_REQUEST_CODE = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pairing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Using and initializing a CompanionDeviceManager objet in order
        // to show a device selection dialog to the user
        deviceManager = requireContext().getSystemService(Context.COMPANION_DEVICE_SERVICE)
        as CompanionDeviceManager

        deviceFilter = BluetoothDeviceFilter.Builder()
            //.setNamePattern(".*".toPattern()) // Regex to match any name
            .build()

        val pairingRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .build()

        val launcher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Handle the result
                Log.i("PAIR", "Device found!")
                val device : BluetoothDevice?
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    device = result.data?.getParcelableExtra(EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    // For older versions of Android
                    device = result.data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE) as? BluetoothDevice
                }
                if (device != null) {
                    Log.i("PAIR", "Device name: ${device.name}")
                    Log.i("PAIR", "Device class: ${device.bluetoothClass}")
                } else {
                    Log.i("PAIR", "No device found")
                }
            }
        }

        deviceManager.associate(
            pairingRequest,
            object : CompanionDeviceManager.Callback() {
                override fun onDeviceFound(chooserLauncher: IntentSender) {
                    // Launching the device chooser
                    launcher.launch(IntentSenderRequest.Builder(chooserLauncher).build())

                    Log.i("PAIR", "Device found! -2")
                }

                override fun onFailure(error: CharSequence?) {
                    // Handle the failure
                    Toast.makeText(context, "Pairing failed: $error", Toast.LENGTH_SHORT).show()
                }
            },
            null
        )

        // Retrieving the device names
        val bluetoothManager = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        bluetoothAdapter = bluetoothManager?.adapter ?: return
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
            return
        }
        val bondedDevices = bluetoothAdapter.bondedDevices
        bondedDevices.forEach { device ->
            val deviceHardwareAddress = device.address // MAC address
            val deviceName = device.name
            Log.i("DEVICE_HW_ADDR", deviceHardwareAddress)
            Log.i("DEVICE_NAME", deviceName)
        }
    }

}
