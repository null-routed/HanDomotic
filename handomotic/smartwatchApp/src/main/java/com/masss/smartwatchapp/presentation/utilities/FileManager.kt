package com.masss.smartwatchapp.presentation.utilities

import android.content.Context
import com.masss.smartwatchapp.presentation.btbeaconmanager.Beacon
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException


// This is an utility class that writes in an append only file the association between
// beacon's address and room name
class FileManager(val path: String) {

    companion object{
        // The default path for the file storage on the device is
        // data/data/package_name/files
        fun writeConfiguration(context: Context, beaconMap: MutableMap<String, Beacon>){
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(ListSerializer(Beacon.serializer()), beaconMap.values.toList())
            context.openFileOutput("room_config.json", Context.MODE_PRIVATE).use {
                println("Saving on file")
                it.write(jsonString.toByteArray())
            }
        }

        fun readConfiguration(context: Context): MutableMap<String, Beacon>? {
            val json = Json { prettyPrint = true }
            try {
                context.openFileInput("room_config.json").use { it ->
                    val jsonString = it.bufferedReader().use { reader -> reader.readText() }
                    return json.decodeFromString(ListSerializer(Beacon.serializer()), jsonString).associateBy { it.address }.toMutableMap()
                }
            } catch (e: FileNotFoundException) {
                // The file is not found for example if the application is run for the first time
                println("Configuration file not found")
                return null
            } catch (e: Exception) {
                println("Error reading configuration file: ${e.message}")
                return null
            }
        }
    }
}