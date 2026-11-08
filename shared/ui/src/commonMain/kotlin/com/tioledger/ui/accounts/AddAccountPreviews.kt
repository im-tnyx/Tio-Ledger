@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.accounts

import androidx.compose.runtime.Composable
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun AddAccountScreenLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        AddAccountScreen(
            onNavigateBack = {},
            onAccountCreated = {},
            currencyCode = MainCurrencyConfig().currency.code.normalized,
        )
    }
}

@Preview
@Composable
private fun AddAccountScreenDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        AddAccountScreen(
            onNavigateBack = {},
            onAccountCreated = {},
            currencyCode = MainCurrencyConfig().currency.code.normalized,
        )
    }
}
