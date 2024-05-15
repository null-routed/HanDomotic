package it.unipi.masss.classifiertest.presentation

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
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
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.InputStreamReader

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
    // Create an OrtSession with the given OrtEnvironment
    private fun createORTSession( ortEnvironment: OrtEnvironment) : OrtSession {
        val modelBytes = resources.openRawResource( R.raw.gesture_classifier).readBytes()
        return ortEnvironment.createSession( modelBytes )
    }

    // Make predictions from a given input
    private fun runPrediction(input: FloatArray, ortSession: OrtSession, ortEnvironment: OrtEnvironment): Pair<Array<FloatArray>, Array<String>> {
        // Get the name of the input node
        val inputName = ortSession.inputNames?.iterator()?.next()

        val floatBufferInputs = FloatBuffer.wrap(input)
        // Create input tensor with floatBufferInputs of shape (1, 30)
        val inputTensor = OnnxTensor.createTensor(ortEnvironment, floatBufferInputs, longArrayOf(1, 33))
        // Run the model
        val results = ortSession.run(mapOf(inputName to inputTensor))
        val preProbabilities = results[1].value as Array<FloatArray>
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
            /*val inputs = floatArrayOf(
                0.6714647492727273f, 1.3284390079341724f, -2.5785553f, 2.7940333f, 1.2450284143636359f,
                121.8588309024358f, 6.0f, -0.4037437158434334f, 144.03753662356294f, 0.0f, -4.922348016181818f,
                6.558244526993268f, -19.302053f, 7.66384f, 5.865837622000001f, 3698.2044697535434f, 5.0f,
                -0.5550304390739136f, 4940.991313633613f, 0.0f, 4.861883669999999f, 8.529836947341735f,
                -12.258312f, 17.104177f, 8.775228053636363f, 5301.781714286688f, 2.0f, -0.7441296674056268f,
                6483.976438716447f, 0.01818181818181818f
            )*/

            /*val inputs = floatArrayOf(-2.0351584947090906f, 2.2859100726954944f, -7.563283f, 3.3758245f, 2.4176213410727274f, 515.1990227470511f, 8f, 2.73424182369022f, -0.22397272080358266f, 729.7339874877176f, 0.0f, -1.5346832769090908f, 6.0355624364868605f, -15.160085f, 6.7492547f, 4.860360026727273f, 2133.0796676835594f, 4f, 2.4959790576431775f, -0.6940063667650447f, 2222.214666479994f, 0.03636363636363636f, 10.32684492f, 9.411386959967887f, -4.912902f, 25.649082f, 11.807767519999999f, 10736.986178159086f, 2f, 1.8089441868040197f, -0.2114432650644774f, 16305.919838463975f, 0.0f)*/
            val inputs = leggiFileToArrayFloat(this, R.raw.inputs)
            for(input in inputs){
                println("FloatArray: ${input.toFloatArray().joinToString()}")

                val ortEnvironment = OrtEnvironment.getEnvironment()
                val ortSession = createORTSession( ortEnvironment )
                val output = runPrediction( input.toFloatArray() , ortSession , ortEnvironment )

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
}