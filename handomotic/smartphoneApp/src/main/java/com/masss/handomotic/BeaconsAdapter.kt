package com.masss.handomotic

import android.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.masss.handomotic.filesocket.FileManager

class BeaconsAdapter(private var beaconsList: List<Beacon>,
    private var alreadyAddBeacon: HashMap<String, Beacon>) : RecyclerView.Adapter<BeaconsAdapter.BeaconsHolder>() {

    class BeaconsHolder(private val row: View) : RecyclerView.ViewHolder(row) {
        val beaconAddress: TextView = row.findViewById(R.id.beacon_address)
        val beaconUuid: TextView = row.findViewById(R.id.beacon_uuid)
        val beaconRssi: TextView = row.findViewById(R.id.beacon_rssi)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconsHolder {
        val layout = LayoutInflater.from(parent.context).inflate(R.layout.beacon_item, parent, false)
        return BeaconsHolder(layout)
    }

    override fun onBindViewHolder(holder: BeaconsHolder, position: Int) {
        val beacon = beaconsList[position] // getting a reference!
        holder.beaconAddress.text = beacon.address
        holder.beaconUuid.text = beacon.id
        holder.beaconRssi.text = beacon.rssi.toString()

        // Handler that is executed when the line is pressed
        holder.itemView.setOnClickListener() {
            Log.i("ROW", "Row $position pressed...")

            // Inflating the popup_room layout
            val li = LayoutInflater.from(holder.itemView.context)
            val popupView = li.inflate(R.layout.popup_room, null)

            // Here you have to show a popup in which the user choose the name of the room
            // The popup is from now on referred as AlertDialog
            val builder = AlertDialog.Builder(holder.itemView.context)
            builder.setView(popupView)

            // Getting the element in which you write the name of the room
            val roomNameField = popupView.findViewById<EditText>(R.id.roomName)

            // Setting up the button
            builder.setPositiveButton("ADD") { dialog, _ ->
                val roomName = roomNameField.text.toString()
                beaconsList[position].name = roomName // roomName is now saved in memory
                // Handle the room name input
                Log.i("POPUP", "Room name: ${beaconsList[position].name}, Address: ${beacon.address}")
                // the room name should be saved into a file..
                // .. in which there's an association between the uuid, the mac and the room name

                alreadyAddBeacon.put(beacon.address, beacon)
                dialog.dismiss()
                Log.i("LIST_STS", beaconsList.joinToString(separator = ","))
                FileManager.writeConfiguration(holder.itemView.context, alreadyAddBeacon)
                Log.i("POPUP", "Written into file...")
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            // Creating and showing the alert dialog
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }
    override fun getItemCount(): Int = beaconsList.size
}