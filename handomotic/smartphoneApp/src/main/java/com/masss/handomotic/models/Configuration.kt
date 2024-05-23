package com.masss.handomotic.models

import kotlinx.serialization.Serializable

@Serializable
data class Configuration (
    val pairedDevice: PairedDevice,
    val beacons: List<Beacon>
)
