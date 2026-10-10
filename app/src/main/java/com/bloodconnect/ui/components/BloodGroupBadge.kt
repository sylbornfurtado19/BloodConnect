package com.bloodconnect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bloodconnect.ui.theme.BloodConnectTheme

/**
 * Compact visual badge for blood groups (e.g., A+, O-, B+, AB-).
 */
@Composable
fun BloodGroupBadge(
    bloodGroup: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = bloodGroup,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            fontSize = if (bloodGroup.length > 2) 13.sp else 16.sp,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun BloodGroupBadgePreview() {
    BloodConnectTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BloodGroupBadge(bloodGroup = "O+")
            BloodGroupBadge(bloodGroup = "A-")
            BloodGroupBadge(bloodGroup = "B+")
            BloodGroupBadge(bloodGroup = "AB+")
        }
    }
}
