package com.masss.smartwatchapp.presentation.utilities

class AccelerometerTimeSeries {

    private var _xTimeSeries = mutableListOf<Float>()
    private var _yTimeSeries = mutableListOf<Float>()
    private var _zTimeSeries = mutableListOf<Float>()
    private var _recordingTimestamps = mutableListOf<Long>()

    val xTimeSeries: MutableList<Float>
        get() = _xTimeSeries

    val yTimeSeries: MutableList<Float>
        get() = _yTimeSeries

    val zTimeSeries: MutableList<Float>
        get() = _zTimeSeries

    val recordingTimestamps: MutableList<Long>
        get() = _recordingTimestamps

//    var newMeasurement: T
//        get() = throw UnsupportedOperationException("Direct access to the new item is not supported.")
//        set
}