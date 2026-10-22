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
import androidx.compose.material.icons.filled.History
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
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bloodconnect.R
import com.bloodconnect.utils.AnalyticsHelper

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
    onNavigateToDonationHistory: () -> Unit = {},
    onSignOut: (() -> Unit)? = null,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val analyticsHelper = remember(context) { AnalyticsHelper(context) }
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
            nameError = context.getString(R.string.profile_error_name_empty)
            isValid = false
        } else if (trimmedName.length < 2) {
            nameError = context.getString(R.string.profile_error_name_min)
            isValid = false
        } else {
            nameError = null
        }

        if (trimmedCity.isBlank()) {
            cityError = context.getString(R.string.profile_error_city_empty)
            isValid = false
        } else if (trimmedCity.length < 2) {
            cityError = context.getString(R.string.profile_error_city_min)
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
                        text = stringResource(R.string.profile_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.profile_cd_back),
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
                        text = if (displayName != FALLBACK_NAME) displayName else stringResource(R.string.profile_fallback_name),
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
                                stringResource(R.string.profile_registered_blood_group, bloodGroup)
                            } else {
                                stringResource(R.string.profile_blood_group_not_specified)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                if (isDonorAvailable && isEligibleToDonate) {
                                    stringResource(R.string.profile_status_active_donor)
                                } else {
                                    stringResource(R.string.profile_status_inactive_cooldown)
                                }
                            )
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
                                text = stringResource(R.string.profile_donor_tier, donorTier.displayName),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = stringResource(R.string.profile_lives_saved_count, donationCount),
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
                        text = stringResource(R.string.profile_lives_saved_footnote),
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
                                text = stringResource(R.string.profile_donations_to_next_tier, donationCount, nextThreshold),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.profile_max_tier_reached),
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
                            text = stringResource(R.string.profile_section_badges),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            text = stringResource(R.string.profile_donation_streak, donationStreak),
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
                                        text = if (isEarned) stringResource(R.string.profile_badge_unlocked) else badge.description,
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
                            analyticsHelper.logShareImpact()
                            val shareText = context.getString(
                                R.string.profile_share_impact_template,
                                donationCount,
                                donorTier.displayName
                            )
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.profile_cd_share_impact),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_btn_share_impact))
                    }

                    OutlinedButton(
                        onClick = onNavigateToDonationHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_btn_donation_history))
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
                        text = stringResource(R.string.profile_section_cooldown),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )

                    if (!isEligibleToDonate && eligibleOnDateStr != null) {
                        Text(
                            text = stringResource(R.string.profile_cooldown_active_msg, eligibleOnDateStr),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.profile_eligible_msg),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isDonorAvailable) {
                            Text(
                                text = stringResource(R.string.profile_cooldown_switch_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { showDonatedTodayDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.profile_btn_donated_today))
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
                                     text = if (isDonorAvailable) {
                                         stringResource(R.string.profile_available_to_donate)
                                     } else {
                                         stringResource(R.string.profile_currently_unavailable)
                                     },
                                     style = MaterialTheme.typography.titleMedium,
                                     fontWeight = FontWeight.Bold,
                                     color = if (isDonorAvailable) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                     modifier = Modifier.semantics { heading() }
                                 )
                                 Text(
                                     text = if (isDonorAvailable) {
                                         stringResource(R.string.profile_status_active_label)
                                     } else {
                                         stringResource(R.string.profile_status_inactive_label)
                                     },
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
                                text = stringResource(R.string.profile_share_phone_toggle),
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
                            stringResource(R.string.profile_volunteer_active_desc)
                        } else {
                            stringResource(R.string.profile_volunteer_inactive_desc)
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
                            text = if (isEditing) stringResource(R.string.profile_section_edit_details) else stringResource(R.string.profile_section_info),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { heading() }
                        )

                        if (!isEditing) {
                            TextButton(
                                onClick = { startEditing() },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = stringResource(R.string.profile_cd_edit_profile),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.profile_btn_edit))
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
                            label = stringResource(R.string.profile_label_display_name_req),
                            placeholder = stringResource(R.string.profile_placeholder_display_name),
                            leadingIcon = Icons.Default.Person,
                            isError = nameError != null,
                            errorMessage = nameError
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = stringResource(R.string.profile_label_blood_group_req),
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
                            label = stringResource(R.string.profile_label_city_req),
                            placeholder = stringResource(R.string.profile_placeholder_city),
                            leadingIcon = Icons.Default.LocationCity,
                            isError = cityError != null,
                            errorMessage = cityError
                        )

                        BloodConnectTextField(
                            value = editContact,
                            onValueChange = { editContact = it },
                            label = stringResource(R.string.profile_label_contact_info),
                            placeholder = stringResource(R.string.profile_placeholder_contact),
                            leadingIcon = Icons.Default.Email
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BloodConnectOutlinedButton(
                                text = stringResource(R.string.profile_btn_cancel),
                                onClick = { cancelEditing() },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                            )

                            BloodConnectButton(
                                text = stringResource(R.string.profile_btn_save_profile),
                                icon = Icons.Default.Save,
                                onClick = { validateAndSave() },
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
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
                                    label = stringResource(R.string.profile_label_full_name),
                                    value = if (displayName != FALLBACK_NAME) displayName else stringResource(R.string.profile_fallback_name)
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
                                            text = stringResource(R.string.profile_label_blood_group),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (bloodGroup in standardBloodGroups) {
                                                stringResource(R.string.profile_donor_blood_group, bloodGroup)
                                            } else {
                                                stringResource(R.string.profile_not_specified)
                                            },
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                ProfileInfoItem(
                                    icon = Icons.Default.LocationCity,
                                    label = stringResource(R.string.profile_label_city),
                                    value = if (cityLocality != FALLBACK_CITY) cityLocality else stringResource(R.string.profile_fallback_city)
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                ProfileInfoItem(
                                    icon = Icons.Default.Email,
                                    label = stringResource(R.string.profile_label_contact),
                                    value = if (contactInfo != FALLBACK_CONTACT && contactInfo.isNotBlank()) contactInfo else stringResource(R.string.profile_fallback_contact)
                                )
                            }
                        }

                        BloodConnectOutlinedButton(
                            text = stringResource(R.string.profile_btn_edit_profile_info),
                            icon = Icons.Default.Edit,
                            onClick = { startEditing() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
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
                            text = stringResource(R.string.profile_section_blocked_users),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.semantics { heading() }
                        )
                        viewModel.blockedUsers.forEach { (targetUid, targetName) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = targetName.ifBlank { stringResource(R.string.profile_user_uid_template, targetUid) },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedButton(
                                    onClick = { viewModel.unblockUser(targetUid) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(stringResource(R.string.profile_btn_unblock))
                                }
                            }
                        }
                    }
                }
            }

            // 5. Sign Out Action
            if (onSignOut != null) {
                BloodConnectOutlinedButton(
                    text = stringResource(R.string.profile_btn_sign_out),
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
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
                    text = stringResource(R.string.profile_dialog_updated_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = stringResource(R.string.profile_dialog_updated_msg, displayName, bloodGroup, cityLocality))
            },
            confirmButton = {
                TextButton(onClick = { showSuccessDialog = false }) {
                    Text(stringResource(R.string.profile_dialog_ok))
                }
            }
        )
    }

    if (showDonatedTodayDialog) {
        AlertDialog(
            onDismissRequest = { showDonatedTodayDialog = false },
            title = { Text(stringResource(R.string.profile_dialog_confirm_donation_title)) },
            text = { Text(stringResource(R.string.profile_dialog_confirm_donation_msg)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDonatedTodayDialog = false
                        viewModel.markDonatedToday()
                    },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.profile_dialog_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDonatedTodayDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.profile_btn_cancel))
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
