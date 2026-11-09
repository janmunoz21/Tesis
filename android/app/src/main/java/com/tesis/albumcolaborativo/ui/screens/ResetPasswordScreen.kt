package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tesis.albumcolaborativo.ui.viewmodel.AuthViewModel

@Composable
fun ResetPasswordScreen(
    token: String,
    authViewModel: AuthViewModel,
    onPasswordResetSuccess: () -> Unit
) {
    var nuevaContrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val primaryColor = Color(0xFF1E7898)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔐 Nueva Contraseña",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Ingresa tu nueva clave para actualizar el acceso a tu cuenta.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Campo 1: Nueva Contraseña
        OutlinedTextField(
            value = nuevaContrasena,
            onValueChange = { nuevaContrasena = it },
            label = { Text("Nueva contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Campo 2: Confirmar Contraseña
        OutlinedTextField(
            value = confirmarContrasena,
            onValueChange = { confirmarContrasena = it },
            label = { Text("Confirmar contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        if (successMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = successMessage!!, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (nuevaContrasena != confirmarContrasena) {
                    errorMessage = "Las contraseñas no coinciden"
                    return@Button
                }
                isLoading = true
                errorMessage = null
                authViewModel.restablecerContrasena(token, nuevaContrasena) { exito, msj ->
                    isLoading = false
                    if (exito) {
                        successMessage = msj
                    } else {
                        errorMessage = msj
                    }
                }
            },
            enabled = !isLoading && nuevaContrasena.length >= 8 && successMessage == null,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Guardar Nueva Contraseña", fontWeight = FontWeight.Bold)
            }
        }

        if (successMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onPasswordResetSuccess) {
                Text("Ir a Iniciar Sesión", color = primaryColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}