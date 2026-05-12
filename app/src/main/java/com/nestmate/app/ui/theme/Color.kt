package com.nestmate.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary: Purple ────────────────────────────────────────────────────────────
val PurplePrimary       = Color(0xFF8B5CF6)   // violet-500
val PurplePrimaryDark   = Color(0xFF7C3AED)   // violet-600
val PurpleLight         = Color(0xFFA78BFA)   // violet-400
val PurpleContainer     = Color(0xFF2D1B69)   // deep purple tint
val OnPurpleContainer   = Color(0xFFEDE9FE)   // very light lavender

// ── Accent: Cyan / Teal ─────────────────────────────────────────────────────
val AccentCyan          = Color(0xFF06B6D4)   // cyan-500
val AccentCyanLight     = Color(0xFF67E8F9)   // cyan-300

// ── Surfaces (Dark) ──────────────────────────────────────────────────────────
val BackgroundDark      = Color(0xFF0A0A0F)   // near-black with slight blue tint
val SurfaceDark         = Color(0xFF111118)   // card base
val SurfaceElevated     = Color(0xFF1A1A2E)   // slightly lighter card
val SurfaceGlass        = Color(0x1FFFFFFF)   // white @ 12% — glass fill (was 10%)
val GlassBorder         = Color(0x24FFFFFF)   // white @ 14% — subtler glass border (was 20%)
val GlassGlow           = Color(0x1AA78BFA)   // purple @ 10% — softer glow tint (was 15%)
val GlassGlowSubtle     = Color(0x0DA78BFA)   // purple @ 5% — very subtle glow

// ── Surface Containers (M3 hierarchy) ────────────────────────────────────────
val SurfaceContainerLowest  = Color(0xFF0D0D14)
val SurfaceContainer        = Color(0xFF141420)
val SurfaceContainerHigh    = Color(0xFF1C1C30)
val SurfaceContainerHighest = Color(0xFF232338)

// ── Bottom Nav ───────────────────────────────────────────────────────────────
val BottomNavBackground = Color(0xFF111118)    // solid dark, no alpha

// ── Surfaces (Light) ─────────────────────────────────────────────────────────
val BackgroundLight     = Color(0xFFF5F3FF)   // very light lavender
val SurfaceLight        = Color(0xFFFFFFFF)
val SurfaceVariantLight = Color(0xFFEDE9FE)

// ── Text ─────────────────────────────────────────────────────────────────────
val TextPrimary         = Color(0xFFF8F8FF)
val TextSecondary       = Color(0xFFB0B0C8)
val TextMuted           = Color(0xFF6B6B8A)

// ── Status ───────────────────────────────────────────────────────────────────
val SuccessGreen        = Color(0xFF22C55E)
val WarningAmber        = Color(0xFFF59E0B)
val ErrorRed            = Color(0xFFEF4444)
val InfoBlue            = Color(0xFF3B82F6)

// ── Shimmer ──────────────────────────────────────────────────────────────────
val ShimmerDark1        = Color(0xFF1E1E2E)
val ShimmerDark2        = Color(0xFF2A2A3E)

// ── Legacy alias — keeps AccentIndigo references compiling ───────────────────
val AccentIndigo        = PurplePrimary
// Old aliases kept for screens not yet migrated
val ForestGreen         = PurplePrimary
val ForestGreenLight    = PurpleLight
val ForestGreenContainer= PurpleContainer
val NestAmber           = AccentCyan
val NestAmberLight      = AccentCyanLight
val NestAmberContainer  = Color(0xFF0E3340)
val SurfaceWhite        = SurfaceLight
val OnSurfaceLight      = Color(0xFF1A1A2E)
val OnSurfaceVariantLight = Color(0xFF4B4B6A)
val SurfaceVariantDark  = SurfaceElevated