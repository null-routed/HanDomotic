package com.masss.handomotic

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.masss.handomotic.models.Beacon

class BeaconsAdapter(private var beaconManager: BTBeaconManager, private val activity: Activity) : RecyclerView.Adapter<BeaconsAdapter.BeaconsHolder>() {

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

        val beacons = beaconManager.getUnknownBeacons().values.sortedBy { it.rssi?.times(-1) }

        Log.i("UKNOWN_BEACONS", "Beacons: ${beaconManager.getUnknownBeacons()}")
        Log.i("KNOWN_BEACONS", "Beacons: ${beaconManager.getKnownBeacons()}")

        val beacon = beacons[position]
        holder.beaconAddress.text = beacon.address
        holder.beaconUuid.text = beacon.id
        holder.beaconRssi.text = beacon.rssi.toString()

        // Handler that is executed when the line is pressed
        holder.itemView.setOnClickListener {
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
                beacon.name = roomName

                val newBeacons : ArrayList<Beacon> = ArrayList()
                newBeacons.add(beacon)

                beaconManager.addKnownBeacon(beacon)
                val resultIntent = Intent()
                resultIntent.putParcelableArrayListExtra("new_beacon", newBeacons)
                activity.setResult(Activity.RESULT_OK, resultIntent)
                activity.finish()

                dialog.dismiss()
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.cancel()
            }

            // Creating and showing the alert dialog
            val alertDialog = builder.create()
            alertDialog.show()
        }
    }
    override fun getItemCount(): Int = beaconManager.getUnknownBeacons().size
}