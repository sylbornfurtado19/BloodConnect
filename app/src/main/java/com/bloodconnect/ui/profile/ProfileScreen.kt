package com.bloodconnect.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bloodconnect.ui.components.BloodConnectButton
import com.bloodconnect.ui.components.BloodConnectOutlinedButton
import com.bloodconnect.ui.components.BloodConnectTextField
import com.bloodconnect.ui.components.BloodGroupBadge
import com.bloodconnect.ui.components.SectionHeader
import com.bloodconnect.ui.theme.BloodConnectTheme

private val standardBloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

/**
 * Screen displaying the user's donor profile, availability status, and general donation guidelines.
 * Uses local Compose state for demo customization and profile editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null
) {
    // Active Profile Demo State (Fictional initial values)
    var displayName by remember { mutableStateOf("Alex Rivera") }
    var bloodGroup by remember { mutableStateOf("O+") }
    var cityLocality by remember { mutableStateOf("Central Metro District") }
    var contactInfo by remember { mutableStateOf("alex.demo@example.com") }
    var isDonorAvailable by remember { mutableStateOf(true) }

    // Edit Mode Form State
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(displayName) }
    var editBloodGroup by remember { mutableStateOf(bloodGroup) }
    var editCity by remember { mutableStateOf(cityLocality) }
    var editContact by remember { mutableStateOf(contactInfo) }

    // Validation State
    var nameError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }

    // Feedback Confirmation Dialog
    var showSuccessDialog by remember { mutableStateOf(false) }

    fun startEditing() {
        editName = displayName
        editBloodGroup = bloodGroup
        editCity = cityLocality
        editContact = contactInfo
        nameError = null
        cityError = null
        isEditing = true
    }

    fun cancelEditing() {
        editName = displayName
        editBloodGroup = bloodGroup
        editCity = cityLocality
        editContact = contactInfo
        nameError = null
        cityError = null
        isEditing = false
    }

    fun validateAndSave() {
        var isValid = true

        if (editName.trim().isBlank()) {
            nameError = "Display name cannot be empty"
            isValid = false
        } else {
            nameError = null
        }

        if (editCity.trim().isBlank()) {
            cityError = "City/locality cannot be empty"
            isValid = false
        } else {
            cityError = null
        }

        if (isValid) {
            displayName = editName.trim()
            bloodGroup = editBloodGroup
            cityLocality = editCity.trim()
            contactInfo = editContact.trim()
            isEditing = false
            showSuccessDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Donor Profile",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Demonstration Data Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Demonstration Profile",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "This screen displays fictional sample profile data. Edits are handled in local UI state and are not saved to a server or permanent database.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 1. Profile Header & Avatar Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar Placeholder with Initials
                    val initials = displayName
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "BC" }

                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BloodGroupBadge(
                            bloodGroup = bloodGroup,
                            size = 36.dp
                        )
                        Text(
                            text = "Registered Blood Group",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. Donor Availability Toggle Card (Immediate local UI state)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isDonorAvailable) Icons.Default.Favorite else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (isDonorAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                        Column {
                            Text(
                                text = if (isDonorAvailable) "Available to Donate" else "Not Available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDonorAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (isDonorAvailable) "Your status is marked active for emergency requests (local demo state)." else "Status marked inactive in local demo state. You will not receive donor alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Switch(
                        checked = isDonorAvailable,
                        onCheckedChange = { isDonorAvailable = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // 3. Profile Information / Edit Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditing) "Edit Profile Details" else "Profile Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (!isEditing) {
                            TextButton(onClick = { startEditing() }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit")
                            }
                        }
                    }

                    if (isEditing) {
                        // Editable Form Fields
                        BloodConnectTextField(
                            value = editName,
                            onValueChange = {
                                editName = it
                                nameError = null
                            },
                            label = "Display Name *",
                            placeholder = "Enter your full name",
                            leadingIcon = Icons.Default.Person,
                            isError = nameError != null,
                            errorMessage = nameError
                        )

                        // Blood Group Selection Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Blood Group *",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                standardBloodGroups.forEach { group ->
                                    FilterChip(
                                        selected = editBloodGroup == group,
                                        onClick = { editBloodGroup = group },
                                        label = { Text(group) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        BloodConnectTextField(
                            value = editCity,
                            onValueChange = {
                                editCity = it
                                cityError = null
                            },
                            label = "City / Locality *",
                            placeholder = "e.g., Central Sector, Downtown",
                            leadingIcon = Icons.Default.LocationCity,
                            isError = cityError != null,
                            errorMessage = cityError
                        )

                        BloodConnectTextField(
                            value = editContact,
                            onValueChange = { editContact = it },
                            label = "Contact Info (Optional Demo Email/Phone)",
                            placeholder = "e.g., alex.demo@example.com",
                            leadingIcon = Icons.Default.Email
                        )

                        // Action Buttons: Save & Cancel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BloodConnectOutlinedButton(
                                text = "Cancel",
                                onClick = { cancelEditing() },
                                modifier = Modifier.weight(1f)
                            )

                            BloodConnectButton(
                                text = "Save Profile",
                                icon = Icons.Default.Save,
                                onClick = { validateAndSave() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        // Read-Only Summary Display
                        ProfileDetailRow(
                            label = "Full Name",
                            value = displayName
                        )
                        ProfileDetailRow(
                            label = "Blood Group",
                            value = bloodGroup
                        )
                        ProfileDetailRow(
                            label = "City / Locality",
                            value = cityLocality
                        )
                        ProfileDetailRow(
                            label = "Contact",
                            value = contactInfo.ifBlank { "Not provided" }
                        )

                        BloodConnectOutlinedButton(
                            text = "Edit Profile Information",
                            icon = Icons.Default.Edit,
                            onClick = { startEditing() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 4. Blood Donation Eligibility Information Section
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    title = "Donation Eligibility Guidelines",
                    subtitle = "General criteria for potential blood donors"
                )

                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EligibilityPoint(
                            text = "Age: Typically between 18 and 65 years."
                        )
                        EligibilityPoint(
                            text = "Weight: Minimum body weight of 50 kg (110 lbs)."
                        )
                        EligibilityPoint(
                            text = "General Health: In good physical health with normal vital signs on donation day."
                        )
                        EligibilityPoint(
                            text = "Donation Interval: At least 84 days (12 weeks) between whole blood donations."
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Educational Medical Disclaimer
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Important Notice: Eligibility criteria vary depending on regional blood service guidelines and individual health evaluations conducted by licensed medical practitioners. This application does not diagnose, verify medical fitness, or certify eligibility to donate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // 5. Sign Out Action (Preserves clean authentication separation)
            if (onSignOut != null) {
                BloodConnectOutlinedButton(
                    text = "Sign Out",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation Feedback Dialog for Local Save
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Profile Updated Locally",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Demo profile updated locally.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Updated profile for $displayName ($bloodGroup) in $cityLocality.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Note: These changes are retained in local session state only and are not synced to an account or remote database.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ProfileDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun EligibilityPoint(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    BloodConnectTheme {
        ProfileScreen(onNavigateBack = {})
    }
}
