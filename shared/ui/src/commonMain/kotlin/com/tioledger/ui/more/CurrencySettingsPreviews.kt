@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.more

import androidx.compose.runtime.Composable
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun CurrencySettingsScreenLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        CurrencySettingsScreen(
            state = CurrencySettingsUiState(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun CurrencySettingsScreenDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        CurrencySettingsScreen(
            state = CurrencySettingsUiState(),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
