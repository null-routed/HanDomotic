package com.masss.handomotic.btsocket

import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.masss.handomotic.models.Beacon
import java.io.IOException
import java.io.OutputStream
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// selAddress is the Bluetooth L2 address of the smartwatch, to be selected among a list of
// associated devices or assuming that the device is always the same
class ClientThread(private var context: Context,
    private val selAddress : String, private val config: List<Beacon>) : Thread(){

    private val tag : String = "serializer"
    /* This UUID is randomly generated at https://www.guidgenerator.com/ */
    private val uuid: UUID = UUID.fromString("bffdf9d2-048d-45cb-b621-3025760dc306")

    private lateinit var adapter: BluetoothAdapter
    private lateinit var device: BluetoothDevice
    private lateinit var socket: BluetoothSocket
    private lateinit var outputStream: OutputStream


    // This function makes the serialization of the Beacon object in JSON format
    private fun sendBeacon(beacon: List<Beacon>){
        try{
            val jsonString = Json.encodeToString(beacon)
            // Converting the jsonString into bytes to flow them in the OutputStream
            val bytes = jsonString.toByteArray()
            outputStream.write(bytes)
            Log.i(tag, "Configuration sent")
        }catch (e: IOException){
            e.printStackTrace()
        }
    }

    // This method is called once the connection is established
    private fun manageConnectedSocket(){
        try{
            outputStream = socket.outputStream
            sendBeacon(config)
            (context as Activity).runOnUiThread {
                Toast.makeText(
                    context,
                    "Configuration correctly synced",
                    Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception){
            (context as Activity).runOnUiThread {
                Toast.makeText(
                    context,
                    "Something went wrong, please try again",
                    Toast.LENGTH_LONG
                ).show()
            }
                e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    override fun run(){
        // Obtaining the bluetooth adapter
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        if(manager != null){
            adapter = manager.adapter
        }
        // Creating the Bluetooth Socket
        device = adapter.getRemoteDevice(selAddress)

        // Connection using the Bluetooth socket
        try{
            socket = device.createRfcommSocketToServiceRecord(uuid)
            adapter.cancelDiscovery()
            Log.i(tag, "Socket created")
            socket.connect()
            manageConnectedSocket()
        }catch(e: IOException){
            (context as Activity).runOnUiThread {
                Toast.makeText(
                    context,
                    "Something went wrong, please try again",
                    Toast.LENGTH_LONG
                ).show()
            }
            Log.e(tag, "Error during connection", e)
        }
    }
}
