package com.example.dealoptimizer.presentation.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val AppBlue = Color(0xFF1E3A5F)
val AppSecondaryBlue = Color(0xFF2563EB)
val AppGreen = Color(0xFF059669)
val AppRed = Color(0xFFDC2626)
val AppAmber = Color(0xFFF59E0B)
val AppInk = Color(0xFF0F172A)
val AppMuted = Color(0xFF6B7280)
val AppLine = Color(0xFFE4E7EB)
val AppSurface = Color(0xFFF8FAFC)
val AppWarnSurface = Color(0xFFFFF7ED)
val AppWarnText = Color(0xFFC2410C)
val AppDangerSurface = Color(0xFFFEF2F2)

private val AppColors: Colors = lightColors(
    primary = AppBlue,
    primaryVariant = Color(0xFF18324F),
    secondary = AppGreen,
    secondaryVariant = Color(0xFF15803D),
    background = AppSurface,
    surface = Color.White,
    error = AppRed,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppInk,
    onSurface = AppInk,
    onError = Color.White
)

@Composable
fun DealOptimizerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = AppColors,
        typography = Typography(
            h6 = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.SemiBold),
            subtitle1 = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
            button = MaterialTheme.typography.button.copy(fontWeight = FontWeight.SemiBold)
        ),
        shapes = Shapes(
            small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(8.dp)
        ),
        content = content
    )
}
