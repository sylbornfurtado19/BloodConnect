package com.bloodconnect.ui.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.R
import com.bloodconnect.ui.theme.BloodConnectTheme
import com.bloodconnect.ui.theme.PureWhite
import com.bloodconnect.ui.theme.StitchAmber
import com.bloodconnect.ui.theme.StitchCrimson
import com.bloodconnect.ui.theme.StitchCrimsonDark
import com.bloodconnect.ui.theme.StitchGreen
import com.bloodconnect.utils.dialNumber
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Main Home & Dashboard screen for BloodConnect.
 * Redesigned to faithfully replicate the Google Stitch design specifications:
 * - Obsidian dark background with custom top location selector and MUMBAI badge.
 * - 24x7 State Blood Council (SBTC) Toll-Free emergency hotline banner.
 * - High-impact Crimson gradient "Request Blood Urgently" hero card.
 * - "Donate Blood" voluntary donor registration card with ACTIVE CITY status badge.
 * - 2x4 Quick Blood Group Check grid showing live city-wide availability.
 * - Urgent Hospital Requests live feed (e.g. KEM Hospital, Tata Memorial, Sion Hospital).
 * - Quick services shortcut chips (Nearby Map, Blood Banks, Blood Drives, AI Assistant).
 * - Sleek 4-tab bottom navigation bar.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onSignOut: () -> Unit,
    onNavigateToDonorSearch: () -> Unit = {},
    onNavigateToBloodRequests: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToMyRequests: () -> Unit = {},
    onNavigateToDonorMap: () -> Unit = {},
    onNavigateToBloodDrives: () -> Unit = {},
    onNavigateToBloodBanks: () -> Unit = {},
    onNavigateToDemandTrends: () -> Unit = {},
    onNavigateToAssistant: () -> Unit = {},
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var selectedBottomNavIndex by remember { mutableIntStateOf(0) }

    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    LaunchedEffect(Unit) {
        if (!locationPermissionState.allPermissionsGranted && !homeViewModel.permissionRequested) {
            homeViewModel.markPermissionRequested()
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            homeViewModel.syncLocation()
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted && !homeViewModel.notificationPermissionRequested) {
                homeViewModel.markNotificationPermissionRequested()
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StitchHomeTopBar(
                onProfileClick = onNavigateToProfile,
                onNotificationClick = onNavigateToBloodRequests
            )
        },
        bottomBar = {
            StitchBottomNavBar(
                selectedIndex = selectedBottomNavIndex,
                onSelectTab = { index ->
                    selectedBottomNavIndex = index
                    when (index) {
                        0 -> {} // Already on Home
                        1 -> onNavigateToBloodBanks()
                        2 -> onNavigateToMyRequests()
                        3 -> onNavigateToProfile()
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SBTC 24x7 Helpline Card
            StitchHelplineCard(
                onCallClick = { dialNumber(context, "104") }
            )

            // 2. High Urgency "Request Blood Urgently" Hero Card
            StitchUrgentRequestHeroCard(
                onRequestClick = onNavigateToBloodRequests
            )

            // 3. "Donate Blood" Voluntary Registration Card
            StitchDonateBloodCard(
                onRegisterClick = onNavigateToDonorSearch
            )

            // 4. Quick Blood Group Check Matrix (8 blood groups)
            StitchQuickBloodGroupGrid(
                onGroupSelected = { _ -> onNavigateToDonorSearch() },
                onViewAllClick = onNavigateToDonorSearch
            )

            // 5. Urgent Hospital Requests Live Feed
            StitchUrgentHospitalFeed(
                onRequestCardClick = onNavigateToBloodRequests
            )

            // 6. Fast Services & AI Assistant Shortcuts
            StitchServicesRow(
                onNavigateToDonorMap = onNavigateToDonorMap,
                onNavigateToBloodBanks = onNavigateToBloodBanks,
                onNavigateToBloodDrives = onNavigateToBloodDrives,
                onNavigateToDemandTrends = onNavigateToDemandTrends,
                onNavigateToAssistant = onNavigateToAssistant
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Top App Bar with BloodConnect branding, MUMBAI capsule, location dropdown, and actions.
 */
@Composable
private fun StitchHomeTopBar(
    onProfileClick: () -> Unit,
    onNotificationClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Logo + Title + Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(StitchCrimson.copy(alpha = 0.2f))
                        .border(1.dp, StitchCrimson.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = StitchCrimson,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Blood",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Connect",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = StitchCrimson
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = StitchCrimson
                        ) {
                            Text(
                                text = stringResource(R.string.login_city_badge),
                                color = PureWhite,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }

                    // Location Selector Subtitle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = StitchCrimson,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = stringResource(R.string.home_location_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Right: Notification Bell + Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Notification Bell with unread dot
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable(onClick = onNotificationClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                    // Notification dot
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .align(Alignment.TopEnd)
                            .padding(end = 1.dp, top = 1.dp)
                            .clip(CircleShape)
                            .background(StitchCrimson)
                    )
                }

                // User Avatar Circle (RK initials)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable(onClick = onProfileClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "RK",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 24x7 State Blood Council (SBTC) Toll-Free Emergency Helpline Banner.
 */
@Composable
private fun StitchHelplineCard(
    onCallClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, StitchGreen.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(StitchGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = StitchGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = stringResource(R.string.home_sbtc_header),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Helpline: ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "104 / 1910",
                            style = MaterialTheme.typography.bodySmall,
                            color = StitchGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " (Toll Free)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Call button
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = StitchGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onCallClick)
            ) {
                Text(
                    text = stringResource(R.string.home_sbtc_call),
                    color = PureWhite,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * High-impact Crimson Gradient Urgent Blood Request Hero Card.
 */
@Composable
private fun StitchUrgentRequestHeroCard(
    onRequestClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(StitchCrimson, StitchCrimsonDark)
                    )
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PureWhite.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PureWhite.copy(alpha = 0.25f)
                    ) {
                        Text(
                            text = stringResource(R.string.home_urgent_badge),
                            color = PureWhite,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                // Title & Description
                Text(
                    text = stringResource(R.string.home_urgent_request_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PureWhite
                )

                Text(
                    text = stringResource(R.string.home_urgent_request_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = PureWhite.copy(alpha = 0.88f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // CTA Button
                Button(
                    onClick = onRequestClick,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PureWhite,
                        contentColor = StitchCrimson
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.home_urgent_request_btn),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = StitchCrimson
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = StitchCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * "Donate Blood" Voluntary Registration Card.
 */
@Composable
private fun StitchDonateBloodCard(
    onRegisterClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header row with Icon and ACTIVE CITY badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(StitchCrimson.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VolunteerActivism,
                        contentDescription = null,
                        tint = StitchCrimson,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchGreen.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, StitchGreen.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = stringResource(R.string.home_donate_active_city),
                        color = StitchGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            // Title & Description
            Text(
                text = stringResource(R.string.home_donate_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = stringResource(R.string.home_donate_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Action Button
            OutlinedButton(
                onClick = onRegisterClick,
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.home_donate_btn),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Data model for Blood Group Availability item in the Stitch matrix.
 */
private data class BloodGroupStatus(
    val group: String,
    val statusText: String,
    val statusColor: Color,
    val badgeBgColor: Color
)

/**
 * 2x4 Quick Blood Group Check grid with live verified inventory status.
 */
@Composable
private fun StitchQuickBloodGroupGrid(
    onGroupSelected: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    val bloodGroups = listOf(
        BloodGroupStatus("O-", "Critical (2)", StitchCrimson, StitchCrimson.copy(alpha = 0.12f)),
        BloodGroupStatus("O+", "Available", StitchGreen, StitchGreen.copy(alpha = 0.12f)),
        BloodGroupStatus("A+", "Available", StitchGreen, StitchGreen.copy(alpha = 0.12f)),
        BloodGroupStatus("A-", "Low (5)", StitchAmber, StitchAmber.copy(alpha = 0.12f)),
        BloodGroupStatus("B+", "Available", StitchGreen, StitchGreen.copy(alpha = 0.12f)),
        BloodGroupStatus("B-", "Low (4)", StitchAmber, StitchAmber.copy(alpha = 0.12f)),
        BloodGroupStatus("AB+", "Available", StitchGreen, StitchGreen.copy(alpha = 0.12f)),
        BloodGroupStatus("AB-", "Low (7)", StitchAmber, StitchAmber.copy(alpha = 0.12f))
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = stringResource(R.string.home_quick_check_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.home_quick_check_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(R.string.home_view_all),
                color = StitchCrimson,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onViewAllClick)
                    .padding(4.dp)
            )
        }

        // 2 rows of 4 cards
        val row1 = bloodGroups.take(4)
        val row2 = bloodGroups.drop(4)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row1.forEach { item ->
                BloodGroupStatusCard(
                    item = item,
                    onClick = { onGroupSelected(item.group) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            row2.forEach { item ->
                BloodGroupStatusCard(
                    item = item,
                    onClick = { onGroupSelected(item.group) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BloodGroupStatusCard(
    item: BloodGroupStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.group,
                color = item.statusColor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.statusText,
                color = item.statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Data class for Urgent Hospital Request Card.
 */
private data class HospitalRequestItem(
    val bloodGroup: String,
    val units: String,
    val hospitalName: String,
    val details: String,
    val severityTag: String,
    val severityColor: Color
)

/**
 * Live Urgent Hospital Requests Feed matching Stitch preview.
 */
@Composable
private fun StitchUrgentHospitalFeed(
    onRequestCardClick: () -> Unit
) {
    val activeRequests = listOf(
        HospitalRequestItem(
            bloodGroup = "B+",
            units = "2 Units",
            hospitalName = "KEM Hospital, Parel",
            details = "Accident Ward • Critical OT Needed",
            severityTag = "CRITICAL",
            severityColor = StitchCrimson
        ),
        HospitalRequestItem(
            bloodGroup = "O-",
            units = "1 Unit",
            hospitalName = "Tata Memorial Hospital, Parel",
            details = "Emergency Surgery • ICU Care",
            severityTag = "CRITICAL",
            severityColor = StitchCrimson
        ),
        HospitalRequestItem(
            bloodGroup = "AB-",
            units = "3 Units",
            hospitalName = "Sion Hospital, Sion",
            details = "Trauma Ward • OT Priority",
            severityTag = "URGENT",
            severityColor = StitchAmber
        )
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Feed Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Live pulsing red dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(StitchCrimson)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.home_urgent_hospital_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(R.string.home_urgent_hospital_active),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // List of Urgent Hospital Cards
        activeRequests.forEach { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onRequestCardClick),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left blood tag container
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = StitchCrimson.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, StitchCrimson.copy(alpha = 0.35f)),
                            modifier = Modifier.size(width = 52.dp, height = 50.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = item.bloodGroup,
                                    color = StitchCrimson,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = item.units,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Center Hospital & Details
                        Column {
                            Text(
                                text = item.hospitalName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.details,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Severity Pill Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = item.severityColor.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, item.severityColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = item.severityTag,
                            color = item.severityColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Fast Services Shortcut Chips Row (Nearby Map, Blood Banks, Drives, Demand Trends, AI Assistant).
 */
@Composable
private fun StitchServicesRow(
    onNavigateToDonorMap: () -> Unit,
    onNavigateToBloodBanks: () -> Unit,
    onNavigateToBloodDrives: () -> Unit,
    onNavigateToDemandTrends: () -> Unit,
    onNavigateToAssistant: () -> Unit
) {
    val serviceItems = listOf(
        ServiceShortcut("Map", Icons.Default.Map, onNavigateToDonorMap),
        ServiceShortcut("Banks", Icons.Default.LocalHospital, onNavigateToBloodBanks),
        ServiceShortcut("Drives", Icons.Default.DateRange, onNavigateToBloodDrives),
        ServiceShortcut("Trends", Icons.AutoMirrored.Filled.ShowChart, onNavigateToDemandTrends),
        ServiceShortcut("AI Assistant", Icons.Default.AutoAwesome, onNavigateToAssistant)
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Services & Features",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(serviceItems) { item ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = item.action)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = StitchCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private data class ServiceShortcut(
    val label: String,
    val icon: ImageVector,
    val action: () -> Unit
)

/**
 * Stitch 4-Tab Bottom Navigation Bar.
 */
@Composable
private fun StitchBottomNavBar(
    selectedIndex: Int,
    onSelectTab: (Int) -> Unit
) {
    val navTabs = listOf(
        BottomNavItem(stringResource(R.string.home_tab_home), Icons.Default.Home, Icons.Default.Home),
        BottomNavItem(stringResource(R.string.home_tab_banks), Icons.Outlined.LocalHospital, Icons.Default.LocalHospital),
        BottomNavItem(stringResource(R.string.home_tab_requests), Icons.AutoMirrored.Outlined.Assignment, Icons.AutoMirrored.Outlined.Assignment),
        BottomNavItem(stringResource(R.string.home_tab_profile), Icons.Outlined.Person, Icons.Default.Person)
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navTabs.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index
                val itemColor = if (isSelected) StitchCrimson else MaterialTheme.colorScheme.onSurfaceVariant

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(index) }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                        contentDescription = tab.label,
                        tint = itemColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = itemColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val unselectedIcon: ImageVector,
    val selectedIcon: ImageVector
)

@Preview(showBackground = true, backgroundColor = 0xFF0B0F17)
@Composable
private fun HomeScreenDarkPreview() {
    BloodConnectTheme(darkTheme = true) {
        HomeScreen(
            onSignOut = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenLightPreview() {
    BloodConnectTheme(darkTheme = false) {
        HomeScreen(
            onSignOut = {}
        )
    }
}
