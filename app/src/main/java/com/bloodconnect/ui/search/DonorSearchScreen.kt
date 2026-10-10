package com.bloodconnect.ui.search

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bloodconnect.ui.components.BloodConnectButton
import com.bloodconnect.ui.components.BloodConnectTextField
import com.bloodconnect.ui.components.BloodGroupBadge
import com.bloodconnect.ui.components.EmptyState
import com.bloodconnect.ui.components.SectionHeader
import com.bloodconnect.ui.theme.BloodConnectTheme

/**
 * Sorting orders available for distance filtering.
 */
enum class DistanceSortOrder {
    NEAREST,
    FARTHEST
}

private val bloodGroupsList = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
private val distanceOptions = listOf(5, 10, 25, 50)

/**
 * Screen for searching volunteer blood donors by blood type, location, and radius.
 * Powered by local Compose state with sample demonstration records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonorSearchScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDonorDetail: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Local filter and sort states
    var selectedBloodGroup by remember { mutableStateOf<String?>(null) }
    var locationInput by remember { mutableStateOf("") }
    var selectedDistanceKm by remember { mutableIntStateOf(50) }
    var sortOrder by remember { mutableStateOf(DistanceSortOrder.NEAREST) }
    var displayedDonors by remember { mutableStateOf(defaultSampleDonors) }

    fun applyFilters(
        group: String? = selectedBloodGroup,
        location: String = locationInput,
        distance: Int = selectedDistanceKm,
        sort: DistanceSortOrder = sortOrder
    ) {
        val filtered = defaultSampleDonors.filter { donor ->
            val matchesBlood = group == null || donor.bloodGroup.equals(group, ignoreCase = true)
            val matchesLocation = location.isBlank() || donor.location.contains(location.trim(), ignoreCase = true)
            val matchesDistance = donor.distanceKm <= distance
            matchesBlood && matchesLocation && matchesDistance
        }
        displayedDonors = when (sort) {
            DistanceSortOrder.NEAREST -> filtered.sortedBy { it.distanceKm }
            DistanceSortOrder.FARTHEST -> filtered.sortedByDescending { it.distanceKm }
        }
    }

    fun clearFilters() {
        selectedBloodGroup = null
        locationInput = ""
        selectedDistanceKm = 50
        sortOrder = DistanceSortOrder.NEAREST
        displayedDonors = defaultSampleDonors.sortedBy { it.distanceKm }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Find Donors",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Dashboard",
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
            // Header Description
            Text(
                text = "Search Volunteer Blood Donors",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Filter available sample donors by blood group, locality, and search radius.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Search Filters Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Blood Group Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Blood Group",
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
                            FilterChip(
                                selected = selectedBloodGroup == null,
                                onClick = {
                                    selectedBloodGroup = null
                                    applyFilters(group = null)
                                },
                                label = { Text("All") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )

                            bloodGroupsList.forEach { group ->
                                FilterChip(
                                    selected = selectedBloodGroup == group,
                                    onClick = {
                                        val newGroup = if (selectedBloodGroup == group) null else group
                                        selectedBloodGroup = newGroup
                                        applyFilters(group = newGroup)
                                    },
                                    label = { Text(group) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Location Input
                    BloodConnectTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it },
                        label = "Location / Locality",
                        placeholder = "e.g., Central, Metro, Eastside",
                        leadingIcon = Icons.Default.Place,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { applyFilters() })
                    )

                    // Distance Radius Selector
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Maximum Radius: ${selectedDistanceKm} km",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            distanceOptions.forEach { radius ->
                                FilterChip(
                                    selected = selectedDistanceKm == radius,
                                    onClick = {
                                        selectedDistanceKm = radius
                                        applyFilters(distance = radius)
                                    },
                                    label = { Text("${radius} km") },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }

                    // Filter Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BloodConnectButton(
                            text = "Search Donors",
                            icon = Icons.Default.Search,
                            onClick = { applyFilters() },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedButton(
                            onClick = { clearFilters() },
                            modifier = Modifier.height(50.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }

            // Results Section Header
            SectionHeader(
                title = "Donor Results (Sample)",
                subtitle = if (displayedDonors.isNotEmpty()) {
                    "Found ${displayedDonors.size} sample matching donor(s)"
                } else {
                    "No results found"
                }
            )

            // Distance Sorting Control
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Options",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sort:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sortOrder == DistanceSortOrder.NEAREST,
                            onClick = {
                                sortOrder = DistanceSortOrder.NEAREST
                                applyFilters(sort = DistanceSortOrder.NEAREST)
                            },
                            label = { Text("Nearest first", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )

                        FilterChip(
                            selected = sortOrder == DistanceSortOrder.FARTHEST,
                            onClick = {
                                sortOrder = DistanceSortOrder.FARTHEST
                                applyFilters(sort = DistanceSortOrder.FARTHEST)
                            },
                            label = { Text("Farthest first", style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            if (displayedDonors.isEmpty()) {
                EmptyState(
                    title = "No Matching Donors",
                    description = "No sample donors match your current filter combination. Try expanding the search radius or resetting the filters.",
                    icon = Icons.Default.SearchOff,
                    actionLabel = "Reset Filters",
                    onActionClick = { clearFilters() }
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayedDonors.forEach { donor ->
                        DonorResultCard(
                            donor = donor,
                            onViewDetails = { onNavigateToDonorDetail(donor.id) }
                        )
                    }
                }
            }

            // Demo Disclosure
            Text(
                text = "* Demonstration data only. No real personal records, phone numbers, or addresses are stored.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 12.dp)
            )
        }
    }
}

/**
 * Result card displaying individual sample donor information.
 */
@Composable
private fun DonorResultCard(
    donor: SampleDonor,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onViewDetails,
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
                    bloodGroup = donor.bloodGroup,
                    size = 48.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = donor.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = donor.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = "Approx. ${donor.distanceKm} km away",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                SuggestionChip(
                    onClick = onViewDetails,
                    label = { Text(donor.status) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                        labelColor = MaterialTheme.colorScheme.tertiary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sample Volunteer Record",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                TextButton(
                    onClick = onViewDetails,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = "View Details",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View Details for ${donor.label}",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DonorSearchScreenPreview() {
    BloodConnectTheme {
        DonorSearchScreen(
            onNavigateBack = {},
            onNavigateToDonorDetail = {}
        )
    }
}
