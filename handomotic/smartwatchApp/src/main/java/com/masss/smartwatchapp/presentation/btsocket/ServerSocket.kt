package com.masss.smartwatchapp.presentation.btsocket

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.util.UUID

class ServerSocket(private var context: Context,
    private var adapter: BluetoothAdapter, private val requiredPermissions: Array<String>,
    private val uuid: UUID) : Thread() {

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 1001
    }
    private lateinit var serverSocket: BluetoothServerSocket
    private lateinit var socket: BluetoothSocket
    private lateinit var inputStream: InputStream

    private fun handleReceivedData(jsonString: String) {
        try {

            val beacon = Json.decodeFromString<Array<RoomSetting>>(jsonString)

            // Handle the received configuration object as needed
            Log.i("ReceiveThread", "Received beacon: $beacon")

            // If needed, you can also send a response back
            // val response = "Response to received data"
            // outputStream.write(response.toByteArray())

            // Here you have to write on file / in memory the changes
        } catch (e: Exception) {
            Log.e("ReceiveThread", "Error handling received data", e)
        }
    }

    private fun receiveData() {
        try {
            val buffer = ByteArray(1024)
            var bytes: Int

            // Read from the InputStream
            bytes = inputStream.read(buffer)

            // Construct a string from the valid bytes in the buffer
            val jsonString = String(buffer, 0, bytes)

            // Handle the received data
            handleReceivedData(jsonString)
        } catch (e: IOException) {
            Log.e("ReceiveThread", "Error reading from input stream", e)
        }
    }

    override fun run() {
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

            serverSocket = adapter.listenUsingRfcommWithServiceRecord("YourServiceName", uuid)
            // Introduce an exit condition for the server!
            while(true) {
                // This will block until a connection is established
                socket = serverSocket.accept()

                // Connection accepted, get input and output streams
                inputStream = socket.inputStream

                // Start receiving data:
                receiveData()

                socket.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}