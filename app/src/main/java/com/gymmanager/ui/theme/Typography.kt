package com.gymmanager.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

val GymTypography = Typography(
    headlineLarge  = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold,   letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall  = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleLarge     = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    titleMedium    = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    titleSmall     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    bodyLarge      = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    bodySmall      = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
    labelLarge     = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium    = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall     = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp)
)

val GymShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small      = RoundedCornerShape(12.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)
