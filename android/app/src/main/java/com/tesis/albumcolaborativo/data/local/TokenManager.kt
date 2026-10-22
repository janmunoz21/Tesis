package com.tesis.albumcolaborativo.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun guardarToken(token: String) {
        val editor = prefs.edit()
        editor.putString("jwt_token", token)
        editor.apply()
    }

    fun obtenerToken(): String? {
        return prefs.getString("jwt_token", null)
    }

    fun eliminarToken() {
        val editor = prefs.edit()
        editor.remove("jwt_token")
        editor.remove("user_name")
        editor.remove("user_email")
        editor.apply()
    }
    fun guardarUsuario(nombre: String, email: String) {
        val editor = prefs.edit()
        editor.putString("user_name", nombre)
        editor.putString("user_email", email)
        editor.apply()
    }

    fun obtenerNombreUsuario(): String {
        return prefs.getString("user_name", null) ?: "Usuario"
    }

    fun obtenerEmailUsuario(): String {
        return prefs.getString("user_email", null) ?: "usuario@correo.com"
    }


}