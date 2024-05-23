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
import com.masss.smartwatchapp.presentation.btbeaconmanager.Beacon
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.util.UUID


class ServerSocket(
    private var context: Context,
    private val uuid: UUID)
        : Thread() {

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 1001
    }
    private lateinit var adapter: BluetoothAdapter
    private lateinit var serverSocket: BluetoothServerSocket
    private lateinit var socket: BluetoothSocket
    private lateinit var inputStream: InputStream

    private fun handleReceivedData(jsonString: String) {
        try {
            val beacon = Json.decodeFromString<Array<Beacon>>(jsonString)

            Log.i("ReceiveThread", "Received beacon: $beacon")

            // Update the configuration file with the changes


            // notify main activity to update known beacons
            val beaconUpdateIntent = Intent("com.masss.smartwatchapp.BEACON_UPDATE")
            context.sendBroadcast(beaconUpdateIntent)
        } catch (e: Exception) {
            Log.e("ReceiveThread", "Error handling received data", e)
        }
    }

    private fun receiveData() {
        try {
            val buffer = ByteArray(1024)

            // Read from the InputStream
            val bytes: Int = inputStream.read(buffer)

            // Construct a string from the valid bytes in the buffer
            val jsonString = String(buffer, 0, bytes)

            // Handle the received data
            handleReceivedData(jsonString)
        } catch (e: IOException) {
            Log.e("ReceiveThread", "Error reading from input stream", e)
        }
    }

    override fun run() {
        // Obtaining the bluetooth adapter
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        if(manager != null){
            adapter = manager.adapter
        }
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

            while(true) {
                Log.i("THREAD", "Thread is blocked waiting for a connection")
                // This will block until a connection is established
                socket = serverSocket.accept()

                // Connection accepted, get input and output streams
                inputStream = socket.inputStream

                // Start receiving data:
                receiveData()

                socket.close()

                // TODO: EXIT CONDITION
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}