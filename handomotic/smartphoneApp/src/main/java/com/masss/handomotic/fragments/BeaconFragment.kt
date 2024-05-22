package com.masss.handomotic.fragments

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.masss.handomotic.R
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.viewmodels.BeaconViewModel

class BeaconFragment : Fragment() {

    private val beaconViewModel: BeaconViewModel by activityViewModels()

    private lateinit var beaconsRecyclerView: RecyclerView
    private lateinit var noBeaconsTextView: TextView
    private lateinit var registeredBeaconAdapter: RegisteredBeaconAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_beacon, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        beaconsRecyclerView = view.findViewById(R.id.beaconsRecyclerView)
        noBeaconsTextView = view.findViewById(R.id.noBeaconsTextView)

        beaconsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        registeredBeaconAdapter = RegisteredBeaconAdapter(emptyList()) { beacon ->
            beaconViewModel.removeBeacon(beacon, requireContext())
        }
        beaconsRecyclerView.adapter = registeredBeaconAdapter

        beaconViewModel.beacons.observe(viewLifecycleOwner) { beacons: List<Beacon> ->
            if (beacons.isEmpty()) {
                noBeaconsTextView.visibility = View.VISIBLE
                beaconsRecyclerView.visibility = View.GONE
            } else {
                noBeaconsTextView.visibility = View.GONE
                beaconsRecyclerView.visibility = View.VISIBLE
                registeredBeaconAdapter.updateBeacons(beacons)
            }
        }

        beaconViewModel.initialize(requireContext())
    }
}
