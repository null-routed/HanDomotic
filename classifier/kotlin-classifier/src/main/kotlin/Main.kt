import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.*
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.transform.*

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import androidx.activity.ComponentActivity
import java.nio.FloatBuffer


@Serializable
data class GestureData(
    val timestamps: List<String>,
    val xTimeSeries: List<Float>,
    val yTimeSeries: List<Float>,
    val zTimeSeries: List<Float>,
    val label: String
)

fun loadData(filepath: String): List<GestureData> {
    val jsonString = File(filepath).readText()
    return Json.decodeFromString(jsonString)
}

fun FloatArray.skewness(): Double {
    val mean = this.average()
    val n = this.size.toDouble()
    val s3 = this.sumOf { ((it - mean) / this.standardDeviation()).pow(3) }
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
}

// This method performs a single prediction
fun retrievePredictionAndConfidence(inputFeatures: FloatArray, numFeatures: Int, threshold: Float = 1.0f) : Pair<String, Float> {
    
    // Creating an ortEnvironment
    ortEnvironment = OrtEnvironment.getEnvironment()
    val ortSession = createORTSession(ortEnvironment)

    val inputName = ortSession.inputNames?.iterator()?.next()
    val floatInputs = FloatBuffer.wrap(inputFeatures)

    // Creating an input tensor with numFeatures features as shape
    val inputTensor = OnnxTensor.createTensor(ortEnvironment, floatInputs, longArrayOf(1, numFeatures))

    // Running the model
    val results = ortSession.run(mapOf(inputName to inputTensor))
    val confidenceMatrix = results[1].values as Array<FloatArray>
    val confidence = computeMaxFloatArray(confidenceMatrix)
    val predictionVector = results[0].values as Array<String>

    val lateinit prediction : String
    if(confidence < threshold){
        prediction = "No Gesture"
    } else {
        prediction = predictionVector[0]
    }

    return prediction, confidence
}

fun main() {
    // Load data
    val gestureData = loadData("labeled_train_data/labeled_data_circle.json")

    // Process each gesture to extract features
    gestureData.forEach { gesture ->
        val xFeatures = extractFeatures(gesture.xTimeSeries.toFloatArray())
        val yFeatures = extractFeatures(gesture.yTimeSeries.toFloatArray())
        val zFeatures = extractFeatures(gesture.zTimeSeries.toFloatArray())

        println("Features for label ${gesture.label}:")
        println("X-axis: $xFeatures")
        println("Y-axis: $yFeatures")
        println("Z-axis: $zFeatures")
    }

    // Merging the arrays of the features for each axis in a single array
    val allFeatures = xFeatures + yFeatures + zFeatures
    println("All-features: $allFeatures")
}
