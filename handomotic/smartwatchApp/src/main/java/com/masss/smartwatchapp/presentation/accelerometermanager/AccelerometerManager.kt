package com.masss.smartwatchapp.presentation.accelerometermanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.masss.smartwatchapp.presentation.classifier.FeatureExtractor
import java.util.LinkedList


class AccelerometerManager(private val context: Context) {

    // Defining the length of the windowing buffer array
    private val bufferSize: Int = 50
    private var counter: Int = 0

    // Defining how many samples to drop from a call to the classifier to another
    private val classificationFrequency: Int = 3

    /* Windowing buffering arrays */
    private val xWindow: LinkedList<Float> = LinkedList()
    private val yWindow: LinkedList<Float> = LinkedList()
    private val zWindow: LinkedList<Float> = LinkedList()

    private fun addSample(xValue: Float, yValue: Float, zValue: Float) {
        if (xWindow.size >= bufferSize) {
            xWindow.removeFirst() // removing at the head
            yWindow.removeFirst() // we can remove also y and z as they are of the same size of x
            zWindow.removeFirst()
        }
        // The tail is added in any case
        xWindow.addLast(xValue)
        yWindow.addLast(yValue)
        zWindow.addLast(zValue)
    }

    // Broadcasts features to the classifier
    private fun broadcastFeatures(featuresList: List<Float>) {
        val intent = Intent("FeatureList")
        for (i in featuresList.indices) {
            intent.putExtra("feature_$i", featuresList[i])
        }
        context.sendBroadcast(intent)
    }

    val accelerometerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val xValue = intent?.getFloatExtra("xValue", 0f)
            val yValue = intent?.getFloatExtra("yValue", 0f)
            val zValue = intent?.getFloatExtra("zValue", 0f)
            val timestamp = intent?.getLongExtra("timestamp", 0)

            if (xValue != null && yValue != null && zValue != null && timestamp != null) {
                addSample(xValue, yValue, zValue) // Makes the window slide: writes in xWindow, yWindow, zWindow
            }

            if (counter < classificationFrequency) {
                counter++
            } else { // every 'classificationFrequency' samples features get extracted and sent to the classifier
                val xFeatures = FeatureExtractor.extractFeatures(xWindow.toFloatArray())
                val yFeatures = FeatureExtractor.extractFeatures(yWindow.toFloatArray())
                val zFeatures = FeatureExtractor.extractFeatures(zWindow.toFloatArray())
                val allFeatures =
                    (xFeatures + yFeatures + zFeatures) +
                            FeatureExtractor.calculateCorrelations(
                                xWindow.toFloatArray(),
                                yWindow.toFloatArray(),
                                zWindow.toFloatArray()
                            )
                counter = 0
                broadcastFeatures(allFeatures)
            }
        }
    }
}
