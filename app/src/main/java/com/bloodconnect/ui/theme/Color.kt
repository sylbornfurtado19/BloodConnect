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

// Stitch Dark Obsidian & Crimson Design System Tokens
val StitchObsidianBg = Color(0xFF0B0F17)      // Deep obsidian dark background
val StitchCardBg = Color(0xFF161F2E)          // Elevated dark card background
val StitchCardBorder = Color(0xFF223049)      // Card border stroke
val StitchCrimson = Color(0xFFE53935)         // Primary crimson accent
val StitchCrimsonDark = Color(0xFFB71C1C)     // Dark gradient crimson
val StitchTextPrimary = Color(0xFFF8FAFC)     // Crisp white text
val StitchTextSecondary = Color(0xFF94A3B8)   // Muted slate text
val StitchInputBg = Color(0xFF101726)         // Input field fill
val StitchInputBorder = Color(0xFF1E293B)     // Input field border
val StitchGoogleBg = Color(0xFF161F2E)        // Google auth button container
val StitchGoogleBorder = Color(0xFF2E3E57)    // Google auth button border
val StitchGlowRed = Color(0x33E53935)         // Crimson drop glow
val StitchAmber = Color(0xFFF59E0B)           // Warning / low stock
val StitchGreen = Color(0xFF10B981)           // Available status / helpline

// Stitch Light Design System Tokens (Crisp, clean, high-contrast)
val StitchLightBg = Color(0xFFF8FAFC)            // Premium soft slate-50 background
val StitchLightCardBg = Color(0xFFFFFFFF)        // Pure white card surface
val StitchLightCardBorder = Color(0xFFE2E8F0)    // Crisp subtle border
val StitchLightInputBg = Color(0xFFF1F5F9)       // Soft input fill
val StitchLightInputBorder = Color(0xFFCBD5E1)   // Input border
val StitchLightTextPrimary = Color(0xFF0F172A)   // Deep slate text
val StitchLightTextSecondary = Color(0xFF64748B) // Neutral secondary slate text
val StitchLightGlowRed = Color(0x1AE53935)       // Soft crimson glow for light mode

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

