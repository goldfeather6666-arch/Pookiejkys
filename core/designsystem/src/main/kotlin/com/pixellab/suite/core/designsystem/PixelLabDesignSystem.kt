package com.pixellab.suite.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * PIXEL LAB Visual Design System
 * Direction: Obsidian + Electric Violet + Cyan + Deep Navy + Glass
 */
object PixelLabColors {
    val ObsidianBg = Color(0xFF07090F)
    val DeepNavySurface = Color(0xFF0E1322)
    val DeepNavyElevated = Color(0xFF161D33)
    val GlassSurface = Color(0xB8172038)
    val GlassBorder = Color(0xFF2B3A63)
    val ElectricViolet = Color(0xFF7C4DFF)
    val ElectricVioletSoft = Color(0xFFB388FF)
    val CyanPulse = Color(0xFF00E5FF)
    val CyanSoft = Color(0xFF84FFFF)
    val EmeraldNominal = Color(0xFF00E676)
    val AmberCaution = Color(0xFFFFAB00)
    val CrimsonAlert = Color(0xFFFF5252)
    val TextPrimary = Color(0xFFF3F6FF)
    val TextSecondary = Color(0xFFA6B4D0)
    val TextMuted = Color(0xFF6C7A9C)

    // Light Theme counterparts
    val LightBg = Color(0xFFF4F7FC)
    val LightSurface = Color(0xFFFFFFFF)
    val LightBorder = Color(0xFFD5DFEE)
    val LightTextPrimary = Color(0xFF0B1120)
    val LightTextSecondary = Color(0xFF475569)
}

enum class ThemeMode {
    OBSIDIAN_DARK,
    LIGHT,
    SYSTEM
}

@Immutable
data class PixelLabDesignTokens(
    val isDark: Boolean = true,
    val highContrast: Boolean = false,
    val reducedMotion: Boolean = false,
    val cardCornerRadius: Dp = 20.dp,
    val glassAlpha: Float = 0.78f
)

val LocalPixelLabTokens = staticCompositionLocalOf { PixelLabDesignTokens() }

private val ObsidianColorScheme = darkColorScheme(
    primary = PixelLabColors.ElectricViolet,
    onPrimary = Color.White,
    secondary = PixelLabColors.CyanPulse,
    onSecondary = PixelLabColors.ObsidianBg,
    tertiary = PixelLabColors.EmeraldNominal,
    background = PixelLabColors.ObsidianBg,
    onBackground = PixelLabColors.TextPrimary,
    surface = PixelLabColors.DeepNavySurface,
    onSurface = PixelLabColors.TextPrimary,
    surfaceVariant = PixelLabColors.DeepNavyElevated,
    onSurfaceVariant = PixelLabColors.TextSecondary,
    error = PixelLabColors.CrimsonAlert
)

private val HighContrastDarkScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    secondary = Color(0xFFFFEA00),
    onSecondary = Color.Black,
    tertiary = Color(0xFF00E676),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0A0A0A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color.White,
    error = Color(0xFFFF5252)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5E35B1),
    onPrimary = Color.White,
    secondary = Color(0xFF00838F),
    onSecondary = Color.White,
    background = PixelLabColors.LightBg,
    onBackground = PixelLabColors.LightTextPrimary,
    surface = PixelLabColors.LightSurface,
    onSurface = PixelLabColors.LightTextPrimary,
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = PixelLabColors.LightTextSecondary,
    error = Color(0xFFD32F2F)
)

val PixelLabTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)

@Composable
fun PixelLabTheme(
    darkTheme: Boolean = true,
    highContrast: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        highContrast -> HighContrastDarkScheme
        darkTheme -> ObsidianColorScheme
        else -> LightColorScheme
    }
    val tokens = PixelLabDesignTokens(
        isDark = darkTheme || highContrast,
        highContrast = highContrast,
        reducedMotion = reducedMotion,
        glassAlpha = if (highContrast) 1.0f else 0.78f
    )
    CompositionLocalProvider(LocalPixelLabTokens provides tokens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PixelLabTypography,
            content = content
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accentColor: Color = PixelLabColors.ElectricViolet,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val tokens = LocalPixelLabTokens.current
    val shape = RoundedCornerShape(tokens.cardCornerRadius)
    val borderColor = if (tokens.highContrast) {
        accentColor
    } else {
        accentColor.copy(alpha = 0.36f)
    }
    val surfaceColor = if (tokens.highContrast) {
        Color(0xFF0A0A0A)
    } else if (tokens.isDark) {
        PixelLabColors.DeepNavyElevated.copy(alpha = tokens.glassAlpha)
    } else {
        PixelLabColors.LightSurface
    }

    val clickModifier = if (onClick != null) {
        Modifier.clip(shape).clickable { onClick() }
    } else {
        Modifier.clip(shape)
    }

    Surface(
        modifier = modifier
            .then(clickModifier)
            .border(
                BorderStroke(if (tokens.highContrast) 2.dp else 1.dp, borderColor),
                shape
            ),
        color = surfaceColor,
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            content = content
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    color: Color = PixelLabColors.CyanPulse,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "statusPillColor"
    )
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(animatedColor.copy(alpha = 0.16f))
            .border(1.dp, animatedColor.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(animatedColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = animatedColor
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    accentColor: Color = PixelLabColors.CyanPulse
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!badge.isNullOrBlank()) {
            StatusPill(label = badge, color = accentColor)
        }
    }
}

@Composable
fun TelemetryKeyValueRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = valueColor
        )
    }
}
