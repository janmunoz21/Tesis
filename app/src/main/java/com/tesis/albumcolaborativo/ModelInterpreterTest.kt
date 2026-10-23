
package com.tesis.albumcolaborativo

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ModelInterpreterTest {

    private const val TAG = "TFLITE_INTERPRETER"
    private const val MODEL_NAME = "mobilenet_v3_small.tflite"
    private const val SIZE = 224
    private const val NUM_CLASSES = 1000

    fun runInference(context: Context): String {
        var interpreter: Interpreter? = null

        return try {
            Log.i(TAG, "INFERENCIA: iniciando")

            // 1. Leer el modelo
            val bytes = context.assets.open(MODEL_NAME).use {
                it.readBytes()
            }

            val modelBuffer = ByteBuffer
                .allocateDirect(bytes.size)
                .order(ByteOrder.nativeOrder())
                .apply {
                    put(bytes)
                    rewind()
                }

            Log.i(TAG, "Modelo leído: ${bytes.size} bytes")

            // 2. Crear el intérprete
            interpreter = Interpreter(
                modelBuffer,
                Interpreter.Options().setNumThreads(2)
            )

            Log.i(TAG, "Intérprete creado")

            // 3. Crear una imagen sintética de prueba
            // Todavía no usamos una fotografía real.
            val bitmap = Bitmap.createBitmap(
                SIZE,
                SIZE,
                Bitmap.Config.ARGB_8888
            )

            // Patrón de colores para probar la entrada.
            for (y in 0 until SIZE) {
                for (x in 0 until SIZE) {
                    val red = x * 255 / (SIZE - 1)
                    val green = y * 255 / (SIZE - 1)
                    val blue = (x + y) * 255 / (2 * (SIZE - 1))

                    bitmap.setPixel(
                        x,
                        y,
                        android.graphics.Color.rgb(red, green, blue)
                    )
                }
            }

            // 4. Preparar entrada FLOAT32 en formato NCHW:
            // [lote][canal][alto][ancho]
            val input = Array(1) {
                Array(3) {
                    Array(SIZE) {
                        FloatArray(SIZE)
                    }
                }
            }

            val mean = floatArrayOf(
                0.485f, 0.456f, 0.406f
            )
            val std = floatArrayOf(
                0.229f, 0.224f, 0.225f
            )

            for (y in 0 until SIZE) {
                for (x in 0 until SIZE) {
                    val pixel = bitmap.getPixel(x, y)

                    val rgb = floatArrayOf(
                        android.graphics.Color.red(pixel) / 255f,
                        android.graphics.Color.green(pixel) / 255f,
                        android.graphics.Color.blue(pixel) / 255f
                    )

                    for (c in 0 until 3) {
                        input[0][c][y][x] =
                            (rgb[c] - mean[c]) / std[c]
                    }
                }
            }

            Log.i(TAG, "Entrada preparada: [1, 3, 224, 224]")

            // 5. Ejecutar la inferencia
            val output = Array(1) {
                FloatArray(NUM_CLASSES)
            }

            interpreter.run(input, output)

            Log.i(TAG, "INFERENCIA EJECUTADA")

            // 6. Mostrar los cinco valores más altos
            val top5 = output[0]
                .indices
                .sortedByDescending { output[0][it] }
                .take(5)

            val result = buildString {
                appendLine("Inferencia completada.")
                appendLine("Número de salidas: ${output[0].size}")
                appendLine("Cinco índices con mayor puntuación:")

                for (index in top5) {
                    appendLine(
                        "Índice $index: ${output[0][index]}"
                    )
                }
            }

            Log.i(TAG, result)

            bitmap.recycle()

            result

        } catch (e: Exception) {
            Log.e(TAG, "Error durante la inferencia", e)
            "ERROR: ${e.javaClass.simpleName}: ${e.message}"

        } finally {
            interpreter?.close()
            Log.i(TAG, "Intérprete cerrado")
        }
    }
}
