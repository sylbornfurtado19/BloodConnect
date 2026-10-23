package com.bloodconnect.ui.search

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.data.model.DonationEligibility
import com.bloodconnect.ui.components.BloodConnectOutlinedButton
import com.bloodconnect.ui.components.BloodGroupBadge
import com.bloodconnect.ui.components.EmptyState
import com.bloodconnect.ui.components.SectionHeader
import com.bloodconnect.ui.theme.BloodConnectTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen displaying detailed information for an individual volunteer donor.
 * Redesigned in Phase 3 with coral red, pure white, and soft-pink styling.
 * Resolves the donor by ID using [DonorDetailViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorDetailScreen(
    donorId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DonorDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                TopAppBar(
                    title = {
                        val titleText = if (uiState is DonorDetailUiState.Success) {
                            val d = (uiState as DonorDetailUiState.Success).donor
                            if (d.name.isNotBlank()) d.name else "Donor Details"
                        } else {
                            "Donor Details"
                        }
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleLarge,
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
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (uiState) {
            is DonorDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            is DonorDetailUiState.Error -> {
                val errorMessage = (uiState as DonorDetailUiState.Error).message
                EmptyState(
                    title = "Donor Not Found",
                    description = errorMessage,
                    icon = Icons.Default.PersonOff,
                    actionLabel = "Back to Donor Search",
                    onActionClick = onNavigateBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            is DonorDetailUiState.Success -> {
                val donor = (uiState as DonorDetailUiState.Success).donor
                val isEligible = DonationEligibility.isEligible(donor.lastDonationAt)
                val eligibilityStatus = if (isEligible) "Eligible" else "Ineligible"
                val status = if (donor.isAvailable && isEligible) "Active" else "Unavailable"
                val locality = donor.locality.ifBlank { "Unknown Location" }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // 1. Donor Overview Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            BloodGroupBadge(
                                bloodGroup = donor.bloodGroup.displayName,
                                size = 68.dp
                            )

                            Text(
                                text = donor.name.ifBlank { "Donor ${donor.id.take(4)}" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (status == "Active") MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = if (status == "Active") MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                ) {
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isEligible) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                    contentColor = if (isEligible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                ) {
                                    Text(
                                        text = eligibilityStatus,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Donor Location & Proximity Section
                    SectionHeader(
                        title = "Location & Availability",
                        subtitle = "Proximity and donation eligibility status"
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            DetailInfoRow(
                                label = "Locality",
                                value = locality,
                                icon = Icons.Default.Place
                            )

                            DetailInfoRow(
                                label = "Last Donation",
                                value = if (donor.lastDonationAt != null && donor.lastDonationAt > 0) {
                                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                                    sdf.format(Date(donor.lastDonationAt))
                                } else {
                                    "No recent donation record"
                                },
                                icon = Icons.Default.Event
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Back Button Action
                    BloodConnectOutlinedButton(
                        text = "Back to Donor Search",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onNavigateBack,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun DetailInfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
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
}

@Preview(showBackground = true)
@Composable
fun DonorDetailScreenPreview() {
    BloodConnectTheme {
        DonorDetailScreen(
            donorId = "sample_id",
            onNavigateBack = {}
        )
    }
}
