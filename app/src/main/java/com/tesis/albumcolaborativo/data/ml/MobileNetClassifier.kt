
package com.tesis.albumcolaborativo.data.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.json.JSONObject

class MobileNetClassifier(context: Context) : AutoCloseable {

    companion object {
        private const val TAG = "MOBILENET"
        private const val MODEL_NAME = "mobilenet_v3_small.tflite"
        private const val SIZE = 224
        private const val NUM_CLASSES = 1000

        private val MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        private val STD = floatArrayOf(0.229f, 0.224f, 0.225f)

        private const val LABELS_NAME = "imagenet-1k-id2label.json"
    }

    data class ClassificationMetrics(
        val topPredictions: List<Pair<Int, Float>>,
        val tensorPreparationTimeMs: Long,
        val inferenceTimeMs: Long,
        val totalClassificationTimeMs: Long
    )

    private val labels: Map<Int, String> by lazy {
        val json = context.applicationContext.assets
            .open(LABELS_NAME)
            .bufferedReader()
            .use { it.readText() }

        val jsonObject = JSONObject(json)
        val result = mutableMapOf<Int, String>()
        val keys = jsonObject.keys()

        while (keys.hasNext()) {
            val key = keys.next()
            result[key.toInt()] = jsonObject.getString(key)
        }

        result
    }

    fun getLabel(classIndex: Int): String {
        return labels[classIndex] ?: "Clase $classIndex"
    }
    private val interpreter: Interpreter

    init {
        val modelBytes = context.applicationContext.assets
            .open(MODEL_NAME)
            .use { it.readBytes() }

        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size)
            .order(ByteOrder.nativeOrder())
            .apply {
                put(modelBytes)
                rewind()
            }

        interpreter = Interpreter(
            modelBuffer,
            Interpreter.Options().setNumThreads(2)
        )

        Log.i(TAG, "Modelo MobileNetV3 cargado")
    }

    /**
     * Clasifica una imagen guardada como archivo.
     * Devuelve los índices de las cinco clases con mayor puntuación.
     */
    fun classify(imageFile: File): List<Pair<Int, Float>> {
        require(imageFile.exists()) {
            "No existe el archivo: ${imageFile.absolutePath}"
        }

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: throw IllegalArgumentException("No se pudo decodificar la imagen")

        return try {
            val resized = if (
                bitmap.width == SIZE && bitmap.height == SIZE
            ) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true)
            }

            try {
                val input = Array(1) {
                    Array(3) {
                        Array(SIZE) {
                            FloatArray(SIZE)
                        }
                    }
                }

                for (y in 0 until SIZE) {
                    for (x in 0 until SIZE) {
                        val pixel = resized.getPixel(x, y)

                        val rgb = floatArrayOf(
                            android.graphics.Color.red(pixel) / 255f,
                            android.graphics.Color.green(pixel) / 255f,
                            android.graphics.Color.blue(pixel) / 255f
                        )

                        for (c in 0 until 3) {
                            input[0][c][y][x] =
                                (rgb[c] - MEAN[c]) / STD[c]
                        }
                    }
                }

                val output = Array(1) {
                    FloatArray(NUM_CLASSES)
                }

                interpreter.run(input, output)

                output[0].indices
                    .sortedByDescending { output[0][it] }
                    .take(5)
                    .map { index -> index to output[0][index] }

            } finally {
                if (resized !== bitmap) {
                    resized.recycle()
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    fun classifyWithMetrics(imageFile: File): ClassificationMetrics {
        val totalStart = System.nanoTime()

        require(imageFile.exists()) {
            "No existe el archivo: ${imageFile.absolutePath}"
        }

        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            ?: throw IllegalArgumentException(
                "No se pudo decodificar la imagen"
            )

        try {
            val resized = if (
                bitmap.width == SIZE && bitmap.height == SIZE
            ) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, true)
            }

            try {
                val preparationStart = System.nanoTime()

                val input = Array(1) {
                    Array(3) {
                        Array(SIZE) {
                            FloatArray(SIZE)
                        }
                    }
                }

                for (y in 0 until SIZE) {
                    for (x in 0 until SIZE) {
                        val pixel = resized.getPixel(x, y)

                        val red =
                            android.graphics.Color.red(pixel) / 255f
                        val green =
                            android.graphics.Color.green(pixel) / 255f
                        val blue =
                            android.graphics.Color.blue(pixel) / 255f

                        input[0][0][y][x] =
                            (red - MEAN[0]) / STD[0]
                        input[0][1][y][x] =
                            (green - MEAN[1]) / STD[1]
                        input[0][2][y][x] =
                            (blue - MEAN[2]) / STD[2]
                    }
                }

                val preparationTimeMs =
                    (System.nanoTime() - preparationStart) / 1_000_000L

                val output = Array(1) {
                    FloatArray(NUM_CLASSES)
                }

                val inferenceStart = System.nanoTime()
                interpreter.run(input, output)
                val inferenceTimeMs =
                    (System.nanoTime() - inferenceStart) / 1_000_000L

                /*val predictions = output[0].indices
                    .sortedByDescending { output[0][it] }
                    .take(5)
                    .map { index -> index to output[0][index] }*/

                val maxLogit = output[0].maxOrNull() ?: 0f

                val expScores = output[0].map { score ->
                    kotlin.math.exp((score - maxLogit).toDouble())
                }

                val sumExp = expScores.sum()

                require(sumExp.isFinite() && sumExp > 0.0) {
                    "No se pudieron calcular las probabilidades del modelo."
                }

                val probabilities = expScores.map { score ->
                    (score / sumExp).toFloat()
                }

                val predictions = probabilities.indices
                    .sortedByDescending { probabilities[it] }
                    .take(5)
                    .map { index -> index to probabilities[index] }

                val totalTimeMs =
                    (System.nanoTime() - totalStart) / 1_000_000L

                return ClassificationMetrics(
                    topPredictions = predictions,
                    tensorPreparationTimeMs = preparationTimeMs,
                    inferenceTimeMs = inferenceTimeMs,
                    totalClassificationTimeMs = totalTimeMs
                )
            } finally {
                if (resized !== bitmap) {
                    resized.recycle()
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    override fun close() {
        interpreter.close()
        Log.i(TAG, "Modelo MobileNetV3 cerrado")
    }
}
