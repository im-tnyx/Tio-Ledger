package com.tioledger.ui.design

import androidx.compose.ui.graphics.Color

object TioColors {
    val light =
        TioColorScheme(
            primary = Color(0xFF2563EB),
            onPrimary = Color(0xFFFFFFFF),
            secondary = Color(0xFF0F766E),
            onSecondary = Color(0xFFFFFFFF),
            background = Color(0xFFF8FAFC),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF111827),
            outline = Color(0xFFCBD5E1),
            positive = Color(0xFF15803D),
            negative = Color(0xFFB91C1C),
        )

    val dark =
        TioColorScheme(
            primary = Color(0xFF93C5FD),
            onPrimary = Color(0xFF0F172A),
            secondary = Color(0xFF5EEAD4),
            onSecondary = Color(0xFF042F2E),
            background = Color(0xFF0B1120),
            onBackground = Color(0xFFE5E7EB),
            surface = Color(0xFF111827),
            onSurface = Color(0xFFF9FAFB),
            outline = Color(0xFF334155),
            positive = Color(0xFF86EFAC),
            negative = Color(0xFFFCA5A5),
        )
}

data class TioColorScheme(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val outline: Color,
    val positive: Color,
    val negative: Color,
)
