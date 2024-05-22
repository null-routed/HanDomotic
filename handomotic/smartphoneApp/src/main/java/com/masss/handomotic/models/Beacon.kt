package com.masss.handomotic.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Beacon(
    val id: String,
    val address: String,
    var name: String? = null,
    var rssi: Double? = null
) : Parcelable