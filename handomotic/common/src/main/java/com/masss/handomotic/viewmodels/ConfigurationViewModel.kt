package com.masss.handomotic.viewmodels

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.models.PairedDevice
import com.masss.handomotic.models.Configuration
import com.masss.handomotic.filesocket.FileManager

class ConfigurationViewModel : ViewModel() {
    private val _beacons = MutableLiveData<MutableList<Beacon>>(mutableListOf())
    private val _pairedDevice = MutableLiveData<PairedDevice?>()
    fun initialize(context: Context) {
        val configuration = FileManager.readConfiguration(context)
        _beacons.value = configuration?.beacons?.toMutableList() ?: mutableListOf()
        _pairedDevice.value = configuration?.pairedDevice
    }

    fun addBeacon(beacon: Beacon, context: Context) {
        _beacons.value?.add(beacon)
        saveConfiguration(context)
    }

    fun removeBeacon(beacon: Beacon, context: Context) {
        _beacons.value?.remove(beacon)
        saveConfiguration(context)
    }

    fun getBeacons(): List<Beacon> {
        return _beacons.value ?: emptyList()
    }

    fun flushBeacons(){
        _beacons.value?.clear()
    }

    fun addPairedDevice(pairedDevice: PairedDevice, context: Context) {
        _pairedDevice.value = pairedDevice
        saveConfiguration(context)
    }

    fun getPairedDevice(): PairedDevice? {
        return _pairedDevice.value
    }

    private fun saveConfiguration(context: Context) {
        val configuration = Configuration(
            pairedDevice = _pairedDevice.value ?: PairedDevice("", ""),
            beacons = _beacons.value ?: emptyList()
        )
        FileManager.writeConfiguration(context, configuration)
    }
}
