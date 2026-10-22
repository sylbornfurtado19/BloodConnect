package com.bloodconnect.ui.auth

import android.Manifest
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.bloodconnect.ui.components.BloodConnectTextField
import com.bloodconnect.ui.theme.BloodConnectTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

/**
 * Register Screen connected to [AuthViewModel].
 * Redesigned in Phase 2 with modern coral, white, and soft-pink styling.
 * Captures donor information and executes real registration against Firebase Auth & Firestore.
 * Requests location permissions via Accompanist so user coordinates can be recorded non-blockingly upon sign-up.
 * Displays loading indicators and error feedback, navigating to Home upon success.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val uiState by authViewModel.uiStateFlow.collectAsState()

    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
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
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

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

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(R.string.register_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main White Registration Card
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
                        text = stringResource(R.string.register_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Full Name Field
                    BloodConnectTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = stringResource(R.string.register_label_full_name),
                        leadingIcon = Icons.Default.Person,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        enabled = uiState !is AuthUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Blood Group Field
                    BloodConnectTextField(
                        value = bloodGroup,
                        onValueChange = {
                            bloodGroup = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = stringResource(R.string.register_label_blood_group),
                        placeholder = "e.g. A+, B+, O-, AB+",
                        leadingIcon = Icons.Default.Favorite,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        enabled = uiState !is AuthUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Number Field
                    BloodConnectTextField(
                        value = phone,
                        onValueChange = {
                            phone = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = stringResource(R.string.register_label_phone),
                        leadingIcon = Icons.Default.Phone,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        enabled = uiState !is AuthUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Address Field
                    BloodConnectTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = stringResource(R.string.register_label_email),
                        leadingIcon = Icons.Default.Email,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        enabled = uiState !is AuthUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val hidePasswordDesc = stringResource(R.string.register_cd_hide_password)
                    val showPasswordDesc = stringResource(R.string.register_cd_show_password)

                    // Password Field
                    BloodConnectTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            if (uiState is AuthUiState.Error) authViewModel.clearError()
                        },
                        label = stringResource(R.string.register_label_password),
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
                                    authViewModel.signUp(fullName, bloodGroup, email, password, phone, onNavigateToHome)
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
                        text = stringResource(R.string.register_btn_register),
                        onClick = {
                            authViewModel.signUp(fullName, bloodGroup, email, password, phone, onNavigateToHome)
                        },
                        isLoading = uiState is AuthUiState.Loading,
                        enabled = uiState !is AuthUiState.Loading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    TextButton(
                        onClick = {
                            authViewModel.clearError()
                            onNavigateToLogin()
                        },
                        enabled = uiState !is AuthUiState.Loading
                    ) {
                        Text(
                            text = stringResource(R.string.register_btn_sign_in),
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
private fun RegisterScreenPreview() {
    BloodConnectTheme {
        RegisterScreen(
            onNavigateToLogin = {},
            onNavigateToHome = {}
        )
    }
}
