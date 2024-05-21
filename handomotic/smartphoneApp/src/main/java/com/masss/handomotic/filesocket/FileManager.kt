package com.masss.handomotic.filesocket

import android.content.Context
import com.masss.handomotic.Beacon
import com.masss.handomotic.R
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException

// This is an utility class that writes in an append only file the association between
// beacon's address and room name
class FileManager(val path: String) {

    companion object{
        // The default path for the file storage on the device is
        // data/data/package_name/files
        fun writeConfiguration(context: Context, beaconMap: HashMap<String, Beacon>){
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(ListSerializer(Beacon.serializer()), beaconMap.values.toList())
            context.openFileOutput("room_config.json", Context.MODE_PRIVATE).use {
                println("Saving on file")
                it.write(jsonString.toByteArray())
            }
        }

        fun readConfiguration(context: Context): List<Beacon>? {
            val json = Json { prettyPrint = true }
            return try {
                context.openFileInput("room_config.json").use {
                    val jsonString = it.bufferedReader().use { reader -> reader.readText() }
                    json.decodeFromString(ListSerializer(Beacon.serializer()), jsonString)
                }
            } catch (e: FileNotFoundException) {
                // The file is not found for example if the application is runned for the first time
                println("Configuration file not found")
                null
            } catch (e: Exception) {
                println("Error reading configuration file: ${e.message}")
                null
            }
        }
    }
}