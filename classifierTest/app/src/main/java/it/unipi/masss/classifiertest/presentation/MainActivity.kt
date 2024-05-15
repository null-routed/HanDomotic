package it.unipi.masss.classifiertest.presentation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import it.unipi.masss.classifiertest.R
import it.unipi.masss.classifiertest.presentation.theme.ClassifierTestTheme
import java.nio.DoubleBuffer
import java.nio.FloatBuffer
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    fun convertPrimitiveFloatArray2DToFloatObjectArray2D(floatArray2D: Array<FloatArray>): Array<Array<Float>> {
        return Array(floatArray2D.size) { i ->
            Array(floatArray2D[i].size) { j ->
                floatArray2D[i][j].toFloat()
            }
        }
    }
    // Create an OrtSession with the given OrtEnvironment
    private fun createORTSession( ortEnvironment: OrtEnvironment) : OrtSession {
        val modelBytes = resources.openRawResource( R.raw.gesture_classifier).readBytes()
        return ortEnvironment.createSession( modelBytes )
    }

    // Make predictions from a given input
    private fun runPrediction(input: DoubleArray, ortSession: OrtSession, ortEnvironment: OrtEnvironment): Pair<Array<DoubleArray>, Array<String>> {
        // Get the name of the input node
        val inputName = ortSession.inputNames?.iterator()?.next()
        // Make a FloatBuffer of the inputs
        val doubleBufferInputs = DoubleBuffer.wrap(input)
        // Create input tensor with floatBufferInputs of shape (1, 30)
        val inputTensor = OnnxTensor.createTensor(ortEnvironment, doubleBufferInputs, longArrayOf(1, 30))
        // Run the model
        val results = ortSession.run(mapOf(inputName to inputTensor))
        val preProbabilities = results[1].value as Array<DoubleArray>
        // val probabilities =  convertPrimitiveFloatArray2DToFloatObjectArray2D(preProbabilities) as FloatArray
        val predictions = results[0].value as Array<String>
        inputTensor.close()
        // val output = results as Array<String>
        // Fetch and return the results as a string
        return preProbabilities to predictions
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        System.getProperty("os.arch", "generic")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pippo)
        // setTheme(android.R.style.Theme_DeviceDefault)
        val button : Button = findViewById(R.id.button2)
        val outputTextView : TextView = findViewById(R.id.textView2)

        button.setOnClickListener{
            // Giving in input an array of 30 random floats
            // val floatArraySize = 30
            // val inputs = FloatArray(floatArraySize) { Random.nextFloat() }

            // Trying with features related to a CIRCLE gesture (custom passed)
            val inputs = doubleArrayOf(
                0.6714647492727273, 1.3284390079341724, -2.5785553, 2.7940333, 1.2450284143636359,
                121.8588309024358, 6.0, -0.4037437158434334, 144.03753662356294, 0.0, -4.922348016181818,
                6.558244526993268, -19.302053, 7.66384, 5.865837622000001, 3698.2044697535434, 5.0,
                -0.5550304390739136, 4940.991313633613, 0.0, 4.861883669999999, 8.529836947341735,
                -12.258312, 17.104177, 8.775228053636363, 5301.781714286688, 2.0, -0.7441296674056268,
                6483.976438716447, 0.01818181818181818
            )
            println("FloatArray: ${inputs.joinToString()}")

            val ortEnvironment = OrtEnvironment.getEnvironment()
            val ortSession = createORTSession( ortEnvironment )
            val output = runPrediction( inputs , ortSession , ortEnvironment )

            val confidence = output.first
            val prediction = output.second

            println("Matrice di Confidence:")
            var rowIndex = 1
            for (row in confidence) {
                print("Riga $rowIndex: ")
                for (value in row) {
                    print("$value ")
                }
                println() // Va a capo dopo aver stampato tutti i valori di una riga
                rowIndex++
            }
            println() // Vai a capo dopo ogni riga
            // Stampa la matrice di prediction
            println("Matrice di Prediction:")
            for (value in prediction) {
                    print("$value ")
                println() // Vai a capo dopo ogni riga
            }
        }
    }
}