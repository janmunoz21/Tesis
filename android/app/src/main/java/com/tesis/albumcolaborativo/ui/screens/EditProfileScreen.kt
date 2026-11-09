package com.tesis.albumcolaborativo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tesis.albumcolaborativo.data.local.TokenManager
import com.tesis.albumcolaborativo.ui.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val initialName = tokenManager.obtenerNombreUsuario()
    val userEmail = tokenManager.obtenerEmailUsuario()

    var nombre by remember { mutableStateOf(initialName) }
    var isSavingName by remember { mutableStateOf(false) }
    var nameSuccessMsg by remember { mutableStateOf<String?>(null) }
    var nameErrorMsg by remember { mutableStateOf<String?>(null) }

    // Campos para cambio de contraseña
    var contrasenaActual by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }
    var isPassCurrentVisible by remember { mutableStateOf(false) }
    var isPassNewVisible by remember { mutableStateOf(false) }

    var isSavingPass by remember { mutableStateOf(false) }
    var passSuccessMsg by remember { mutableStateOf<String?>(null) }
    var passErrorMsg by remember { mutableStateOf<String?>(null) }

    val primaryColor = Color(0xFF2196F3)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.Black
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Cabecera con foto de perfil y correo
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(64.dp),
                        shape = CircleShape,
                        color = primaryColor.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = primaryColor,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECCIÓN 1: DATOS PERSONALES (CU-A-05)
            Text(
                text = "Información Personal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = nombre,
                        onValueChange = {
                            nombre = it
                            nameErrorMsg = null
                            nameSuccessMsg = null
                        },
                        label = { Text("Nombre completo") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = primaryColor)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (nameErrorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = nameErrorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    if (nameSuccessMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = nameSuccessMsg!!, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isSavingName = true
                            nameErrorMsg = null
                            nameSuccessMsg = null
                            authViewModel.editarPerfil(nombre.trim()) { exito, error ->
                                isSavingName = false
                                if (exito) {
                                    nameSuccessMsg = "Nombre actualizado correctamente"
                                } else {
                                    nameErrorMsg = error ?: "Error al actualizar"
                                }
                            }
                        },
                        enabled = nombre.isNotBlank() && nombre != initialName && !isSavingName,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isSavingName) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Guardar Nombre", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN 2: CAMBIAR CONTRASEÑA
            Text(
                text = "Seguridad de la Cuenta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    OutlinedTextField(
                        value = contrasenaActual,
                        onValueChange = { contrasenaActual = it; passErrorMsg = null; passSuccessMsg = null },
                        label = { Text("Contraseña actual") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFB300)) },
                        visualTransformation = if (isPassCurrentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPassCurrentVisible = !isPassCurrentVisible }) {
                                Icon(
                                    imageVector = if (isPassCurrentVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = nuevaContrasena,
                        onValueChange = { nuevaContrasena = it; passErrorMsg = null; passSuccessMsg = null },
                        label = { Text("Nueva contraseña") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                        visualTransformation = if (isPassNewVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPassNewVisible = !isPassNewVisible }) {
                                Icon(
                                    imageVector = if (isPassNewVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmarContrasena,
                        onValueChange = { confirmarContrasena = it; passErrorMsg = null; passSuccessMsg = null },
                        label = { Text("Confirmar nueva contraseña") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryColor) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (passErrorMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = passErrorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    if (passSuccessMsg != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = passSuccessMsg!!, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (nuevaContrasena != confirmarContrasena) {
                                passErrorMsg = "Las nuevas contraseñas no coinciden"
                                return@Button
                            }
                            isSavingPass = true
                            passErrorMsg = null
                            passSuccessMsg = null
                            authViewModel.cambiarContrasena(contrasenaActual, nuevaContrasena) { exito, msj ->
                                isSavingPass = false
                                if (exito) {
                                    passSuccessMsg = msj
                                    contrasenaActual = ""
                                    nuevaContrasena = ""
                                    confirmarContrasena = ""
                                } else {
                                    passErrorMsg = msj
                                }
                            }
                        },
                        enabled = contrasenaActual.isNotBlank() && nuevaContrasena.length >= 8 && !isSavingPass,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isSavingPass) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Cambiar Contraseña", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}