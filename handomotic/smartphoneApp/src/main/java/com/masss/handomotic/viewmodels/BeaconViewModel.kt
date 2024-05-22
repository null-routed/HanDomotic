package com.masss.handomotic.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.masss.handomotic.models.Beacon
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException

class BeaconViewModel : ViewModel() {
    private val _beacons = MutableLiveData<MutableList<Beacon>>(mutableListOf())
    val beacons: LiveData<MutableList<Beacon>> get() = _beacons

    fun initialize(context: Context) {
        readConfiguration(context)
    }

    private fun readConfiguration(context: Context) {
        val json = Json { prettyPrint = true }
        try {
            context.openFileInput("room_config.json").use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { reader -> reader.readText() }
                val loadedBeacons = json.decodeFromString(ListSerializer(Beacon.serializer()), jsonString).toMutableList()
                _beacons.value = loadedBeacons
            }
        } catch (e: FileNotFoundException) {
            // The file is not found, for example if the application is run for the first time
            println("Configuration file not found")
        } catch (e: Exception) {
            println("Error reading configuration file: ${e.message}")
        }
    }

    private fun writeConfiguration(context: Context) {
        val json = Json { prettyPrint = true }
        val jsonString = json.encodeToString(ListSerializer(Beacon.serializer()), _beacons.value!!)
        context.openFileOutput("room_config.json", Context.MODE_PRIVATE).use {
            it.write(jsonString.toByteArray())
        }
    }

    fun addBeacon(beacon: Beacon, context: Context) {
        _beacons.value?.add(beacon)
        _beacons.value = _beacons.value
        writeConfiguration(context)
    }

    fun removeBeacon(beacon: Beacon, context: Context) {
        _beacons.value?.remove(beacon)
        _beacons.value = _beacons.value
        writeConfiguration(context)
    }

    fun getBeacons(): List<Beacon> {
        return _beacons.value ?: emptyList()
    }
}
