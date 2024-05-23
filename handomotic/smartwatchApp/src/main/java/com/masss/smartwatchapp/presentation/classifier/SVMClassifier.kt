package com.masss.smartwatchapp.presentation.classifier

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.util.Predicate
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.masss.smartwatchapp.R
import java.nio.FloatBuffer


class SVMClassifier(private val context: Context) {

    private var isReceiverRegister = false

    // TEST
    private var knownGestures: List<String> = mutableListOf("Circle", "Clap")

    // This function computes the highest class probability given a confidence Array<FloatArray>
    private fun computeMaxFloatArray(matrix: Array<FloatArray>) : Float{
        var max : Float = 0.0f
        for(row in matrix){
            for (value in row){
                if(value >= max)
                    max = value
            }
        }
        return max
    }

    // Create an OrtSession with the given OrtEnvironment
    private fun createORTSession( ortEnvironment: OrtEnvironment) : OrtSession {
        val modelBytes = context.resources.openRawResource( R.raw.gesture_classifier).readBytes()
        return ortEnvironment.createSession( modelBytes )
    }

    // This function performs the SVM classification
    private fun retrievePredictionAndConfidence(inputFeatures: FloatArray, numFeatures: Int, threshold: Float = 0.899f): Pair<String, Float>{
        val ortEnvironment = OrtEnvironment.getEnvironment()
        val ortSession = createORTSession(ortEnvironment)

        val inputName = ortSession.inputNames?.iterator()?.next()
        val floatInputs = FloatBuffer.wrap(inputFeatures)

        // Creating an input tensor with numFeatures features as shape
        val inputTensor = OnnxTensor.createTensor(ortEnvironment, floatInputs, longArrayOf(1, numFeatures.toLong()))

        // Running the model
        val results = ortSession.run(mapOf(inputName to inputTensor))
        val confidenceMatrix = results[1].value as Array<FloatArray>
        val confidence = computeMaxFloatArray(confidenceMatrix)
        val predictionVector = results[0].value as Array<String>

        val prediction : String
        if(confidence < threshold)
            prediction = "No Gesture"
        else
            prediction = predictionVector[0]

        // send broadcast to mainActivity to notify a known hand gesture has been performed
        if (prediction in knownGestures)
            sendKnownGestureBroadcast(prediction)

        return prediction to confidence
    }

    private fun sendKnownGestureBroadcast(prediction: String) {
        val intent = Intent("com.masss.smartwatchapp.GESTURE_RECOGNIZED")
        intent.putExtra("prediction", prediction)
        context.sendBroadcast(intent)
    }


    // Defining the BroadcastReceiver
    private val featureReceiver : BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let{
                val featureList = mutableListOf<Float>()
                var i = 0
                while(it.hasExtra("feature_$i")){
                    featureList.add(it.getFloatExtra("feature_$i", 0.0f))
                    i++
                }
                val (prediction, confidence) = retrievePredictionAndConfidence(featureList.toFloatArray(), featureList.size)

                // Printing on Log the results of classification SVM
                if (prediction == "Circle" || prediction == "Clap")
                    Log.i("CLASSIFIER_LOG", "Predicted: $prediction, with confidence: $confidence")
            }
        }
    }

    fun registerReceiver() {
        if (!isReceiverRegister) {
            val filter = IntentFilter("FeatureList")
            context.registerReceiver(featureReceiver, filter)
            isReceiverRegister = true
        }
    }

    fun unregisterReceiver() {
        if (isReceiverRegister) {
            context.unregisterReceiver(featureReceiver)
            isReceiverRegister = false
        }
    }
}