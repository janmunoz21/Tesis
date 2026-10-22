package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.albumcolaborativo.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    val isLoading by authViewModel.isLoading.collectAsState()
    val errorMessage by authViewModel.errorMessage.collectAsState()

    val primaryColor = Color(0xFF1E7898)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Icono de la camara
            Text(
                text = "📷",
                fontSize = 42.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bienvenido de vuelta",
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Titulo "Ingresar"
            Text(
                text = "Ingresar",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Campo de Email
            Text(
                text = "Email",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.align(Alignment.Start)
            )

            TextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("example@email.com", color = Color.Gray) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = primaryColor,
                    unfocusedIndicatorColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Campo de Contraseña
            Text(
                text = "Contraseña",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor,
                modifier = Modifier.align(Alignment.Start)
            )

            TextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("********", color = Color.Gray) },
                singleLine = true,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password Visibility",
                            tint = Color.Gray
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = primaryColor,
                    unfocusedIndicatorColor = Color.LightGray
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
        }

        // Seccion Inferior (Boton "Ingresar" y enlaces)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    authViewModel.iniciarSesion(email, password) {
                        onNavigateToHome()
                    }
                },
                enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = "Ingresar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Link "¿Olvide mi contraseña? Recuperar" (CU-A-04)
            val forgotPasswordText = buildAnnotatedString {
                append("¿Olvidé mi contraseña? ")
                withStyle(style = SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                    append("Recuperar")
                }
            }
            Text(
                text = forgotPasswordText,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.clickable { showForgotPasswordDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Link "¿Sin cuenta? Registrate"
            val registerText = buildAnnotatedString {
                append("¿Sin cuenta? ")
                withStyle(style = SpanStyle(color = primaryColor, fontWeight = FontWeight.Bold)) {
                    append("Regístrate")
                }
            }
            Text(
                text = registerText,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.clickable { onNavigateToRegister() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // ==========================================
    // CU-A-04: DIÁLOGO RECUPERAR CONTRASEÑA
    // ==========================================
    if (showForgotPasswordDialog) {
        var correoRecuperacion by remember { mutableStateOf("") }
        var isSending by remember { mutableStateOf(false) }
        var dialogError by remember { mutableStateOf<String?>(null) }
        var successMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = {
                if (!isSending) showForgotPasswordDialog = false
            },
            title = { Text("Recuperar contraseña") },
            text = {
                Column {
                    Text("Ingresa el correo registrado asociado a tu cuenta:")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = correoRecuperacion,
                        onValueChange = {
                            correoRecuperacion = it
                            dialogError = null
                        },
                        label = { Text("Correo electrónico") },
                        placeholder = { Text("example@email.com") },
                        singleLine = true,
                        enabled = !isSending && successMsg == null,
                        isError = dialogError != null,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (dialogError != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dialogError!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }

                    if (successMsg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = successMsg!!,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                if (successMsg == null) {
                    Button(
                        enabled = correoRecuperacion.isNotBlank() && !isSending,
                        onClick = {
                            isSending = true
                            dialogError = null
                            authViewModel.solicitarRecuperacionContrasena(correoRecuperacion.trim()) { exito, msj ->
                                isSending = false
                                if (exito) {
                                    successMsg = msj
                                } else {
                                    dialogError = msj
                                }
                            }
                        }
                    ) {
                        if (isSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Enviar")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !isSending,
                    onClick = { showForgotPasswordDialog = false }
                ) {
                    Text(if (successMsg != null) "Cerrar" else "Cancelar")
                }
            }
        )
    }
}
