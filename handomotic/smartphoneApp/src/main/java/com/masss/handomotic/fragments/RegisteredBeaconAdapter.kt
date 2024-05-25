package com.masss.handomotic.fragments

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.masss.handomotic.R
import com.masss.handomotic.models.Beacon

class RegisteredBeaconAdapter(private var beacons: List<Beacon>, private val deleteCallback: (Beacon) -> Unit) :
    RecyclerView.Adapter<RegisteredBeaconAdapter.BeaconViewHolder>() {

    inner class BeaconViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val macAddressTextView: TextView = view.findViewById(R.id.macAddressTextView)
        val roomNameTextView: TextView = view.findViewById(R.id.roomNameTextView)
        val deleteImageView: Button = view.findViewById(R.id.deleteBeaconButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BeaconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.registered_beacon_row, parent, false)
        return BeaconViewHolder(view)
    }

    override fun onBindViewHolder(holder: BeaconViewHolder, position: Int) {
        val beacon = beacons[position]
        holder.macAddressTextView.text = beacon.address
        holder.roomNameTextView.text = beacon.name
        holder.deleteImageView.setOnClickListener {
            deleteCallback(beacon)
        }
    }

    override fun getItemCount() = beacons.size

    fun updateBeacons(newBeacons: List<Beacon>) {
        this.beacons = newBeacons
        notifyDataSetChanged()
    }
}
