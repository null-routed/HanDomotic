package com.masss.handomotic.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class PairedDevice(
    val deviceName: String,
    val deviceAddress: String
) : Parcelable
