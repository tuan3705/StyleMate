package com.example.stylemate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.stylemate.R
import com.example.stylemate.viewmodel.AuthViewModel

/**
 * Màn hình Login/SignUp theo kiến trúc MVVM.
 *
 * Composable KHÔNG quản lý state nào.
 * Toàn bộ form state, validation, loading đều do [AuthViewModel] quản lý.
 */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoggedIn: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    when (val state = uiState) {
        is AuthViewModel.LoginUiState.Loading -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        is AuthViewModel.LoginUiState.Form -> {
            LoginForm(
                state = state,
                onNameChange = viewModel::updateName,
                onEmailChange = viewModel::updateEmail,
                onPasswordChange = viewModel::updatePassword,
                onConfirmPasswordChange = viewModel::updateConfirmPassword,
                onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
                onToggleRegisterMode = viewModel::toggleRegisterMode,
                onSubmit = viewModel::submit,
                onClearError = viewModel::clearError,
                focusManager = focusManager
            )
        }

        is AuthViewModel.LoginUiState.LoggedIn -> {
            // Handled by AppNavigation
            onLoggedIn()
        }
    }
}

@Composable
private fun LoginForm(
    state: AuthViewModel.LoginUiState.Form,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleRegisterMode: () -> Unit,
    onSubmit: () -> Unit,
    onClearError: () -> Unit,
    focusManager: FocusManager
) {
    val isSubmitting = state.isLoading
    val canSubmit = if (state.isRegisterMode) {
        state.email.isNotBlank() &&
                state.password.isNotBlank() &&
                state.confirmPassword.isNotBlank() &&
                !state.isPasswordMismatch
    } else {
        state.isFormValid
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (state.isRegisterMode)
                            stringResource(R.string.signup_title)
                        else
                            stringResource(R.string.login_title),
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (state.isRegisterMode) {
                        val isNameMissing = state.validationError == "name_required"
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = {
                                onNameChange(it)
                                onClearError()
                            },
                            label = { Text(stringResource(R.string.name_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isSubmitting,
                            shape = MaterialTheme.shapes.small,
                            singleLine = true,
                            isError = isNameMissing,
                            supportingText = {
                                if (isNameMissing) {
                                    Text(stringResource(R.string.name_required))
                                }
                            },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = state.email,
                        onValueChange = {
                            onEmailChange(it)
                            onClearError()
                        },
                        label = { Text(stringResource(R.string.email_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting,
                        shape = MaterialTheme.shapes.small,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = {
                            onPasswordChange(it)
                            onClearError()
                        },
                        label = { Text(stringResource(R.string.password_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (state.passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            val icon = if (state.passwordVisible) {
                                Icons.Filled.VisibilityOff
                            } else {
                                Icons.Filled.Visibility
                            }
                            IconButton(onClick = onTogglePasswordVisibility) {
                                Icon(
                                    icon,
                                    contentDescription = stringResource(R.string.toggle_password_content_desc)
                                )
                            }
                        },
                        enabled = !isSubmitting,
                        shape = MaterialTheme.shapes.small,
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (state.isRegisterMode) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (!state.isRegisterMode && state.isFormValid && !isSubmitting) {
                                    onSubmit()
                                }
                            },
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    )

                    if (state.isRegisterMode) {
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = state.confirmPassword,
                            onValueChange = {
                                onConfirmPasswordChange(it)
                                onClearError()
                            },
                            label = { Text(stringResource(R.string.confirm_password_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = if (state.passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            enabled = !isSubmitting,
                            shape = MaterialTheme.shapes.small,
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                            isError = state.isPasswordMismatch,
                            supportingText = {
                                if (state.isPasswordMismatch) {
                                    Text(stringResource(R.string.password_mismatch))
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (state.isFormValid && !isSubmitting) {
                                        onSubmit()
                                    }
                                }
                            )
                        )
                    }

                    val displayError = if (state.validationError == "name_required") {
                        state.errorMessage
                    } else {
                        state.validationError ?: state.errorMessage
                    }
                    if (displayError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (displayError) {
                                "name_required" -> stringResource(R.string.name_required)
                                "password_mismatch" -> stringResource(R.string.password_mismatch)
                                "session_expired" -> stringResource(R.string.session_expired)
                                else -> displayError
                            },
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSubmit,
                        enabled = !isSubmitting && canSubmit,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(18.dp)
                            )
                        } else {
                            Text(
                                if (state.isRegisterMode)
                                    stringResource(R.string.register_button)
                                else
                                    stringResource(R.string.login_button)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = onToggleRegisterMode,
                        enabled = !isSubmitting
                    ) {
                        Text(
                            if (state.isRegisterMode)
                                stringResource(R.string.has_account_prompt)
                            else
                                stringResource(R.string.no_account_prompt)
                        )
                    }
                }
            }
        }
    }
}

// --- Previews ---

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(viewModel = androidx.lifecycle.viewmodel.compose.viewModel(), onLoggedIn = {})
}