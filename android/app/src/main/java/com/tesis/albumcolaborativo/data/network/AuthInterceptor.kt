package com.tesis.albumcolaborativo.data.network

import android.content.Context
import android.util.Log
import com.tesis.albumcolaborativo.data.local.TokenManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(private val context: Context) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val tokenManager = TokenManager(context)
        val token = tokenManager.obtenerToken()

        Log.d("AuthInterceptor", "Token obtenido para petición: $token")

        val requestBuilder = chain.request().newBuilder()

        if (!token.isNullOrEmpty()) {
            // Asegúrate de que no tenga comillas dobles al inicio o final si el backend devolvió el String crudo
            val cleanToken = token.replace("\"", "").trim()
            requestBuilder.addHeader("Authorization", "Bearer $cleanToken")
        } else {
            Log.w("AuthInterceptor", "OJO: Intentando hacer petición sin token en SharedPreferences")
        }

        return chain.proceed(requestBuilder.build())
    }
}