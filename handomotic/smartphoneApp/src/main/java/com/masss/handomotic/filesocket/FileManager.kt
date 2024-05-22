package com.masss.handomotic.filesocket

import android.content.Context
import com.masss.handomotic.models.Beacon
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException

// This is an utility class that writes in an append only file the association between
// beacon's address and room name
class FileManager() {

    companion object{
        fun readConfiguration(context: Context): MutableList<Beacon> {
            val json = Json { prettyPrint = true }
            try {
                context.openFileInput("room_config.json").use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { reader -> reader.readText() }
                    val loadedBeacons = json.decodeFromString(ListSerializer(Beacon.serializer()), jsonString).toMutableList()
                    return loadedBeacons
                }
            } catch (e: FileNotFoundException) {
                println("Configuration file not found")
            } catch (e: Exception) {
                println("Error reading configuration file: ${e.message}")
            }
            return mutableListOf()
        }

        fun writeConfiguration(context: Context, beacons: List<Beacon>) {
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(ListSerializer(Beacon.serializer()), beacons)
            context.openFileOutput("room_config.json", Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
        }
    }
}