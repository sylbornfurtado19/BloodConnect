package com.bloodconnect.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Coral-Red Design System Palette
val CoralPrimary = Color(0xFFFF5964)         // Primary coral-red
val DarkCoral = Color(0xFFE94755)            // Dark coral for accents & pressed states
val SoftPinkBackground = Color(0xFFFFF5F6)   // Soft pink global background
val LightCoralAccent = Color(0xFFFFECEE)     // Light coral container & badge accent
val PureWhite = Color(0xFFFFFFFF)            // Card & surface pure white
val PrimaryText = Color(0xFF202124)          // High contrast dark slate text
val SecondaryText = Color(0xFF737780)        // Neutral secondary text
val SubtleBorder = Color(0xFFECECEF)         // Border and divider outline

// Extended Semantic Tokens (Light)
val SuccessGreen = Color(0xFF00875A)         // Medical active/available donor state
val SuccessGreenContainer = Color(0xFFE3FCEF)
val ErrorRed = Color(0xFFE53935)             // Urgent / error states
val ErrorRedContainer = Color(0xFFFFEBEE)

// Dark Mode Tokens (Calibrated for dark surfaces with coral glow)
val CoralDarkPrimary = Color(0xFFFF7B84)
val DarkCoralOnPrimary = Color(0xFF5C000B)
val DarkBackground = Color(0xFF161112)
val DarkSurface = Color(0xFF1E1718)
val DarkOnSurface = Color(0xFFF1E4E5)
val DarkSurfaceVariant = Color(0xFF2B2022)
val DarkOnSurfaceVariant = Color(0xFFC7B6B7)
val DarkBorder = Color(0xFF3F3133)

// Backward Compatibility Aliases for Legacy Code
val BloodRed = CoralPrimary
val BloodRedDark = CoralDarkPrimary
val CrimsonPrimary = DarkCoral
val RoseSecondary = SecondaryText
val RoseSecondaryDark = DarkOnSurfaceVariant
val SlateTertiary = SuccessGreen
val SlateTertiaryDark = Color(0xFFAECFA9)

val BackgroundLight = SoftPinkBackground
val SurfaceLight = PureWhite
val OnSurfaceLight = PrimaryText
val SurfaceVariantLight = LightCoralAccent
val OnSurfaceVariantLight = SecondaryText

val BackgroundDark = DarkBackground
val SurfaceDark = DarkSurface
val OnSurfaceDark = DarkOnSurface
val SurfaceVariantDark = DarkSurfaceVariant
val OnSurfaceVariantDark = DarkOnSurfaceVariant

