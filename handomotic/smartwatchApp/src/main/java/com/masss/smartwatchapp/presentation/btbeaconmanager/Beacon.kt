package com.masss.smartwatchapp.presentation.btbeaconmanager

import kotlinx.serialization.Serializable


@Serializable
data class Beacon(
    val id: String,
    val address: String,
    var name: String? = null,           // room the beacon has been placed in
    var rssi: Double
)