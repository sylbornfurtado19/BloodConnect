package com.bloodconnect.ui.request

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Directions
import com.bloodconnect.utils.IntentHelpers
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.MoreVert
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
import com.bloodconnect.ui.components.BloodConnectTextField
import com.bloodconnect.ui.components.BloodGroupBadge
import com.bloodconnect.ui.components.EmptyState
import com.bloodconnect.ui.components.ReportDialog
import com.bloodconnect.ui.components.SectionHeader
import com.bloodconnect.ui.theme.BloodConnectTheme
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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Request Emergency Blood",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Fill in the emergency request details below to broadcast live to registered donors.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

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
                                val chipUrgency = Urgency.fromString(level)
                                FilterChip(
                                    selected = selectedUrgency == level,
                                    onClick = {
                                        selectedUrgency = level
                                        urgencyError = null
                                    },
                                    label = { Text(level) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
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
                                    )
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

                    BloodConnectTextField(
                        value = additionalNotes,
                        onValueChange = { additionalNotes = it },
                        label = "Additional Notes (Optional)",
                        placeholder = "e.g., Patient Bed #12, Blood bank contact desk",
                        singleLine = false
                    )

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

                        OutlinedButton(
                            onClick = { resetForm() },
                            enabled = !isSubmitting,
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Reset")
                        }
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
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading active requests...", style = MaterialTheme.typography.bodyMedium)
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            icon = {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
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
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                BloodGroupBadge(
                                    bloodGroup = req.bloodGroup,
                                    size = 40.dp
                                )
                                Column {
                                    Text(
                                        text = "${req.bloodGroup} Blood Required",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${req.units} Unit(s) Needed",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

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
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Broadcast Request")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showReviewDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Back to Edit")
                }
            }
        )
    }

    if (showConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmationDialog = false },
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
                    text = "Request Broadcasted",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Text(
                    text = "Your emergency blood request has been successfully broadcasted to Firestore and is now visible on the active requests feed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirmationDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Done")
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

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                BloodGroupBadge(
                    bloodGroup = request.bloodGroup,
                    size = 48.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = request.hospital,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (request.verified) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Verified", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall) },
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
                        fontWeight = FontWeight.Medium
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
                        label = { Text(chipUrgency.displayName) },
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

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Call hospital")
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
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Get directions to hospital",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text("Directions")
            }

            if (canPledge) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (myPledge == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showPledgeConfirmDialog = true },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.padding(start = 6.dp))
                            Text("I can donate")
                        }

                        TextButton(onClick = onDismiss) {
                            Text("Not now", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.withdrawPledge(request.id) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Can't make it")
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
            title = { Text("Pledge Blood Donation") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(dialogMessage)

                    Text(
                        text = "I can reach in:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        etaOptions.forEach { mins ->
                            val label = if (mins < 60) "$mins min" else "${mins / 60} hour(s)"
                            FilterChip(
                                selected = selectedEtaMinutes == mins,
                                onClick = { selectedEtaMinutes = mins },
                                label = { Text(label) }
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
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showPledgeConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
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
