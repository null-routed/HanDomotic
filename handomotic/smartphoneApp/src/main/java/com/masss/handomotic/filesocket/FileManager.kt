package com.masss.handomotic.filesocket

import android.content.Context
import com.masss.handomotic.models.Beacon
import com.masss.handomotic.models.Configuration
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException

class FileManager {

    companion object {
        fun readConfiguration(context: Context): Configuration? {
            val json = Json { prettyPrint = true }
            return try {
                context.openFileInput("config.json").use { inputStream ->
                    val jsonString = inputStream.bufferedReader().use { reader -> reader.readText() }
                    json.decodeFromString(Configuration.serializer(), jsonString)
                }
            } catch (e: FileNotFoundException) {
                println("Configuration file not found")
                Configuration(pairedDevice = null, beacons = emptyList())
            } catch (e: Exception) {
                println("Error reading configuration file: ${e.message}")
                Configuration(pairedDevice = null, beacons = emptyList())
            }
        }

        fun writeConfiguration(context: Context, configuration: Configuration) {
            val json = Json { prettyPrint = true }
            val jsonString = json.encodeToString(Configuration.serializer(), configuration)
            context.openFileOutput("config.json", Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
        }
    }
}
