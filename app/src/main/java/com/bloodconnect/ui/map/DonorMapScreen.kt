package com.bloodconnect.ui.map

import android.Manifest
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.data.model.BloodGroup
import com.bloodconnect.ui.components.EmptyState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private val bloodGroupsFilterList = listOf("Any") + listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

/**
 * Privacy helper rounding latitude or longitude to 2 decimal places (~1 km radius) for donor markers.
 */
fun Double.coarse(): Double = kotlin.math.round(this * 100) / 100

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DonorMapScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DonorMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val permissionGranted = locationPermissionState.allPermissionsGranted

    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.refreshUserLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    val defaultLocation = LatLng(19.0760, 72.8777)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 12f)
    }

    // Camera animation tracking flags
    val hasAnimatedToUser = remember { mutableStateOf(false) }
    val cameraHasMoved = remember { mutableStateOf(false) }

    // Animate camera to user location ONCE when uiState.userLocation becomes non-null
    LaunchedEffect(uiState.userLocation) {
        val loc = uiState.userLocation
        if (loc != null && !hasAnimatedToUser.value) {
            hasAnimatedToUser.value = true
            cameraHasMoved.value = true
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 13f),
                1000
            )
        }
    }

    // Fall back to first marker only if no user location exists and camera has not moved yet
    LaunchedEffect(uiState.donors, uiState.requests) {
        if (!cameraHasMoved.value && uiState.userLocation == null) {
            val firstDonor = uiState.donors.firstOrNull { it.latitude != null && it.longitude != null }
            if (firstDonor != null) {
                cameraHasMoved.value = true
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(LatLng(firstDonor.latitude!!.coarse(), firstDonor.longitude!!.coarse()), 13f),
                    1000
                )
            } else {
                val firstReq = uiState.requests.firstOrNull { it.latitude != 0.0 && it.longitude != 0.0 }
                if (firstReq != null) {
                    cameraHasMoved.value = true
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(LatLng(firstReq.latitude, firstReq.longitude), 13f),
                        1000
                    )
                }
            }
        }
    }

    // SnackbarHost for errorMessage
    val errorMessage = uiState.errorMessage
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    val visibleDonors = if (uiState.filter == MapFilter.ALL || uiState.filter == MapFilter.DONORS) {
        uiState.donors
    } else {
        emptyList()
    }

    val visibleRequests = if (uiState.filter == MapFilter.ALL || uiState.filter == MapFilter.REQUESTS) {
        uiState.requests
    } else {
        emptyList()
    }

    // Log marker counts & coordinates for debugging
    LaunchedEffect(visibleDonors, visibleRequests) {
        Log.d("DonorMapScreen", "Rendering ${visibleDonors.size} donors and ${visibleRequests.size} requests on map.")
        visibleDonors.forEach { d ->
            Log.d("DonorMapScreen", "Donor ${d.name}: lat=${d.latitude}, lon=${d.longitude}")
        }
        visibleRequests.forEach { r ->
            Log.d("DonorMapScreen", "Request ${r.hospital}: lat=${r.latitude}, lon=${r.longitude}")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Map", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = permissionGranted),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = true,
                    myLocationButtonEnabled = permissionGranted,
                    compassEnabled = true,
                    mapToolbarEnabled = true
                )
            ) {
                // Donor Markers
                visibleDonors.forEach { donor ->
                    if (donor.latitude != null && donor.longitude != null) {
                        val coarseLat = donor.latitude.coarse()
                        val coarseLon = donor.longitude.coarse()
                        
                        // Apply a deterministic small jitter (~100-300m) to prevent exact overlapping
                        val jitterLat = (donor.id.hashCode() % 1000) * 0.000005
                        val jitterLon = (donor.id.reversed().hashCode() % 1000) * 0.000005
                        val finalLat = coarseLat + jitterLat
                        val finalLon = coarseLon + jitterLon

                        key(donor.id) {
                            Marker(
                                state = rememberMarkerState(position = LatLng(finalLat, finalLon)),
                                title = "${donor.bloodGroup.displayName} donor",
                                snippet = donor.locality
                            )
                        }
                    }
                }

                // Request Markers
                visibleRequests.forEach { req ->
                    if (req.latitude != 0.0 && req.longitude != 0.0) {
                        val reqId = req.id.ifBlank { req.requestId }

                        // Apply a tiny jitter for requests at the exact same hospital
                        val jitterLat = (reqId.hashCode() % 100) * 0.000002
                        val jitterLon = (reqId.reversed().hashCode() % 100) * 0.000002
                        val finalLat = req.latitude + jitterLat
                        val finalLon = req.longitude + jitterLon

                        key(reqId) {
                            Marker(
                                state = rememberMarkerState(position = LatLng(finalLat, finalLon)),
                                title = "${req.bloodGroup} needed - ${req.hospital}",
                                snippet = "${req.urgency} - ${req.unitsNeeded} unit(s) - ${req.city}"
                            )
                        }
                    }
                }
            }

            // Top Filter Row & Blood Group Chips overlay
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MapFilter.entries.forEach { filter ->
                            FilterChip(
                                selected = uiState.filter == filter,
                                onClick = { viewModel.setFilter(filter) },
                                label = { Text(filter.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        bloodGroupsFilterList.forEach { bgStr ->
                            val isSelected = if (bgStr == "Any") {
                                uiState.bloodGroupFilter == null
                            } else {
                                uiState.bloodGroupFilter == BloodGroup.fromString(bgStr)
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val bg = if (bgStr == "Any") null else BloodGroup.fromString(bgStr)
                                    viewModel.setBloodGroupFilter(bg)
                                },
                                label = { Text(bgStr) }
                            )
                        }
                    }
                }
            }

            // Bottom Counts Caption & Empty State overlay if needed
            if (visibleDonors.isEmpty() && visibleRequests.isEmpty() && !uiState.isLoading) {
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmptyState(
                            title = "No Markers Found",
                            description = "No donors or emergency requests match the current filters.",
                            icon = Icons.Default.Info
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shadowElevation = 2.dp
                ) {
                    Text(
                        text = "${visibleDonors.size} donors · ${visibleRequests.size} requests",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
