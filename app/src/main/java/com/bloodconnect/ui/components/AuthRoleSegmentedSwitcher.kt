package com.bloodconnect.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.bloodconnect.ui.theme.StitchAmber

/**
 * Segmented pill tab switcher matching the modern authentication design:
 * - "Donor" tab
 * - "Hospital" tab
 *
 * Selected tab renders with an amber pill container and high-contrast dark typography.
 * The container adapts seamlessly to both dark and light theme palettes.
 */
@Composable
fun AuthRoleSegmentedSwitcher(
    selectedRole: String, // "DONOR" or "HOSPITAL_ADMIN"
    onRoleSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    donorLabel: String = "Donor",
    hospitalLabel: String = "Hospital"
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val isDonor = selectedRole.equals("DONOR", ignoreCase = true)
            val isHospital = selectedRole.equals("HOSPITAL_ADMIN", ignoreCase = true)

            val donorBgColor by animateColorAsState(
                targetValue = if (isDonor) StitchAmber else Color.Transparent,
                animationSpec = tween(durationMillis = 200),
                label = "donorBg"
            )
            val hospitalBgColor by animateColorAsState(
                targetValue = if (isHospital) StitchAmber else Color.Transparent,
                animationSpec = tween(durationMillis = 200),
                label = "hospitalBg"
            )

            val unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

            // 1. Donor Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(donorBgColor)
                    .clickable { onRoleSelected("DONOR") }
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isDonor) Color(0xFF0F172A) else unselectedTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = donorLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isDonor) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isDonor) Color(0xFF0F172A) else unselectedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 2. Hospital Tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(26.dp))
                    .background(hospitalBgColor)
                    .clickable { onRoleSelected("HOSPITAL_ADMIN") }
                    .padding(vertical = 10.dp, horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = if (isHospital) Color(0xFF0F172A) else unselectedTextColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = hospitalLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isHospital) FontWeight.Bold else FontWeight.SemiBold,
                        color = if (isHospital) Color(0xFF0F172A) else unselectedTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
