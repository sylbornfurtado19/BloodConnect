package com.bloodconnect.ui.profile

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.data.model.Badge
import com.bloodconnect.data.model.DonationEligibility
import com.bloodconnect.data.model.DonorTier
import com.bloodconnect.ui.components.BloodConnectButton
import com.bloodconnect.ui.components.BloodConnectOutlinedButton
import com.bloodconnect.ui.components.BloodConnectTextField
import com.bloodconnect.ui.components.BloodGroupBadge
import com.bloodconnect.ui.theme.BloodConnectTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val standardBloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
private const val FALLBACK_NAME = "Not available"
private const val FALLBACK_BLOOD_GROUP = "Not specified"
private const val FALLBACK_CITY = "Not specified"
private const val FALLBACK_CONTACT = "Not provided"

/**
 * Screen displaying the user's donor profile, donor tier, lives saved, badges, streak, availability status, and general donation guidelines.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onSignOut: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf(FALLBACK_NAME) }
    var bloodGroup by remember { mutableStateOf(FALLBACK_BLOOD_GROUP) }
    var cityLocality by remember { mutableStateOf(FALLBACK_CITY) }
    var contactInfo by remember { mutableStateOf(FALLBACK_CONTACT) }
    var isDonorAvailable by remember { mutableStateOf(false) }
    var isContactable by remember { mutableStateOf(true) }

    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf(displayName) }
    var editBloodGroup by remember { mutableStateOf(bloodGroup) }
    var editCity by remember { mutableStateOf(cityLocality) }
    var editContact by remember { mutableStateOf(contactInfo) }

    var nameError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }

    var showSuccessDialog by remember { mutableStateOf(false) }
    var showDonatedTodayDialog by remember { mutableStateOf(false) }

    val lastDonationAt = viewModel.user?.lastDonationAt
    val isEligibleToDonate = DonationEligibility.isEligible(lastDonationAt)
    val eligibleOnDateStr = remember(lastDonationAt) {
        val eligibleTimestamp = DonationEligibility.eligibleOn(lastDonationAt)
        if (eligibleTimestamp != null) {
            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(eligibleTimestamp))
        } else null
    }

    val donationCount = viewModel.donationCount
    val donationStreak = viewModel.donationStreak
    val donorTier = viewModel.donorTier
    val nextThreshold = DonorTier.nextThreshold(donationCount)
    val earnedBadges = viewModel.badges
    val allBadges = Badge.entries

    LaunchedEffect(viewModel.user) {
        val user = viewModel.user
        if (user != null) {
            val name = user.displayName.ifBlank { user.fullName }.trim()
            displayName = if (name.isNotBlank()) name else FALLBACK_NAME

            val bg = user.bloodGroup.trim()
            bloodGroup = if (bg.isNotBlank()) bg else FALLBACK_BLOOD_GROUP

            val locality = user.locality.trim()
            cityLocality = if (locality.isNotBlank()) locality else FALLBACK_CITY

            val contact = user.email.ifBlank { user.phone }.trim()
            contactInfo = if (contact.isNotBlank()) contact else FALLBACK_CONTACT

            isDonorAvailable = user.isAvailable
            isContactable = user.contactable
        } else {
            displayName = FALLBACK_NAME
            bloodGroup = FALLBACK_BLOOD_GROUP
            cityLocality = FALLBACK_CITY
            contactInfo = FALLBACK_CONTACT
            isDonorAvailable = false
            isContactable = true
        }
    }

    fun startEditing() {
        editName = if (displayName != FALLBACK_NAME) displayName else ""
        editBloodGroup = if (bloodGroup in standardBloodGroups) bloodGroup else standardBloodGroups.first()
        editCity = if (cityLocality != FALLBACK_CITY) cityLocality else ""
        editContact = if (contactInfo != FALLBACK_CONTACT) contactInfo else ""
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
        val trimmedName = editName.trim()
        val trimmedCity = editCity.trim()

        var isValid = true

        if (trimmedName.isBlank()) {
            nameError = "Display name cannot be empty"
            isValid = false
        } else if (trimmedName.length < 2) {
            nameError = "Display name must be at least 2 characters"
            isValid = false
        } else {
            nameError = null
        }

        if (trimmedCity.isBlank()) {
            cityError = "City/locality cannot be empty"
            isValid = false
        } else if (trimmedCity.length < 2) {
            cityError = "City/locality must be at least 2 characters"
            isValid = false
        } else {
            cityError = null
        }

        if (isValid) {
            displayName = trimmedName
            bloodGroup = editBloodGroup
            cityLocality = trimmedCity
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
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val initials = displayName
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                        .take(2)
                        .joinToString("")
                        .ifEmpty { "U" }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
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
                            bloodGroup = if (bloodGroup in standardBloodGroups) bloodGroup else "--",
                            size = 36.dp
                        )
                        Text(
                            text = if (bloodGroup in standardBloodGroups) {
                                "Registered Blood Group ($bloodGroup)"
                            } else {
                                "Blood Group Not Specified"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(if (isDonorAvailable && isEligibleToDonate) "Active Donor" else "Inactive / Cooldown")
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (isDonorAvailable && isEligibleToDonate) {
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                            },
                            labelColor = if (isDonorAvailable && isEligibleToDonate) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // Real Donor Tier & Impact Card (F4)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Donor Tier: ${donorTier.displayName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Lives Saved: $donationCount",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        SuggestionChip(
                            onClick = {},
                            label = { Text(donorTier.displayName) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                labelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Text(
                        text = "* Each confirmed donation counts as 1 life saved.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (nextThreshold != null) {
                        val prevMin = donorTier.minDonations
                        val progress = (donationCount - prevMin).toFloat() / (nextThreshold - prevMin).toFloat()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            LinearProgressIndicator(
                                progress = { progress.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "$donationCount / $nextThreshold donations to reach next tier",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = "Maximum tier reached! Thank you for your extraordinary lifesaving contributions.",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Badges & Streak Card (3D)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Badges & Achievements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Donation streak: $donationStreak",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        allBadges.forEach { badge ->
                            val isEarned = badge in earnedBadges
                            OutlinedCard(
                                modifier = Modifier.width(130.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = if (isEarned) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = badge.emoji,
                                        style = MaterialTheme.typography.headlineSmall
                                    )
                                    Text(
                                        text = badge.displayName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEarned) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = if (isEarned) "Unlocked" else badge.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isEarned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val shareText = "I've donated blood $donationCount time(s) as a ${donorTier.displayName} donor on BloodConnect. Be a hero too! #BloodConnect"
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Share my impact")
                    }
                }
            }

            // 2. Donation Cooldown & Eligibility Action Card
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Donation Status & Cooldown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!isEligibleToDonate && eligibleOnDateStr != null) {
                        Text(
                            text = "You are currently in a 90-day donation cooldown. Eligible again on $eligibleOnDateStr.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = "You are currently eligible to donate blood.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isDonorAvailable) {
                            Text(
                                text = "You can switch availability back on when your cooldown ends.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showDonatedTodayDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("I donated today")
                    }
                }
            }

            // 3. Donor Availability & Privacy Settings Card (3G)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    text = if (isDonorAvailable) "Available to Donate" else "Currently Unavailable",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDonorAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isDonorAvailable) "Status: Active Donor" else "Status: Inactive / On Cooldown",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isDonorAvailable) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Switch(
                            checked = isDonorAvailable,
                            onCheckedChange = {
                                isDonorAvailable = it
                                viewModel.updateAvailability(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Phone Privacy Switch (3G)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Share my phone with requesters I pledge to",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = isContactable,
                            onCheckedChange = {
                                isContactable = it
                                viewModel.updateContactable(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = if (isDonorAvailable) {
                            "You are currently marked as an active volunteer donor. You will receive emergency blood request notifications."
                        } else {
                            "You are currently marked as unavailable. Requests and emergency alerts are paused while inactive."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 4. Profile Information / Edit Section
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
                            label = "Contact Info (Email/Phone)",
                            placeholder = "e.g., user@example.com",
                            leadingIcon = Icons.Default.Email
                        )

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
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProfileInfoItem(
                                    icon = Icons.Default.Person,
                                    label = "Full Name",
                                    value = displayName
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    BloodGroupBadge(
                                        bloodGroup = if (bloodGroup in standardBloodGroups) bloodGroup else "--",
                                        size = 40.dp
                                    )
                                    Column {
                                        Text(
                                            text = "Blood Group",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (bloodGroup in standardBloodGroups) "$bloodGroup Donor" else "Not Specified",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                ProfileInfoItem(
                                    icon = Icons.Default.LocationCity,
                                    label = "City / Locality",
                                    value = cityLocality
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                ProfileInfoItem(
                                    icon = Icons.Default.Email,
                                    label = "Contact Info",
                                    value = contactInfo.ifBlank { "Not provided" }
                                )
                            }
                        }

                        BloodConnectOutlinedButton(
                            text = "Edit Profile Information",
                            icon = Icons.Default.Edit,
                            onClick = { startEditing() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Blocked Users Section (2I)
            if (viewModel.blockedUsers.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Blocked users",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        viewModel.blockedUsers.forEach { (targetUid, targetName) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = targetName.ifBlank { "User $targetUid" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedButton(
                                    onClick = { viewModel.unblockUser(targetUid) },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Unblock")
                                }
                            }
                        }
                    }
                }
            }

            // 5. Sign Out Action
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
                    text = "Profile Updated",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = "Updated profile for $displayName ($bloodGroup) in $cityLocality.")
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showDonatedTodayDialog) {
        AlertDialog(
            onDismissRequest = { showDonatedTodayDialog = false },
            title = { Text("Confirm Blood Donation") },
            text = { Text("Did you donate blood today? This will record your donation and start your 90-day cooldown period.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDonatedTodayDialog = false
                        viewModel.markDonatedToday()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDonatedTodayDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenPreview() {
    BloodConnectTheme {
        ProfileScreen(onNavigateBack = {})
    }
}
