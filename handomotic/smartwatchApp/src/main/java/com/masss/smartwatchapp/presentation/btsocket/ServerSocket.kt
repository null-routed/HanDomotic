package com.masss.smartwatchapp.presentation.btsocket

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.viewmodels.ConfigurationViewModel
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.util.UUID


class ServerSocket(
    private var context: Context,
    private val uuid: UUID,
    private val configurationViewModel: ConfigurationViewModel)
        : Thread() {

    private val TAG = "SERVER_SOCKET"

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 1001
    }
    private lateinit var adapter: BluetoothAdapter
    private lateinit var serverSocket: BluetoothServerSocket
    private lateinit var socket: BluetoothSocket
    private lateinit var inputStream: InputStream

    @Volatile
    private var running = true

    private fun handleReceivedData(jsonString: String) {
        try {
            configurationViewModel.flushBeacons()
            val beacons = Json.decodeFromString<Array<Beacon>>(jsonString)
            for (beacon in beacons)
                configurationViewModel.addBeacon(beacon, context)

            val beaconsAsStr = beacons.joinToString(separator = ",")
            Log.i(TAG, "Received: $beaconsAsStr from companion app")

            // notify main activity to update known beacons
            val beaconUpdateIntent = Intent("CompanionApp_ReceivedBeaconUpdate")
            context.sendBroadcast(beaconUpdateIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling received data", e)
        }
    }

    private fun receiveData() {
        try {
            val buffer = ByteArray(4096)

            // Read from the InputStream
            val bytes: Int = inputStream.read(buffer)

            // Construct a string from the valid bytes in the buffer
            val jsonString = String(buffer, 0, bytes)

            // Handle the received data
            handleReceivedData(jsonString)
        } catch (e: IOException) {
            Log.e(TAG, "Error reading from input stream", e)
        }
    }

    override fun run() {
        Log.i(TAG, "Thread started. Listening for beacon updates...")

        // Obtaining the bluetooth adapter
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        if(manager != null)
            adapter = manager.adapter

        try {
            // Create a server socket
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(
                        context as Activity,
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        REQUEST_BLUETOOTH_PERMISSION
                    )
                }
            }

            serverSocket = adapter.listenUsingRfcommWithServiceRecord("BeaconUpdatesFromCompanionApp", uuid)

            while(running) {
                Log.i(TAG, "Thread is blocked waiting for a connection")

                socket = serverSocket.accept()  // This will block until a connection is established

                inputStream = socket.inputStream        // Connection accepted, get input and output streams

                receiveData()   // Start receiving data:

                socket.close()
            }
        } catch (e: IOException) {
            if (running)
                Log.e(TAG, "Error accepting connection", e)
            else
                Log.i(TAG, "Thread stopped")
        }
    }

    fun stopServer() {
        running = false
        try {
            serverSocket.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }
    }

}