package com.bloodconnect.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.R
import com.bloodconnect.ui.components.BloodConnectTextField
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Login Screen connected to [AuthViewModel].
 * Handles user authentication with email and password or Google Sign-In, displaying loading feedback
 * and user-readable error states, and navigating to Home upon successful sign-in.
 */
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val uiState by authViewModel.uiStateFlow.collectAsState()
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                if (idToken != null) {
                    authViewModel.signInWithGoogle(idToken, onNavigateToHome)
                } else {
                    val errorMsg = context.getString(R.string.login_error_token_null)
                    authViewModel.setError(errorMsg)
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }
            } catch (e: ApiException) {
                val msg = when (e.statusCode) {
                    10 -> context.getString(R.string.login_error_dev)
                    12500 -> context.getString(R.string.login_error_oauth)
                    12501 -> context.getString(R.string.login_error_canceled_user)
                    else -> context.getString(R.string.login_error_status_code, e.statusCode)
                }
                authViewModel.setError(msg)
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        } else if (result.resultCode == Activity.RESULT_CANCELED) {
            val msg = context.getString(R.string.login_error_canceled)
            authViewModel.setError(msg)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        } else {
            val msg = context.getString(R.string.login_error_result_code, result.resultCode)
            authViewModel.setError(msg)
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.login_title_app),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.login_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        if (uiState is AuthUiState.Error) authViewModel.clearError()
                    },
                    label = { Text(stringResource(R.string.login_email_label)) },
                    singleLine = true,
                    enabled = uiState !is AuthUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                val hidePasswordDesc = stringResource(R.string.login_cd_hide_password)
                val showPasswordDesc = stringResource(R.string.login_cd_show_password)

                BloodConnectTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        if (uiState is AuthUiState.Error) authViewModel.clearError()
                    },
                    label = stringResource(R.string.login_password_label),
                    singleLine = true,
                    enabled = uiState !is AuthUiState.Loading,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        val description = if (passwordVisible) hidePasswordDesc else showPasswordDesc
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = description
                            )
                        }
                    }
                )

                if (uiState is AuthUiState.Error) {
                    val errorState = uiState as AuthUiState.Error
                    val displayText = if (errorState.messageResId != null) {
                        stringResource(errorState.messageResId)
                    } else {
                        errorState.message
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = displayText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        authViewModel.signIn(email, password, onNavigateToHome)
                    },
                    enabled = uiState !is AuthUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Text(stringResource(R.string.login_btn_sign_in))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val webClientId = try {
                            context.getString(R.string.default_web_client_id)
                        } catch (e: Exception) {
                            "928966163583-8vuu7m6052fbqpg5k77q89mg08thc3du.apps.googleusercontent.com"
                        }

                        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestIdToken(webClientId)
                            .requestEmail()
                            .build()
                        val googleSignInClient = GoogleSignIn.getClient(context, gso)
                        googleSignInClient.signOut().addOnCompleteListener {
                            googleSignInLauncher.launch(googleSignInClient.signInIntent)
                        }
                    },
                    enabled = uiState !is AuthUiState.Loading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.login_btn_google))
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        authViewModel.clearError()
                        onNavigateToRegister()
                    },
                    enabled = uiState !is AuthUiState.Loading
                ) {
                    Text(stringResource(R.string.login_btn_register))
                }
            }
        }
    }
}

