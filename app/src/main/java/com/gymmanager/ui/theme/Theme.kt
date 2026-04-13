package com.gymmanager.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── GYM THEME: Yellow Walls | Grey Carpet | Green Weight Pads ───
val GymYellow      = Color(0xFFFFD600)
val GymYellowDark  = Color(0xFFFFC200)
val GymYellowLight = Color(0xFFFFE566)
val GymGreen       = Color(0xFF2E7D32)
val GymGreenLight  = Color(0xFF4CAF50)
val GymGreenBright = Color(0xFF66BB6A)
val GymGrey        = Color(0xFF616161)
val GymGreyLight   = Color(0xFF9E9E9E)
val GymBgDark      = Color(0xFF0A0A0A)
val GymBgCard      = Color(0xFF1A1A1A)
val GymBgElevated  = Color(0xFF242424)
val GymBgBorder    = Color(0xFF333333)

// Status
val StatusPaid     = Color(0xFF66BB6A)
val StatusUnpaid   = Color(0xFFEF5350)
val StatusPartial  = Color(0xFFFFD600)

// Text
val TextPrimary    = Color(0xFFFFFFFF)
val TextSecondary  = Color(0xFFB0B0B0)
val TextMuted      = Color(0xFF666666)

// Zinc aliases (compatibility)
val Zinc950  = GymBgDark
val Zinc900  = GymBgCard
val Zinc800  = GymBgElevated
val Zinc700  = GymBgBorder
val Zinc600  = Color(0xFF555555)
val Zinc500  = Color(0xFF717171)
val Zinc400  = Color(0xFFB0B0B0)
val Zinc300  = Color(0xFFD4D4D4)

// Legacy aliases
val Emerald500 = GymGreen
val Emerald400 = GymGreenBright
val Emerald600 = Color(0xFF1B5E20)
val Blue500    = Color(0xFF1565C0)
val Purple500  = Color(0xFF6A1B9A)
val Amber500   = GymYellow
val Rose500    = Color(0xFFEF5350)
val Rose400    = Color(0xFFEF9A9A)
val Cyan500    = Color(0xFF00838F)

private val GymColorScheme = darkColorScheme(
    primary           = GymYellow,
    onPrimary         = Color(0xFF1A1A00),
    primaryContainer  = Color(0xFF3D3000),
    secondary         = GymGreen,
    onSecondary       = Color.White,
    secondaryContainer= Color(0xFF1B3A1B),
    tertiary          = GymGrey,
    background        = GymBgDark,
    surface           = GymBgCard,
    surfaceVariant    = GymBgElevated,
    onBackground      = TextPrimary,
    onSurface         = TextPrimary,
    onSurfaceVariant  = TextSecondary,
    error             = StatusUnpaid,
    outline           = GymBgBorder
)

@Composable
fun GymManagerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = GymBgDark.toArgb()
            window.navigationBarColor = GymBgDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = GymColorScheme,
        typography = GymTypography,
        content = content
    )
}
