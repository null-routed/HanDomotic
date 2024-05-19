package com.masss.handomotic

data class Beacon(
    val id: String,
    val macAddress: String,
    val name: String? = null,
    var rssi: Double
)