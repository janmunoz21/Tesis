package com.tesis.albumcolaborativo

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

object ModelCompatibilityTest {

    private const val TAG = "TFLITE_COMPAT"
    private const val MODEL_NAME = "mobilenet_v3_small.tflite"

    fun check(context: Context): String {
        var interpreter: Interpreter? = null

        return try {
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

            interpreter = Interpreter(modelBuffer)

            val inputs = (0 until interpreter.inputTensorCount).map { index ->
                val tensor = interpreter.getInputTensor(index)
                "Entrada $index: forma=${tensor.shape().contentToString()}, " +
                        "tipo=${tensor.dataType()}"
            }

            val outputs = (0 until interpreter.outputTensorCount).map { index ->
                val tensor = interpreter.getOutputTensor(index)
                "Salida $index: forma=${tensor.shape().contentToString()}, " +
                        "tipo=${tensor.dataType()}"
            }

            val result = (
                    listOf("Modelo cargado correctamente") + inputs + outputs
                    ).joinToString("\n")

            Log.i(TAG, result)
            result

        } catch (e: Throwable) {
            Log.e(TAG, "FALLO al cargar o inspeccionar el modelo", e)

            "ERROR: ${e.javaClass.simpleName}: ${e.message}"
        } finally {
            interpreter?.close()
        }
    }
}