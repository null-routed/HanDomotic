package com.masss.handomotic.btsocket

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// selAddress is the Bluetooth L2 address of the smartwatch, to be selected among a list of
// associated devices or assuming that the device is always the same
class ClientThread(private var context: Context, private var adapter: BluetoothAdapter,
    private val requiredPermissions : Array<String>, private val selAddress : String, private val config: Array<RoomSetting>) : Thread(){

    private val TAG : String = "SERIALIZER"
    /* This UUID is randomly generated at https://www.guidgenerator.com/ */
    private val uuid: UUID = UUID.fromString("bffdf9d2-048d-45cb-b621-3025760dc306")

    private lateinit var device: BluetoothDevice
    private lateinit var socket: BluetoothSocket
    private lateinit var outputStream: OutputStream

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSION = 1001
    }

    // This function makes the serialization of the Beacon object in JSON format
    private fun sendBeacon(beacon: Array<RoomSetting>){
        try{
            val jsonString = Json.encodeToString(beacon)
            // Converting the jsonString into bytes to flow them in the OutputStream
            val bytes = jsonString.toByteArray()
            outputStream.write(bytes)
            Log.i(TAG, "Configuration sent")
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    // This method is called once the connection is estabilished
    private fun manageConnectedSocket(){
        try{
            outputStream = socket.outputStream
            sendBeacon(config)
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun run(){
        // Creating the Bluetooth Socket
        device = adapter.getRemoteDevice(selAddress)
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ){
            // Asks permissions if not granted
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    REQUEST_BLUETOOTH_PERMISSION
                )
            }
            return
        }

        // Connection using the Bluetooth socket
        try{
            socket = device.createRfcommSocketToServiceRecord(uuid)
            adapter.cancelDiscovery()
            Log.i(TAG, "Socket created")
            socket.connect()
            manageConnectedSocket()
        }catch(e: IOException){
            Log.e(TAG, "Error during connection", e)
        }
    }
}
