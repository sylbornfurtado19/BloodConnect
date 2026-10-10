package com.bloodconnect.ui.request

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationCity
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bloodconnect.ui.components.BloodConnectButton
import com.bloodconnect.ui.components.BloodConnectTextField
import com.bloodconnect.ui.components.BloodGroupBadge
import com.bloodconnect.ui.components.SectionHeader
import com.bloodconnect.ui.theme.BloodConnectTheme

/**
 * Fictional sample request data structure for demonstration purposes.
 * Note: These do not represent real patients, contacts, or medical records.
 */
private data class SampleRequestEntry(
    val id: String,
    val bloodGroup: String,
    val hospital: String,
    val city: String,
    val units: Int,
    val urgency: String,
    val timeLabel: String
)

private val sampleRequestsList = listOf(
    SampleRequestEntry(
        id = "R1",
        bloodGroup = "O+",
        hospital = "City General Hospital (Emergency Wing)",
        city = "Central Sector",
        units = 2,
        urgency = "Critical",
        timeLabel = "15 mins ago"
    ),
    SampleRequestEntry(
        id = "R2",
        bloodGroup = "A-",
        hospital = "Metro Care Medical Center",
        city = "Westside District",
        units = 1,
        urgency = "Urgent",
        timeLabel = "1 hour ago"
    ),
    SampleRequestEntry(
        id = "R3",
        bloodGroup = "B+",
        hospital = "St. Jude Regional Clinic",
        city = "North Valley Ward",
        units = 3,
        urgency = "Routine",
        timeLabel = "3 hours ago"
    )
)

private val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
private val urgencyLevels = listOf("Routine", "Urgent", "Critical")

/**
 * Screen for creating new emergency blood requests and viewing recent illustrative requests.
 * Uses local Compose state for interactive form validation and demo feedback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodRequestsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Form input states
    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var unitsRequired by remember { mutableStateOf("1") }
    var hospitalName by remember { mutableStateOf("") }
    var cityLocality by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf("Urgent") }
    var additionalNotes by remember { mutableStateOf("") }

    // Validation error states
    var bloodGroupError by remember { mutableStateOf<String?>(null) }
    var unitsError by remember { mutableStateOf<String?>(null) }
    var hospitalError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }

    // Dialog state
    var showConfirmationDialog by remember { mutableStateOf(false) }

    fun validateAndSubmit() {
        var isValid = true

        if (selectedBloodGroup == null) {
            bloodGroupError = "Please select a blood group"
            isValid = false
        } else {
            bloodGroupError = null
        }

        val parsedUnits = unitsRequired.trim().toIntOrNull()
        if (parsedUnits == null || parsedUnits <= 0) {
            unitsError = "Please enter a valid positive number of units"
            isValid = false
        } else if (parsedUnits > 20) {
            unitsError = "Units cannot exceed 20 for a single request"
            isValid = false
        } else {
            unitsError = null
        }

        if (hospitalName.trim().isBlank()) {
            hospitalError = "Hospital/facility name is required"
            isValid = false
        } else {
            hospitalError = null
        }

        if (cityLocality.trim().isBlank()) {
            cityError = "City or locality is required"
            isValid = false
        } else {
            cityError = null
        }

        if (isValid) {
            showConfirmationDialog = true
        }
    }

    fun resetForm() {
        selectedBloodGroup = null
        unitsRequired = "1"
        hospitalName = ""
        cityLocality = ""
        selectedUrgency = "Urgent"
        additionalNotes = ""
        bloodGroupError = null
        unitsError = null
        hospitalError = null
        cityError = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Blood Requests",
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
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Screen Introduction
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Request Emergency Blood",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Post an emergency blood request form for hospital verification and donor notification.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // SECTION A: Create a Blood Request Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "New Blood Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // 1. Blood Group Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Blood Group Required *",
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
                            bloodGroups.forEach { group ->
                                FilterChip(
                                    selected = selectedBloodGroup == group,
                                    onClick = {
                                        selectedBloodGroup = group
                                        bloodGroupError = null
                                    },
                                    label = { Text(group) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }

                        if (bloodGroupError != null) {
                            Text(
                                text = bloodGroupError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                            )
                        }
                    }

                    // 2. Units Required
                    BloodConnectTextField(
                        value = unitsRequired,
                        onValueChange = {
                            unitsRequired = it
                            unitsError = null
                        },
                        label = "Units Required (Pints) *",
                        placeholder = "e.g., 1, 2, 3",
                        isError = unitsError != null,
                        errorMessage = unitsError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // 3. Hospital Name
                    BloodConnectTextField(
                        value = hospitalName,
                        onValueChange = {
                            hospitalName = it
                            hospitalError = null
                        },
                        label = "Hospital / Medical Facility *",
                        placeholder = "e.g., City General Hospital",
                        leadingIcon = Icons.Default.LocalHospital,
                        isError = hospitalError != null,
                        errorMessage = hospitalError
                    )

                    // 4. City / Locality
                    BloodConnectTextField(
                        value = cityLocality,
                        onValueChange = {
                            cityLocality = it
                            cityError = null
                        },
                        label = "City / Locality *",
                        placeholder = "e.g., Central District, Downtown",
                        leadingIcon = Icons.Default.LocationCity,
                        isError = cityError != null,
                        errorMessage = cityError
                    )

                    // 5. Urgency Selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Urgency Level *",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            urgencyLevels.forEach { level ->
                                FilterChip(
                                    selected = selectedUrgency == level,
                                    onClick = { selectedUrgency = level },
                                    label = { Text(level) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (level) {
                                            "Critical" -> MaterialTheme.colorScheme.primary
                                            "Urgent" -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        selectedLabelColor = when (level) {
                                            "Critical" -> MaterialTheme.colorScheme.onPrimary
                                            "Urgent" -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    )
                                )
                            }
                        }
                    }

                    // 6. Additional Notes (Optional)
                    BloodConnectTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = "Additional Notes (Optional)",
                        placeholder = "e.g., Contact Blood Bank desk, Patient Bed #12",
                        singleLine = false
                    )

                    // Submit Button
                    BloodConnectButton(
                        text = "Submit Blood Request",
                        icon = Icons.Default.AddAlert,
                        onClick = { validateAndSubmit() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // SECTION B: Existing Sample Requests
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(
                    title = "Active Blood Requests (Sample)",
                    subtitle = "Illustrative emergency feeds for demonstration"
                )

                sampleRequestsList.forEach { request ->
                    SampleRequestRowCard(entry = request)
                }

                Text(
                    text = "* Sample demonstration requests. Real requests will connect to live hospital broadcasts in future phases.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Confirmation Dialog
    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmationDialog = false
                resetForm()
            },
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
                    text = "Demo Request Validated",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Demo request validated successfully. Backend submission will be available after integration.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Summary: $selectedBloodGroup (${unitsRequired.trim()} unit(s)) at ${hospitalName.trim()}, ${cityLocality.trim()} [Urgency: $selectedUrgency]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Note: No real broadcast was transmitted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        resetForm()
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}

/**
 * Card displaying an individual sample blood request entry.
 */
@Composable
private fun SampleRequestRowCard(
    entry: SampleRequestEntry,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            BloodGroupBadge(
                bloodGroup = entry.bloodGroup,
                size = 48.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = entry.hospital,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${entry.city} · ${entry.timeLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${entry.units} Unit(s) Needed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            SuggestionChip(
                onClick = {},
                label = { Text(entry.urgency) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = when (entry.urgency) {
                        "Critical" -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        "Urgent" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                    },
                    labelColor = when (entry.urgency) {
                        "Critical" -> MaterialTheme.colorScheme.error
                        "Urgent" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BloodRequestsScreenPreview() {
    BloodConnectTheme {
        BloodRequestsScreen(onNavigateBack = {})
    }
}
