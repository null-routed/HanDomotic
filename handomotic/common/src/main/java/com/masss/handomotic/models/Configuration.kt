package com.masss.handomotic.models

import kotlinx.serialization.Serializable

@Serializable
data class Configuration (
    var pairedDevice: PairedDevice?,
    val beacons: List<Beacon>
)
