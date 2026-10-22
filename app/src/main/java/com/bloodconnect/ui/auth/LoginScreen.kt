package com.bloodconnect.ui.auth

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.R
import com.bloodconnect.ui.components.BloodConnectButton
import com.bloodconnect.ui.components.BloodConnectOutlinedButton
import com.bloodconnect.ui.components.BloodConnectTextField
import com.bloodconnect.ui.theme.BloodConnectTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

/**
 * Login Screen connected to [AuthViewModel].
 * Redesigned in Phase 2 with modern coral, white, and soft-pink styling.
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // App Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Blood",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Connect",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Main White Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.login_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    BloodConnectTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = stringResource(R.string.login_email_label),
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        enabled = uiState !is AuthUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val hidePasswordDesc = stringResource(R.string.login_cd_hide_password)
                    val showPasswordDesc = stringResource(R.string.login_cd_show_password)

                    BloodConnectTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = stringResource(R.string.login_password_label),
                        leadingIcon = Icons.Default.Lock,
                        singleLine = true,
                        enabled = uiState !is AuthUiState.Loading,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (uiState !is AuthUiState.Loading) {
                                    authViewModel.signIn(email, password, onNavigateToHome)
                                }
                            }
                        ),
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
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = displayText,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    BloodConnectButton(
                        text = stringResource(R.string.login_btn_sign_in),
                        onClick = {
                            authViewModel.signIn(email, password, onNavigateToHome)
                        },
                        isLoading = uiState is AuthUiState.Loading,
                        enabled = uiState !is AuthUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    BloodConnectOutlinedButton(
                        text = stringResource(R.string.login_btn_google),
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
                        enabled = uiState !is AuthUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            authViewModel.clearError()
                            onNavigateToRegister()
                        },
                        enabled = uiState !is AuthUiState.Loading
                    ) {
                        Text(
                            text = stringResource(R.string.login_btn_register),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    BloodConnectTheme {
        LoginScreen(
            onNavigateToRegister = {},
            onNavigateToHome = {}
        )
    }
}
