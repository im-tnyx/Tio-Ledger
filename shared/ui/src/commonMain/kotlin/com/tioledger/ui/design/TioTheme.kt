@file:Suppress("FunctionName")

package com.tioledger.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TioLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) TioMaterialColorSchemes.dark else TioMaterialColorSchemes.light,
        typography = TioTypography.material,
        shapes = TioShapes.material,
        content = content,
    )
}

object TioMaterialColorSchemes {
    val light: ColorScheme =
        lightColorScheme(
            primary = TioColors.light.primary,
            onPrimary = TioColors.light.onPrimary,
            secondary = TioColors.light.secondary,
            onSecondary = TioColors.light.onSecondary,
            background = TioColors.light.background,
            onBackground = TioColors.light.onBackground,
            surface = TioColors.light.surface,
            onSurface = TioColors.light.onSurface,
            outline = TioColors.light.outline,
            error = TioColors.light.negative,
            onError = Color.White,
        )

    val dark: ColorScheme =
        darkColorScheme(
            primary = TioColors.dark.primary,
            onPrimary = TioColors.dark.onPrimary,
            secondary = TioColors.dark.secondary,
            onSecondary = TioColors.dark.onSecondary,
            background = TioColors.dark.background,
            onBackground = TioColors.dark.onBackground,
            surface = TioColors.dark.surface,
            onSurface = TioColors.dark.onSurface,
            outline = TioColors.dark.outline,
            error = TioColors.dark.negative,
            onError = Color.Black,
        )
}

object TioTypography {
    val material: Typography =
        Typography(
            titleLarge = Typography().titleLarge.copy(fontSize = 20.sp),
            titleMedium = Typography().titleMedium.copy(fontSize = 16.sp),
            bodyLarge = Typography().bodyLarge.copy(fontSize = 16.sp),
            bodyMedium = Typography().bodyMedium.copy(fontSize = 14.sp),
            labelLarge = Typography().labelLarge.copy(fontSize = 14.sp),
            labelMedium = Typography().labelMedium.copy(fontSize = 12.sp),
        )
}

object TioShapes {
    val material: Shapes =
        Shapes(
            extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
            small = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
            large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        )
}
