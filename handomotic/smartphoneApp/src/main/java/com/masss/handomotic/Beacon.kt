package com.masss.handomotic

import kotlinx.serialization.Serializable

@Serializable
data class Beacon(
    val id: String,
    val address: String,
    var name: String? = null,
    var rssi: Double
)