package com.masss.smartwatchapp.presentation.btsocket
import kotlinx.serialization.Serializable

@Serializable
data class RoomSetting(
    val id: String,             // Kontakt identifier
    val address: String,        // L2 address
    val name: String? = null,   // Room name
    val gesture: String,        // Associated gesture
    val action: String          // Associated action
)
