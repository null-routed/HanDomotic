package com.masss.handomotic.fragments

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.masss.handomotic.R
import com.masss.handomotic.ScanActivity
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.viewmodels.BeaconViewModel

class BeaconFragment : Fragment() {

    private lateinit var beaconsRecyclerView: RecyclerView
    private lateinit var noBeaconsTextView: TextView
    private lateinit var addNewDevices: Button
    private lateinit var registeredBeaconAdapter: RegisteredBeaconAdapter
    private val beaconViewModel: BeaconViewModel by activityViewModels()

    private lateinit var scanActivityResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_beacon, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize beaconManager here
        beaconsRecyclerView = view.findViewById(R.id.beaconsRecyclerView)
        noBeaconsTextView = view.findViewById(R.id.noBeaconsTextView)
        addNewDevices = view.findViewById(R.id.add_new_devices)

        beaconsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Initialize the adapter and pass the callback to remove a beacon
        registeredBeaconAdapter = RegisteredBeaconAdapter(emptyList()) { beacon ->
            beaconViewModel.removeBeacon(beacon, requireContext())
            updateVisibility(beaconViewModel.getBeacons())
        }
        beaconsRecyclerView.adapter = registeredBeaconAdapter

        updateVisibility(beaconViewModel.getBeacons())

        scanActivityResultLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleActivityResult(result.resultCode, result.data)
        }

        addNewDevices.setOnClickListener {
            val intent = Intent(requireContext(), ScanActivity::class.java)
            intent.putParcelableArrayListExtra("beacons", ArrayList(beaconViewModel.getBeacons()))
            scanActivityResultLauncher.launch(intent)
        }
    }

    private fun updateVisibility(beacons: List<Beacon>) {
        if (beacons.isEmpty()) {
            noBeaconsTextView.visibility = View.VISIBLE
            beaconsRecyclerView.visibility = View.GONE
        } else {
            noBeaconsTextView.visibility = View.GONE
            beaconsRecyclerView.visibility = View.VISIBLE
            registeredBeaconAdapter.updateBeacons(beacons)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun handleActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val newBeacon = data?.getParcelableArrayListExtra("new_beacon", Beacon::class.java)
            if (newBeacon != null) {
                beaconViewModel.addBeacon(newBeacon.first(), requireContext())
                updateVisibility(beaconViewModel.getBeacons())
            }
        }
    }
}
