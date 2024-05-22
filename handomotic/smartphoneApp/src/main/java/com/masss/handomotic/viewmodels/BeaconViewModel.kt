package com.masss.handomotic.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.filesocket.FileManager
class BeaconViewModel : ViewModel() {
    private val _beacons = MutableLiveData<MutableList<Beacon>>(mutableListOf())
    val beacons: LiveData<MutableList<Beacon>> get() = _beacons

    fun initialize(context: Context) {
        _beacons.value = FileManager.readConfiguration(context)
    }

    fun addBeacon(beacon: Beacon, context: Context) {
        _beacons.value?.add(beacon)
        FileManager.writeConfiguration(context, _beacons.value!!)
    }

    fun removeBeacon(beacon: Beacon, context: Context) {
        _beacons.value?.remove(beacon)
        FileManager.writeConfiguration(context, _beacons.value!!)
    }

    fun getBeacons(): List<Beacon> {
        return _beacons.value ?: emptyList()
    }
}
