package it.unipi.masss.classifiertest.presentation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import it.unipi.masss.classifiertest.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.apache.commons.math3.complex.Complex
import java.nio.FloatBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.pow
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import kotlin.math.sqrt


@Serializable
data class GestureData(
    val timestamps: List<String>,
    val xTimeSeries: List<Float>,
    val yTimeSeries: List<Float>,
    val zTimeSeries: List<Float>,
    val label: String
)

class MainActivity : ComponentActivity() {
    fun leggiFileToArrayFloat(context: Context, resourceId: Int): Array<Array<Float>> {
        val result = mutableListOf<Array<Float>>()

        // Leggi il contenuto del file e convertilo in un array di array di float
        context.resources.openRawResource(resourceId).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val quadreInterne = line!!.split("], [")
                    quadreInterne.forEach { quadraInterna ->
                        val values = quadraInterna.replace("[", "").replace("]", "").split(", ").map { it.toFloat() }
                        result.add(values.toTypedArray())
                    }
                }
            }
        }

        return result.toTypedArray()
    }

    fun loadData(context: Context, resourceId: Int): List<GestureData> {
        val inputStream = context.resources.openRawResource(resourceId)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return Json.decodeFromString(jsonString)
    }

    fun FloatArray.skewness(): Double {
        val mean = this.average()
        val n = this.size.toDouble()
        val s3 = this.sumOf{ ((it - mean) / this.standardDeviation()).pow(3) }
        return (n / ((n - 1) * (n - 2))) * s3
    }

    fun extractFeatures(data: FloatArray): List<Double> {
        val features = mutableListOf<Double>()

        // Time domain features
        features.apply {
            add(data.average())
            add(data.standardDeviation())
            add(data.minOrNull()?.toDouble() ?: 0.0)
            add(data.maxOrNull()?.toDouble() ?: 0.0)
            add(data.sumOf { abs(it.toDouble()) } / data.size)  // SMA using sumOf
            add(data.sumOf { it.toDouble() * it.toDouble() })  // Energy using sumOf
            add(data.zeroCrossings().toDouble())  // Zero crossings
            add(Kurtosis().evaluate(data.map { it.toDouble() }.toDoubleArray()))  // Kurtosis
            add(data.skewness())  // Skewness
        }

        // Frequency domain features
        val (spectralEnergy, dominantFrequency) = extractFrequencyFeatures(data, 50.0)
        features.apply {
            add(spectralEnergy)
            add(dominantFrequency)
        }

        return features
    }


    fun extractFrequencyFeatures(data: FloatArray, sampleRate: Double): Pair<Double, Double> {
        val n = Integer.highestOneBit(data.size - 1) shl 1
        val paddedData = data.copyOf(n)

        val transform = FastFourierTransformer(DftNormalization.STANDARD)
        val fftVals = transform.transform(paddedData.map { Complex(it.toDouble(), 0.0) }.toTypedArray(), TransformType.FORWARD)

        val fftMagnitudes = fftVals.map { it.abs() }
        val spectralEnergy = fftMagnitudes.sumOf { it * it } / n
        val frequencies = fftVals.indices.map { index -> (index * sampleRate) / n }
        val dominantFrequencyIndex = fftMagnitudes.indices.maxByOrNull { fftMagnitudes[it] } ?: 0
        val dominantFrequency = frequencies[dominantFrequencyIndex]

        return Pair(spectralEnergy, dominantFrequency)
    }

    fun FloatArray.standardDeviation(): Double {
        val mean = this.average()
        return sqrt(this.fold(0.0, { acc, d -> acc + (d - mean).pow(2.0) }) / this.size)
    }

    fun FloatArray.zeroCrossings(): Int {
        var count = 0
        for (i in 1 until this.size) {
            if (this[i - 1] * this[i] < 0) {
                count++
            }
        }
        return count
    }

    // This function computes the highest class probability given a confidence Array<FloatArray>
    fun computeMaxFloatArray(matrix: Array<FloatArray>) : Float{
        var max : Float = 0.0f
        for(row in matrix){
            for (value in row){
                if(value >= max){
                    max = value
                }
            }
        }
        return max
    }

    // Create an OrtSession with the given OrtEnvironment
    private fun createORTSession( ortEnvironment: OrtEnvironment) : OrtSession {
        val modelBytes = resources.openRawResource( R.raw.gesture_classifier).readBytes()
        return ortEnvironment.createSession( modelBytes )
    }

    // This method performs a single prediction
    fun retrievePredictionAndConfidence(inputFeatures: FloatArray, numFeatures: Int, threshold: Float = 1.0f) : Pair<String, Float> {

        // Creating an ortEnvironment
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
        if(confidence < threshold){
            prediction = "No Gesture"
        } else {
            prediction = predictionVector[0]
        }

        return prediction to confidence
    }

    fun main() {
        // Load data
        val gestureData = loadData(this,R.raw.labeled_data_circle)
        var allFeatures = mutableListOf<Double>() // List empty at the beginning
        // Process each gesture to extract features
        gestureData.forEach { gesture ->
            val xFeatures = extractFeatures(gesture.xTimeSeries.toFloatArray())
            val yFeatures = extractFeatures(gesture.yTimeSeries.toFloatArray())
            val zFeatures = extractFeatures(gesture.zTimeSeries.toFloatArray())
            allFeatures = (xFeatures + yFeatures + zFeatures).toMutableList()
            println("Features for label ${gesture.label}:")
            println("X-axis: $xFeatures")
            println("Y-axis: $yFeatures")
            println("Z-axis: $zFeatures")
        }

        for(feature in allFeatures){
            println(feature)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // System.getProperty("os.arch", "generic")
        super.onCreate(savedInstanceState)
        main()
        // setContentView(R.layout.pippo)
        // setTheme(android.R.style.Theme_DeviceDefault)
        // val button : Button = findViewById(R.id.button2)
        // val outputTextView : TextView = findViewById(R.id.textView2)

    }
}