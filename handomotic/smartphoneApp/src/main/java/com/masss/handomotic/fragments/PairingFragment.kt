package com.masss.handomotic.fragments

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.masss.handomotic.R

class PairingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pairing, container, false)
    }
}
