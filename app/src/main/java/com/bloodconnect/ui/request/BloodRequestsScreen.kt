package com.bloodconnect.ui.request

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.DonationEligibility
import com.bloodconnect.data.model.Urgency
import com.bloodconnect.data.model.User
import com.bloodconnect.ui.components.BloodConnectButton
import com.bloodconnect.ui.components.BloodConnectOutlinedButton
import com.bloodconnect.ui.components.BloodConnectTextField
import com.bloodconnect.ui.components.BloodGroupBadge
import com.bloodconnect.ui.components.EmptyState
import com.bloodconnect.ui.components.ReportDialog
import com.bloodconnect.ui.components.SectionHeader
import com.bloodconnect.ui.theme.BloodConnectTheme
import com.bloodconnect.utils.IntentHelpers
import kotlinx.coroutines.launch

private data class PendingBloodRequest(
    val bloodGroup: String,
    val units: Int,
    val hospital: String,
    val city: String,
    val urgency: String,
    val notes: String,
    val contactNumber: String,
    val patientReference: String
)

private val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
private val urgencyLevels = Urgency.entries.map { it.displayName }
private val etaOptions = listOf(15, 30, 60, 120) // minutes

private fun getDismissedRequests(context: Context): Set<String> {
    val prefs = context.getSharedPreferences("dismissed_requests_prefs", Context.MODE_PRIVATE)
    return prefs.getStringSet("dismissed_requests", emptySet()) ?: emptySet()
}

private fun dismissRequest(context: Context, requestId: String) {
    val prefs = context.getSharedPreferences("dismissed_requests_prefs", Context.MODE_PRIVATE)
    val set = (prefs.getStringSet("dismissed_requests", emptySet()) ?: emptySet()).toMutableSet()
    set.add(requestId)
    val trimmed = if (set.size > 100) set.toList().takeLast(100).toSet() else set
    prefs.edit().putStringSet("dismissed_requests", trimmed).apply()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodRequestsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BloodRequestsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var dismissedIds by remember { mutableStateOf(getDismissedRequests(context)) }

    val rawRequests = viewModel.requests
    val requests = remember(rawRequests, dismissedIds) {
        rawRequests.filter { it.id !in dismissedIds && it.requestId !in dismissedIds }
    }

    val isLoading = viewModel.isLoading
    val isSubmitting = viewModel.isSubmitting
    val errorMessage = viewModel.errorMessage
    val currentUserProfile = viewModel.currentUserProfile

    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var unitsNeeded by remember { mutableStateOf("1") }
    var hospital by remember { mutableStateOf("") }
    var cityLocality by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var patientReference by remember { mutableStateOf("") }
    var selectedUrgency by remember { mutableStateOf(Urgency.URGENT.displayName) }
    var additionalNotes by remember { mutableStateOf("") }

    var bloodGroupError by remember { mutableStateOf<String?>(null) }
    var unitsError by remember { mutableStateOf<String?>(null) }
    var hospitalError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }
    var contactNumberError by remember { mutableStateOf<String?>(null) }
    var patientReferenceError by remember { mutableStateOf<String?>(null) }
    var urgencyError by remember { mutableStateOf<String?>(null) }

    var pendingRequest by remember { mutableStateOf<PendingBloodRequest?>(null) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }

    fun validateAndSubmit() {
        val trimmedHospital = hospital.trim()
        val trimmedCity = cityLocality.trim()
        val trimmedUnits = unitsNeeded.trim()
        val trimmedNotes = additionalNotes.trim()
        val trimmedContact = contactNumber.trim()
        val trimmedPatientRef = patientReference.trim()

        val bgErr = BloodRequestValidator.validateBloodGroup(selectedBloodGroup)
        val unitsErr = BloodRequestValidator.validateUnits(trimmedUnits)
        val hospErr = BloodRequestValidator.validateHospital(trimmedHospital)
        val cityErr = BloodRequestValidator.validateCity(trimmedCity)
        val urgErr = BloodRequestValidator.validateUrgency(selectedUrgency)
        val contactErr = BloodRequestValidator.validateContactNumber(trimmedContact)
        val patientRefErr = BloodRequestValidator.validatePatientReference(trimmedPatientRef)

        bloodGroupError = bgErr
        unitsError = unitsErr
        hospitalError = hospErr
        cityError = cityErr
        urgencyError = urgErr
        contactNumberError = contactErr
        patientReferenceError = patientRefErr

        val isValid = bgErr == null && unitsErr == null && hospErr == null && cityErr == null && urgErr == null && contactErr == null && patientRefErr == null
        val parsedUnits = trimmedUnits.toIntOrNull()

        if (isValid && parsedUnits != null) {
            val canonicalUrgency = Urgency.fromStringOrDefault(selectedUrgency).displayName
            pendingRequest = PendingBloodRequest(
                bloodGroup = selectedBloodGroup!!,
                units = parsedUnits,
                hospital = trimmedHospital,
                city = trimmedCity,
                urgency = canonicalUrgency,
                notes = trimmedNotes,
                contactNumber = trimmedContact,
                patientReference = trimmedPatientRef
            )
            showReviewDialog = true
        }
    }

    fun resetForm() {
        selectedBloodGroup = null
        unitsNeeded = "1"
        hospital = ""
        cityLocality = ""
        contactNumber = ""
        patientReference = ""
        selectedUrgency = Urgency.URGENT.displayName
        additionalNotes = ""
        bloodGroupError = null
        unitsError = null
        hospitalError = null
        cityError = null
        urgencyError = null
        contactNumberError = null
        patientReferenceError = null
        pendingRequest = null
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Request Blood",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
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
            // Header card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "Broadcast Emergency Need",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fill in the details below to broadcast live to volunteer donors across the network.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // New Request Form Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "New Blood Request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Blood Group selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                val isSelected = selectedBloodGroup == group
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedBloodGroup = group
                                        bloodGroupError = null
                                    },
                                    label = {
                                        Text(
                                            text = group,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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

                    // Units needed
                    BloodConnectTextField(
                        value = unitsNeeded,
                        onValueChange = {
                            unitsNeeded = it
                            unitsError = null
                        },
                        label = "Units Needed (1 to 20) *",
                        placeholder = "e.g., 1, 2, 3",
                        isError = unitsError != null,
                        errorMessage = unitsError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Hospital
                    BloodConnectTextField(
                        value = hospital,
                        onValueChange = {
                            hospital = it
                            hospitalError = null
                        },
                        label = "Hospital / Medical Facility *",
                        placeholder = "e.g., City General Hospital",
                        leadingIcon = Icons.Default.LocalHospital,
                        isError = hospitalError != null,
                        errorMessage = hospitalError
                    )

                    // City / Locality
                    BloodConnectTextField(
                        value = cityLocality,
                        onValueChange = {
                            cityLocality = it
                            cityError = null
                        },
                        label = "City / Locality *",
                        placeholder = "e.g., Central Sector, Downtown",
                        leadingIcon = Icons.Default.LocationCity,
                        isError = cityError != null,
                        errorMessage = cityError
                    )

                    // Hospital contact number
                    BloodConnectTextField(
                        value = contactNumber,
                        onValueChange = {
                            contactNumber = it
                            contactNumberError = null
                        },
                        label = "Hospital / ward contact number *",
                        placeholder = "e.g., +91 98765 43210 or 022-2345-6789",
                        leadingIcon = Icons.Default.Call,
                        isError = contactNumberError != null,
                        errorMessage = contactNumberError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    // Doctor / patient reference
                    BloodConnectTextField(
                        value = patientReference,
                        onValueChange = {
                            patientReference = it
                            patientReferenceError = null
                        },
                        label = "Doctor name or patient ID (Optional)",
                        placeholder = "e.g., Dr. Sharma / Case #402",
                        isError = patientReferenceError != null,
                        errorMessage = patientReferenceError
                    )

                    // Urgency Level selection
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                val chipUrgency = Urgency.fromString(level)
                                val isSelected = selectedUrgency == level
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedUrgency = level
                                        urgencyError = null
                                    },
                                    label = {
                                        Text(
                                            text = level,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        selectedContainerColor = when (chipUrgency) {
                                            Urgency.CRITICAL -> MaterialTheme.colorScheme.primary
                                            Urgency.URGENT -> MaterialTheme.colorScheme.primaryContainer
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        selectedLabelColor = when (chipUrgency) {
                                            Urgency.CRITICAL -> MaterialTheme.colorScheme.onPrimary
                                            Urgency.URGENT -> MaterialTheme.colorScheme.onPrimaryContainer
                                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                                        }
                                    ),
                                    border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                        }

                        if (urgencyError != null) {
                            Text(
                                text = urgencyError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }

                    // Notes
                    BloodConnectTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = "Additional Notes (Optional)",
                        placeholder = "e.g., Patient Bed #12, Blood bank contact desk",
                        singleLine = false
                    )

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BloodConnectButton(
                            text = if (isSubmitting) "Broadcasting..." else "Review Request",
                            icon = Icons.Default.AddAlert,
                            onClick = { validateAndSubmit() },
                            enabled = !isSubmitting,
                            modifier = Modifier.weight(1f)
                        )

                        BloodConnectOutlinedButton(
                            text = "Reset",
                            onClick = { resetForm() },
                            enabled = !isSubmitting,
                            modifier = Modifier.width(100.dp)
                        )
                    }

                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }

                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Active Requests Section
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionHeader(
                    title = "Active Blood Requests",
                    subtitle = "Live emergency broadcasts from the network"
                )

                if (isLoading && requests.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Loading active requests...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (requests.isEmpty()) {
                    EmptyState(
                        title = "No Active Requests",
                        description = "There are currently no active emergency blood requests. Use the form above to broadcast a new request.",
                        icon = Icons.Default.Info,
                        actionLabel = "Refresh",
                        onActionClick = { viewModel.loadRequests() }
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        requests.forEach { request ->
                            LiveRequestCard(
                                request = request,
                                currentUser = currentUserProfile,
                                viewModel = viewModel,
                                onDismiss = {
                                    val reqId = request.id.ifBlank { request.requestId }
                                    dismissRequest(context, reqId)
                                    dismissedIds = getDismissedRequests(context)
                                },
                                onShowSnackbar = { msg ->
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showReviewDialog && pendingRequest != null) {
        val req = pendingRequest!!
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Review Blood Request",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Please verify your emergency request details before broadcasting:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.background
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                BloodGroupBadge(
                                    bloodGroup = req.bloodGroup,
                                    size = 44.dp
                                )
                                Column {
                                    Text(
                                        text = "${req.bloodGroup} Blood Required",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${req.units} Unit(s) Needed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )

                            ReviewDetailRow(label = "Hospital", value = req.hospital)
                            ReviewDetailRow(label = "Contact Number", value = req.contactNumber)
                            ReviewDetailRow(
                                label = "Doctor / Patient ID",
                                value = req.patientReference.ifBlank { "Not specified" }
                            )
                            ReviewDetailRow(label = "Locality", value = req.city)
                            ReviewDetailRow(label = "Urgency", value = req.urgency)
                            if (req.notes.isNotBlank()) {
                                ReviewDetailRow(label = "Notes", value = req.notes)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showReviewDialog = false
                        viewModel.createRequestWithLocation(
                            bloodGroup = req.bloodGroup,
                            hospital = req.hospital,
                            unitsNeeded = req.units,
                            urgency = req.urgency,
                            city = req.city,
                            notes = req.notes,
                            contactNumber = req.contactNumber,
                            patientReference = req.patientReference,
                            onSuccess = {
                                resetForm()
                                showConfirmationDialog = true
                            }
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Broadcast Request", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReviewDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Back to Edit")
                }
            }
        )
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Request Broadcasted",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Your emergency blood request has been successfully broadcasted and is now visible on the active requests feed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirmationDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

@Composable
private fun ReviewDetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LiveRequestCard(
    request: BloodRequest,
    currentUser: User?,
    viewModel: BloodRequestsViewModel,
    onDismiss: () -> Unit,
    onShowSnackbar: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showPledgeConfirmDialog by remember { mutableStateOf(false) }
    var selectedEtaMinutes by remember { mutableStateOf(30) } // default 30 min
    var showMenu by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    val myPledgeState = remember(request.id) {
        viewModel.observeMyPledge(request.id)
    }?.collectAsState(initial = null)

    val myPledge = myPledgeState?.value

    val userBloodGroup = currentUser?.bloodGroup?.let { BloodGroup.fromString(it) }
    val requestBloodGroup = BloodGroup.fromString(request.bloodGroup)
    val isEligibleToDonate = DonationEligibility.isEligible(currentUser?.lastDonationAt)

    val canPledge = currentUser != null &&
            request.requesterId.isNotBlank() &&
            currentUser.uid != request.requesterId &&
            isEligibleToDonate &&
            userBloodGroup != null &&
            requestBloodGroup != null &&
            userBloodGroup.canDonateTo(requestBloodGroup)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BloodGroupBadge(
                    bloodGroup = request.bloodGroup,
                    size = 50.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = request.hospital,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (request.verified) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = "Verified",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFFE8F5E9),
                                labelColor = Color(0xFF2E7D32)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else {
                        Text(
                            text = "Not yet verified",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Text(
                        text = "${request.city} · Requested by ${request.requesterName.ifBlank { "Anonymous" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "${request.unitsNeeded} Unit(s) Needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (request.notes.isNotBlank()) {
                        Text(
                            text = "Notes: ${request.notes}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val chipUrgency = request.urgencyLevel
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = chipUrgency.displayName,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = when (chipUrgency) {
                                Urgency.CRITICAL -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                                Urgency.URGENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                Urgency.ROUTINE -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            },
                            labelColor = when (chipUrgency) {
                                Urgency.CRITICAL -> MaterialTheme.colorScheme.error
                                Urgency.URGENT -> MaterialTheme.colorScheme.primary
                                Urgency.ROUTINE -> MaterialTheme.colorScheme.secondary
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    IconButton(
                        onClick = { IntentHelpers.shareRequest(context, request) },
                        modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share request",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Open menu",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Report request") },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Block requester") },
                                onClick = {
                                    showMenu = false
                                    viewModel.blockRequester(
                                        targetUid = request.requesterId,
                                        targetName = request.requesterName,
                                        onSuccess = {
                                            onShowSnackbar("Blocked ${request.requesterName.ifBlank { "requester" }}")
                                        }
                                    )
                                }
                            )
                        }
                    }
                }
            }

            if (request.contactNumber.isNotBlank()) {
                OutlinedButton(
                    onClick = {
                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${request.contactNumber}"))
                        context.startActivity(dialIntent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Call hospital", fontWeight = FontWeight.Medium)
                }
            }

            OutlinedButton(
                onClick = {
                    IntentHelpers.openDirections(
                        context = context,
                        latitude = request.latitude,
                        longitude = request.longitude,
                        hospital = request.hospital,
                        city = request.city
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Get directions to hospital",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Directions", fontWeight = FontWeight.Medium)
            }

            if (canPledge) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

                if (myPledge == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showPledgeConfirmDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("I can donate", fontWeight = FontWeight.SemiBold)
                        }

                        TextButton(onClick = onDismiss) {
                            Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.withdrawPledge(request.id) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Can't make it", fontWeight = FontWeight.Medium)
                        }

                        TextButton(onClick = onDismiss) {
                            Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    if (showPledgeConfirmDialog) {
        val isContactable = currentUser?.contactable == true
        val dialogMessage = if (isContactable) {
            "Share your name and phone number with the requester ONLY for this request?"
        } else {
            "Your phone will not be shared. The requester will see only your name and blood group."
        }

        AlertDialog(
            onDismissRequest = { showPledgeConfirmDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    text = "Pledge Blood Donation",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = dialogMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "I can reach in:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        etaOptions.forEach { mins ->
                            val label = if (mins < 60) "$mins min" else "${mins / 60} hour(s)"
                            val isSelected = selectedEtaMinutes == mins
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedEtaMinutes = mins },
                                label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPledgeConfirmDialog = false
                        viewModel.pledgeToRequest(request, etaMinutes = selectedEtaMinutes)
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPledgeConfirmDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReportDialog) {
        ReportDialog(
            title = "Report request",
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, details ->
                viewModel.submitReport(
                    targetType = "request",
                    targetId = request.id,
                    requestId = request.id,
                    reason = reason,
                    details = details,
                    onSuccess = {
                        onShowSnackbar("Thanks, we will review it")
                    }
                )
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BloodRequestsScreenPreview() {
    BloodConnectTheme {
        BloodRequestsScreen(onNavigateBack = {})
    }
}
