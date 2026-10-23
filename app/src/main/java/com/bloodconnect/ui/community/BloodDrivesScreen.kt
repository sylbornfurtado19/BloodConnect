package com.bloodconnect.ui.community

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.data.model.BloodDrive
import com.bloodconnect.ui.components.EmptyState
import com.bloodconnect.utils.IntentHelpers
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodDrivesScreen(
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: BloodDrivesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState = viewModel.uiState
    var showScheduleDialog by remember { mutableStateOf(false) }

    if (showScheduleDialog) {
        ScheduleBloodDriveDialog(
            defaultOrganizer = uiState.currentHospitalName,
            onDismiss = { showScheduleDialog = false },
            onConfirm = { title, org, address, city, startAt, endAt, contact, desc ->
                viewModel.scheduleDrive(
                    title = title,
                    organizer = org,
                    address = address,
                    city = city,
                    startAt = startAt,
                    endAt = endAt,
                    contactNumber = contact,
                    description = desc,
                    onSuccess = {
                        showScheduleDialog = false
                        Toast.makeText(context, "Blood drive scheduled successfully!", Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    }
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Blood Drives",
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
                actions = {
                    if (uiState.isHospitalAdmin) {
                        IconButton(onClick = { showScheduleDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Schedule Blood Drive",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (uiState.isHospitalAdmin) {
                ExtendedFloatingActionButton(
                    onClick = { showScheduleDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Schedule Drive") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                // Loading state
                uiState.isLoading && uiState.drives.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Error state
                uiState.errorMessage != null && uiState.drives.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Failed to load blood drives",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { viewModel.loadDrives() }) {
                            Text("Retry")
                        }
                    }
                }

                // Empty state
                !uiState.isLoading && uiState.drives.isEmpty() -> {
                    EmptyState(
                        title = "No upcoming drives",
                        description = if (uiState.isHospitalAdmin)
                            "No blood donation drives scheduled. As a hospital authority, you can schedule and publish upcoming camps for donors."
                        else
                            "There are no upcoming blood donation drives scheduled at this time. Please check back later.",
                        icon = Icons.Default.EventBusy,
                        actionLabel = if (uiState.isHospitalAdmin) "Schedule a Drive" else null,
                        onActionClick = if (uiState.isHospitalAdmin) { { showScheduleDialog = true } } else null
                    )
                }

                // Content list
                else -> {
                    var rsvpConfirmDriveId by remember { mutableStateOf<String?>(null) }
                    
                    if (rsvpConfirmDriveId != null) {
                        AlertDialog(
                            onDismissRequest = { rsvpConfirmDriveId = null },
                            title = { Text("Confirm Attendance") },
                            text = { 
                                Text(
                                    "By confirming your attendance, your name, blood group, and phone number will be securely shared with the hospital organizing this drive for their registration guest list."
                                ) 
                            },
                            confirmButton = {
                                Button(
                                    onClick = { 
                                        viewModel.toggleRsvp(rsvpConfirmDriveId!!)
                                        rsvpConfirmDriveId = null
                                    }
                                ) {
                                    Text("Confirm")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { rsvpConfirmDriveId = null }) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.drives, key = { it.id }) { drive ->
                            val isGoing = uiState.rsvpStatus[drive.id] ?: false
                            BloodDriveCard(
                                drive = drive,
                                isGoing = isGoing,
                                rsvpCount = uiState.rsvpCounts[drive.id] ?: 0,
                                canDelete = uiState.isHospitalAdmin,
                                onDelete = { viewModel.deleteDrive(drive.id) },
                                onToggleRsvp = { 
                                    if (!isGoing) {
                                        rsvpConfirmDriveId = drive.id
                                    } else {
                                        viewModel.toggleRsvp(drive.id)
                                    }
                                },
                                onCall = {
                                    IntentHelpers.dialNumber(context, drive.contactNumber)
                                },
                                onDirections = {
                                    IntentHelpers.openDirections(
                                        context = context,
                                        latitude = drive.latitude,
                                        longitude = drive.longitude,
                                        hospital = drive.title,
                                        city = drive.city
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BloodDriveCard(
    drive: BloodDrive,
    isGoing: Boolean,
    rsvpCount: Int,
    onToggleRsvp: () -> Unit,
    onCall: () -> Unit,
    onDirections: () -> Unit,
    modifier: Modifier = Modifier,
    canDelete: Boolean = false,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title, Organizer, and Admin Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = drive.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (drive.organizer.isNotBlank()) {
                        Text(
                            text = "Organized by ${drive.organizer}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (canDelete) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Drive",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Date / Time Range
            val dateRangeStr = formatDriveDateTime(drive.startAt, drive.endAt)
            if (dateRangeStr.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = dateRangeStr,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Address and City
            val fullAddress = listOf(drive.address.trim(), drive.city.trim())
                .filter { it.isNotEmpty() }
                .joinToString(", ")
            if (fullAddress.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = fullAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Description if present
            if (drive.description.isNotBlank()) {
                Text(
                    text = drive.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // RSVP Status & Toggle Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$rsvpCount going",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (isGoing) {
                    Button(
                        onClick = onToggleRsvp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "I'm going")
                    }
                } else {
                    OutlinedButton(
                        onClick = onToggleRsvp,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "I'm going")
                    }
                }
            }

            // Action Buttons: Call & Directions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCall,
                    enabled = drive.contactNumber.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Call")
                }

                Button(
                    onClick = onDirections,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Directions")
                }
            }
        }
    }
}

/**
 * Formats start and end epoch milliseconds into human readable date/time range.
 */
fun formatDriveDateTime(startAt: Long, endAt: Long): String {
    if (startAt <= 0L) return ""
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    val startDate = dateFormat.format(Date(startAt))
    val startTime = timeFormat.format(Date(startAt))

    return if (endAt > 0L) {
        val endDate = dateFormat.format(Date(endAt))
        val endTime = timeFormat.format(Date(endAt))
        if (startDate == endDate) {
            "$startDate, $startTime - $endTime"
        } else {
            "$startDate $startTime - $endDate $endTime"
        }
    } else {
        "$startDate, $startTime"
    }
}

@Composable
fun ScheduleBloodDriveDialog(
    defaultOrganizer: String,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        organizer: String,
        address: String,
        city: String,
        startAt: Long,
        endAt: Long,
        contactNumber: String,
        description: String
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var organizer by remember { mutableStateOf(defaultOrganizer) }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Mumbai") }
    var contactNumber by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var daysInFuture by remember { mutableStateOf("1") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule Blood Drive",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Publish an upcoming blood donation camp for voluntary donors.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Drive / Camp Name *") },
                    placeholder = { Text("e.g. City Hospital Annual Blood Camp") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = organizer,
                    onValueChange = { organizer = it },
                    label = { Text("Hospital / Organizer Name *") },
                    placeholder = { Text("e.g. Lilavati Hospital & Research Centre") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Venue / Address *") },
                    placeholder = { Text("e.g. Ground Floor Auditorium, Bandra West") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = daysInFuture,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) daysInFuture = it },
                    label = { Text("Days from today") },
                    placeholder = { Text("1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Hospital Helpline / Contact Number") },
                    placeholder = { Text("+91 98765 43210") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Details / Eligibility / Refreshments") },
                    placeholder = { Text("e.g. Free health checkup and certificates for all donors.") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = daysInFuture.toIntOrNull() ?: 1
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, days)
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    val startAt = cal.timeInMillis
                    cal.set(Calendar.HOUR_OF_DAY, 17)
                    val endAt = cal.timeInMillis

                    onConfirm(
                        title,
                        organizer,
                        address,
                        city,
                        startAt,
                        endAt,
                        contactNumber,
                        description
                    )
                },
                enabled = title.isNotBlank() && address.isNotBlank() && city.isNotBlank()
            ) {
                Text("Schedule Drive")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
