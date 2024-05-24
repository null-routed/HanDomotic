package com.masss.smartwatchapp.presentation.classifier

import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class FeatureExtractor {

    companion object{

        // Utility method for converting a FloatArray into a DoubleArray
        private fun FloatArray.toDoubleArray(): DoubleArray {
            val result = DoubleArray(this.size)
            for (i in this.indices) {
                result[i] = this[i].toDouble()
            }
            return result
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

        private fun ptp(array: FloatArray): Float {
            val max = array.maxOrNull()!!
            val min = array.minOrNull()!!
            return max - min
        }

        private fun calculateAvgAbsIncrement(axisData: FloatArray): Float {
            // Calculate the absolute increments
            val diffs = FloatArray(axisData.size - 1) { i -> abs(axisData[i + 1] - axisData[i]) }
            // Calculate the mean of the absolute increments
            return diffs.average().toFloat()
        }

        private fun calculateMeanCrossings(axisData: FloatArray): Float {
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

        private fun FloatArray.standardDeviation(): Double {
            val mean = this.average()
            return sqrt(this.fold(0.0) { acc, d -> acc + (d - mean).pow(2.0) } / this.size)
        }

        private fun extractFrequencyFeatures(data: FloatArray): FloatArray {
            val n = data.size
            val fft = FloatFFT_1D(n.toLong())

            // JTransforms requires a double-sized array for real input to store complex outputs.
            val fftData = FloatArray(n)  // Directly use the data array if not modifying original data.
            System.arraycopy(data, 0, fftData, 0, n)

            // Perform the FFT in place for real data
            fft.realForward(fftData)

            // Calculate spectral energy
            var spectralEnergy = 0.0
            var totalMagnitude = 0.0
            val freqs = FloatArray(n/2) { it * 1.0f / n }

            for (i in 0 until n/2) {
                val re = fftData[2*i]   // Real part
                val im = if (i == 0 || 2*i == n - 1) 0.0f else fftData[2*i + 1] // Imaginary part
                val mag = re*re + im*im
                spectralEnergy += mag
                totalMagnitude += Math.sqrt(mag.toDouble())
            }
            spectralEnergy /= n

            // Calculate spectral centroid
            var spectralCentroid = 0.0
            for (i in 0 until n/2) {
                val re = fftData[2*i]
                val im = if (i == 0 || 2*i == n - 1) 0.0f else fftData[2*i + 1]
                val mag = Math.sqrt((re*re + im*im).toDouble())
                spectralCentroid += freqs[i] * mag
            }
            spectralCentroid /= totalMagnitude

            return floatArrayOf(spectralEnergy.toFloat() * 2, spectralCentroid.toFloat())
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
            val frequencyFeatures = extractFrequencyFeatures(data)
            features.addAll(frequencyFeatures.toList())
            return features
        }
    }

}