package com.example.stylemate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.stylemate.viewmodel.AuthViewModel
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun LoginScreen(
    uiState: AuthViewModel.AuthUiState,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
    onClearError: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var localError by rememberSaveable { mutableStateOf<String?>(null) }

    val isSubmitting = uiState.isLoading
    val passwordMismatch = isRegisterMode && confirmPassword.isNotBlank() && confirmPassword != password
    val isFormValid = email.isNotBlank() && password.isNotBlank() && (!isRegisterMode || confirmPassword.isNotBlank()) && !passwordMismatch

    val submitAction: () -> Unit = {
        if (isRegisterMode) {
            if (passwordMismatch) {
                localError = "Mật khẩu xác nhận không khớp"
            } else {
                onRegister(email, password)
            }
        } else {
            onLogin(email, password)
        }
    }

    val screenPadding = 24.dp
    val cardPadding = 20.dp
    val fieldSpacing = 12.dp
    val sectionSpacing = 24.dp
    val buttonSpacing = 16.dp
    val fieldShape = RoundedCornerShape(12.dp)
    val cardShape = RoundedCornerShape(20.dp)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(cardPadding),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRegisterMode) "StyleMate Sign Up" else "StyleMate Login",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(sectionSpacing))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onClearError()
                        },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting,
                        shape = fieldShape,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(fieldSpacing))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            localError = null
                            onClearError()
                        },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            val icon = if (passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            }
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(icon, contentDescription = "Toggle password")
                            }
                        },
                        enabled = !isSubmitting,
                        shape = fieldShape,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!isRegisterMode && isFormValid && !isSubmitting) {
                                    submitAction()
                                }
                            }
                        )
                    )

                    if (isRegisterMode) {
                        Spacer(modifier = Modifier.height(fieldSpacing))
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                localError = null
                                onClearError()
                            },
                            label = { Text("Confirm Password") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            enabled = !isSubmitting,
                            shape = fieldShape,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            isError = passwordMismatch,
                            supportingText = {
                                if (passwordMismatch) {
                                    Text("Mật khẩu xác nhận không khớp")
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (isFormValid && !isSubmitting) {
                                        submitAction()
                                    }
                                }
                            )
                        )
                    }

                    val errorText = localError
                        ?: if (passwordMismatch) "Mật khẩu xác nhận không khớp" else null
                        ?: uiState.errorMessage

                    if (!errorText.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(buttonSpacing))

                    Button(
                        onClick = { submitAction() },
                        enabled = !isSubmitting && isFormValid,
                        modifier = Modifier.fillMaxWidth(),
                        shape = fieldShape
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(18.dp)
                            )
                        } else {
                            Text(if (isRegisterMode) "Đăng ký" else "Đăng nhập")
                        }
                    }

                    Spacer(modifier = Modifier.height(fieldSpacing))
                    TextButton(
                        onClick = {
                            isRegisterMode = !isRegisterMode
                            localError = null
                            onClearError()
                        },
                        enabled = !isSubmitting
                    ) {
                        Text(
                            if (isRegisterMode) {
                                "Đã có tài khoản? Đăng nhập"
                            } else {
                                "Chưa có tài khoản? Đăng ký"
                            }
                        )
                    }
                }
            }
        }
    }
}
