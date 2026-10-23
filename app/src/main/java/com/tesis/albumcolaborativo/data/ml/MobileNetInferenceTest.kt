
package com.tesis.albumcolaborativo.data.ml

import android.content.Context
import android.util.Log
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MobileNetInferenceTest {

    private const val TAG = "MOBILENET_TEST"

    suspend fun testImage(
        context: Context,
        imageFile: File
    ) {
        withContext(Dispatchers.IO) {
            var classifier: MobileNetClassifier? = null

            try {
                require(imageFile.exists()) {
                    "No existe el archivo: ${imageFile.absolutePath}"
                }

                Log.i(TAG, "Probando: ${imageFile.absolutePath}")

                classifier = MobileNetClassifier(context)

                val results = classifier.classify(imageFile)

                results.forEachIndexed { position, result ->
                    Log.i(
                        TAG,
                        "Puesto ${position + 1}: " +
                                "índice=${result.first}, " +
                                "puntuación=${result.second}"
                    )
                }

                Log.i(TAG, "Prueba terminada correctamente")

            } catch (e: Exception) {
                Log.e(TAG, "Error durante la inferencia", e)
            } finally {
                classifier?.close()
            }
        }
    }
}
