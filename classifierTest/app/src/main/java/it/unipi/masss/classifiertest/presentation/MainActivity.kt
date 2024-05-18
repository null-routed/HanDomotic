package it.unipi.masss.classifiertest.presentation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.tambapps.fft4j.FastFouriers
import it.unipi.masss.classifiertest.R
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import org.jtransforms.fft.FloatFFT_1D
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer

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

    @Serializable
    data class FeatureData(
        val features: List<Float>,
        val label: String
    )
    fun saveFeaturesToJson(context: Context, data: List<FeatureData>) {
        val json = Json { prettyPrint = true }
        // Explicitly use ListSerializer for FeatureData
        val jsonString = json.encodeToString(ListSerializer(FeatureData.serializer()), data)

        context.openFileOutput("features.json", Context.MODE_PRIVATE).use {
            println("Saving on file")
            it.write(jsonString.toByteArray())
        }
    }


    fun calculateCorrelations(xFeatures: FloatArray, yFeatures: FloatArray, zFeatures: FloatArray): List<Float> {
        val features = mutableListOf<Float>()
        val mAxisData = arrayOf(xFeatures, yFeatures, zFeatures)

        // Calculate pairwise correlations
        for (i in 0 until 3) {
            for (j in i + 1 until 3) {
                val correlation = PearsonsCorrelation().correlation(mAxisData[i].toDoubleArray(), mAxisData[j].toDoubleArray())
                features.add(correlation.toFloat())
            }
        }

        return features
    }

    fun loadData(context: Context, resourceId: Int): List<GestureData> {
        val inputStream = context.resources.openRawResource(resourceId)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        return Json.decodeFromString(jsonString)
    }

    fun ptp(array: FloatArray): Float {
        val max = array.maxOrNull()!!
        val min = array.minOrNull()!!
        return max - min
    }

    fun calculateAvgAbsIncrement(axisData: FloatArray): Float {
        // Calculate the absolute increments
        val diffs = FloatArray(axisData.size - 1) { i -> abs(axisData[i + 1] - axisData[i]) }
        // Calculate the mean of the absolute increments
        return diffs.average().toFloat()
    }


    fun calculateMeanCrossings(axisData: FloatArray): Float {
        // Calculate the mean of the axis data
        val mean = axisData.average()
        // Calculate the mean-crossings
        var crossings = 0
        for (i in 1 until axisData.size) {
            val prev = axisData[i - 1] - mean
            val current = axisData[i] - mean
            if (prev * current < 0) {
                crossings++
            }
        }
        return crossings.toFloat()
    }

    fun extractFeatures(data: FloatArray): List<Float> {
        val features = mutableListOf<Float>()
        // Time domain features
        features.apply {
            add(data.average().toFloat())
            add(data.standardDeviation().toFloat())
            add(ptp(data))
            add(data.minOrNull() ?: 0.0f)
            add(data.maxOrNull() ?: 0.0f)
            add((data.sumOf { abs(it.toDouble()) } / data.size).toFloat())  // SMA using sumOf
            add(calculateAvgAbsIncrement(data))
            add(calculateMeanCrossings(data))
        }

        // Frequency domain features
        // val (spectralEnergy, dominantFrequency) = extractFrequencyFeatures(data, 50.0)
        val frequencyFeatures = extractFrequencyFeatures(data)
        features.addAll(frequencyFeatures.toList())
        return features
    }

    fun extractFrequencyFeatures(data: FloatArray): FloatArray {
        val n = data.size
        val fft = FloatFFT_1D(n.toLong())
        val fftData = FloatArray(n)  // This will hold the FFT result
        System.arraycopy(data, 0, fftData, 0, n)

        // Perform the FFT in place
        fft.realForward(fftData)

        // Calculate spectral energy
        var spectralEnergy = 0.0
        var totalMagnitude = 0.0
        val freqs = FloatArray(n / 2) { it * 1.0f / n }

        for (i in 0 until n / 2) {
            val re = fftData[2 * i]  // Real part
            val im = if (i == 0 || 2 * i == n - 1) 0.0f else fftData[2 * i + 1] // Imaginary part
            val mag = re * re + im * im
            spectralEnergy += mag
            totalMagnitude += Math.sqrt(mag.toDouble())
        }
        spectralEnergy /= n // Normalize by the number of points

        // Calculate spectral centroid
        var spectralCentroid = 0.0
        for (i in 0 until n / 2) {
            val re = fftData[2 * i]
            val im = if (i == 0 || 2 * i == n - 1) 0.0f else fftData[2 * i + 1]
            val mag = Math.sqrt((re * re + im * im).toDouble())
            spectralCentroid += freqs[i] * mag
        }
        spectralCentroid /= totalMagnitude // Normalized magnitude for centroid calculation
        return floatArrayOf((spectralEnergy * 2.0).toFloat()  , spectralCentroid.toFloat())
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
            prediction = "Other"
        } else {
            prediction = predictionVector[0]
        }

        return prediction to confidence
    }

    fun main() {
        // Load data
        val gestureData = loadData(this,R.raw.labeled_data_circle)+ loadData(this, R.raw.labeled_data_clap) + loadData(this, R.raw.labeled_data_random)
        val allFeatureData = mutableListOf<FeatureData>()
        // Process each gesture to extract features
        gestureData.forEach { gesture ->
            val xFeatures = extractFeatures(gesture.xTimeSeries.toFloatArray())
            val yFeatures = extractFeatures(gesture.yTimeSeries.toFloatArray())
            val zFeatures = extractFeatures(gesture.zTimeSeries.toFloatArray())
            val allFeatures  =
                    (xFeatures + yFeatures + zFeatures) +
                    calculateCorrelations(
                        gesture.xTimeSeries.toFloatArray(),
                        gesture.yTimeSeries.toFloatArray(),
                        gesture.zTimeSeries.toFloatArray()
                    )
            allFeatureData.add(FeatureData(allFeatures, gesture.label))
            //println("allFeatures: $allFeatures")
            // println("True class: ${gesture.label}:")
            val (prediction, confidence) = retrievePredictionAndConfidence(allFeatures.toFloatArray(), allFeatures.size)
            println("Predicted: $prediction, Actual: ${gesture.label}, with confidence: $confidence")
            if(prediction != gesture.label){
                if(gesture.label == "Other"){
                    Log.i("PRED_OK", "Predicted: $prediction, Actual: ${gesture.label}, with confidence: $confidence")
                } else {
                    Log.i("PRED_ERR", "Predicted: $prediction, Actual: ${gesture.label}, with confidence: $confidence")
                }
            }
        }
        saveFeaturesToJson(this, allFeatureData)
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

    // Utility method for converting a FloatArray into a DoubleArray

    fun FloatArray.toDoubleArray(): DoubleArray {
        val result = DoubleArray(this.size)
        for (i in this.indices) {
            result[i] = this[i].toDouble()
        }
        return result
    }
}