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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.data.model.BloodRequest
import com.bloodconnect.data.model.RequestStatus
import com.bloodconnect.data.model.Urgency
import com.bloodconnect.ui.components.BloodGroupBadge
import com.bloodconnect.ui.components.EmptyState
import com.bloodconnect.ui.components.SectionHeader
import com.bloodconnect.ui.theme.BloodConnectTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val statusFilterOptions = listOf("All") + RequestStatus.entries.map { it.displayName }
private val urgencyFilterOptions = listOf("All") + Urgency.entries.map { it.displayName }

/**
 * Screen displaying the user's historical blood requests from Firestore.
 * Allows users to review request statuses, mark pending requests as fulfilled, or cancel them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyRequestsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyRequestsViewModel = hiltViewModel()
) {
    val requests = viewModel.requests
    val isLoading = viewModel.isLoading
    val errorMessage = viewModel.errorMessage

    var selectedFilter by remember { mutableStateOf("All") }
    var selectedUrgencyFilter by remember { mutableStateOf("All") }
    var selectedRequestForDetails by remember { mutableStateOf<BloodRequest?>(null) }

    val displayedRequests = remember(requests, selectedFilter, selectedUrgencyFilter) {
        requests.filter { req ->
            val statusMatches = if (selectedFilter == "All") {
                true
            } else {
                val filterStatus = RequestStatus.fromString(selectedFilter)
                req.requestStatus == filterStatus || req.status.startsWith(selectedFilter, ignoreCase = true)
            }

            val urgencyMatches = if (selectedUrgencyFilter == "All") {
                true
            } else {
                val filterUrgency = Urgency.fromString(selectedUrgencyFilter)
                req.urgencyLevel == filterUrgency || req.urgency.equals(selectedUrgencyFilter, ignoreCase = true)
            }

            statusMatches && urgencyMatches
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Requests",
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Screen Introduction
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Request History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Review your past emergency blood requests and manage their statuses.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status Filter Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Filter by Status",
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
                        statusFilterOptions.forEach { filterOption ->
                            FilterChip(
                                selected = selectedFilter == filterOption,
                                onClick = { selectedFilter = filterOption },
                                label = { Text(filterOption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "Filter by Urgency",
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
                        urgencyFilterOptions.forEach { urgencyOption ->
                            FilterChip(
                                selected = selectedUrgencyFilter == urgencyOption,
                                onClick = { selectedUrgencyFilter = urgencyOption },
                                label = { Text(urgencyOption) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                                )
                            )
                        }
                    }
                }
            }

            // Requests List Header
            SectionHeader(
                title = "My Requests",
                subtitle = if (displayedRequests.isNotEmpty()) {
                    "Showing ${displayedRequests.size} request record(s)"
                } else {
                    "No matching records found"
                }
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
                    Text("Loading your requests...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (displayedRequests.isEmpty()) {
                EmptyState(
                    title = "No Requests Found",
                    description = "You have not submitted any emergency blood requests matching the current filters.",
                    icon = Icons.Default.Inbox,
                    actionLabel = "Refresh",
                    onActionClick = { viewModel.loadMyRequests() }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    displayedRequests.forEach { request ->
                        MyRequestCard(
                            entry = request,
                            onClick = { selectedRequestForDetails = request }
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Request Detail Dialog with Actions
    if (selectedRequestForDetails != null) {
        val req = selectedRequestForDetails!!
        val dateFormatted = remember(req.createdAt) {
            SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()).format(Date(req.createdAt))
        }

        AlertDialog(
            onDismissRequest = { selectedRequestForDetails = null },
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
                    text = "Request Details",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
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
                                            text = "${req.bloodGroup} Blood Request",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${req.unitsNeeded} Unit(s) Needed",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                val statusEnum = req.requestStatus
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(statusEnum.displayName) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = when (statusEnum) {
                                            RequestStatus.PENDING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                            RequestStatus.FULFILLED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            RequestStatus.CANCELLED -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                                        },
                                        labelColor = when (statusEnum) {
                                            RequestStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                                            RequestStatus.FULFILLED -> MaterialTheme.colorScheme.primary
                                            RequestStatus.CANCELLED -> MaterialTheme.colorScheme.error
                                        }
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            RequestDetailRow(label = "Request ID", value = req.id.ifBlank { req.requestId })
                            RequestDetailRow(label = "Date & Time", value = dateFormatted)
                            RequestDetailRow(label = "Hospital", value = req.hospital)
                            RequestDetailRow(label = "Locality", value = req.city)
                            RequestDetailRow(label = "Urgency", value = req.urgencyLevel.displayName)
                        }
                    }

                    // Action buttons if Pending
                    if (req.requestStatus == RequestStatus.PENDING) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateStatus(req.id.ifBlank { req.requestId }, RequestStatus.FULFILLED)
                                    selectedRequestForDetails = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Mark Fulfilled")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.updateStatus(req.id.ifBlank { req.requestId }, RequestStatus.CANCELLED)
                                    selectedRequestForDetails = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(imageVector = Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.size(4.dp))
                                Text("Cancel")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedRequestForDetails = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * Card representing an individual blood request in history.
 */
@Composable
private fun MyRequestCard(
    entry: BloodRequest,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatted = remember(entry.createdAt) {
        SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault()).format(Date(entry.createdAt))
    }

    OutlinedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BloodGroupBadge(
                    bloodGroup = entry.bloodGroup,
                    size = 46.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = entry.hospital,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationCity,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = entry.city.ifBlank { "Unspecified Location" },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "${entry.unitsNeeded} Unit(s) Needed · ${entry.urgencyLevel.displayName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                val statusEnum = entry.requestStatus
                SuggestionChip(
                    onClick = onClick,
                    label = { Text(statusEnum.displayName) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = when (statusEnum) {
                            RequestStatus.PENDING -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            RequestStatus.FULFILLED -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            RequestStatus.CANCELLED -> MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                        },
                        labelColor = when (statusEnum) {
                            RequestStatus.PENDING -> MaterialTheme.colorScheme.tertiary
                            RequestStatus.FULFILLED -> MaterialTheme.colorScheme.primary
                            RequestStatus.CANCELLED -> MaterialTheme.colorScheme.error
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = dateFormatted,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Tap to view & manage",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RequestDetailRow(
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

@Preview(showBackground = true)
@Composable
private fun MyRequestsScreenPreview() {
    BloodConnectTheme {
        MyRequestsScreen(onNavigateBack = {})
    }
}
