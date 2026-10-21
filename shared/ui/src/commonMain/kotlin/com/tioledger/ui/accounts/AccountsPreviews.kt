@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.accounts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun AccountsEmptyLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        AccountsScreen(state = AccountsPreviewData.empty, onAction = {}, onNavigate = {})
    }
}

@Preview
@Composable
private fun AccountsOneAccountLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        AccountsScreen(state = AccountsPreviewData.oneAccount, onAction = {}, onNavigate = {})
    }
}

@Preview
@Composable
private fun AccountsMultipleDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        AccountsScreen(state = AccountsPreviewData.multipleAccounts, onAction = {}, onNavigate = {})
    }
}

@Preview
@Composable
private fun AccountsLargeBalancePreview() {
    TioLedgerTheme(darkTheme = false) {
        AccountsScreen(state = AccountsPreviewData.largeBalance, onAction = {}, onNavigate = {})
    }
}

@Preview
@Composable
private fun AccountsMixedCurrencyLightPreview() {
    AccountsCurrencyGroupPreview(AccountsPreviewData.mixedCurrency, darkTheme = false)
}

@Preview
@Composable
private fun AccountsMixedCurrencyDarkPreview() {
    AccountsCurrencyGroupPreview(AccountsPreviewData.mixedCurrency, darkTheme = true)
}

@Preview
@Composable
private fun AccountsMixedCurrencyLargeTextLightPreview() {
    AccountsCurrencyGroupPreview(AccountsPreviewData.mixedCurrency, darkTheme = false, fontScale = 2f, width = 320.dp)
}

@Preview
@Composable
private fun AccountsMixedCurrencyLargeTextDarkPreview() {
    AccountsCurrencyGroupPreview(AccountsPreviewData.mixedCurrency, darkTheme = true, fontScale = 2f, width = 320.dp)
}

@Preview
@Composable
private fun AccountsMixedCurrencyZeroLightPreview() {
    AccountsCurrencyGroupPreview(AccountsPreviewData.mixedCurrencyZero, darkTheme = false)
}

@Preview
@Composable
private fun AccountsMixedCurrencyZeroDarkPreview() {
    AccountsCurrencyGroupPreview(AccountsPreviewData.mixedCurrencyZero, darkTheme = true)
}

@Preview
@Composable
private fun AccountsMixedCurrencyLargeSignedLightPreview() {
    AccountsCurrencyGroupPreview(AccountsPreviewData.mixedCurrencyLargeSigned, darkTheme = false, fontScale = 2f, width = 320.dp)
}

@Preview
@Composable
private fun AccountsMixedCurrencyLargeSignedDarkPreview() {
    AccountsCurrencyGroupPreview(AccountsPreviewData.mixedCurrencyLargeSigned, darkTheme = true, fontScale = 2f, width = 320.dp)
}

@Composable
private fun AccountsCurrencyGroupPreview(
    state: AccountsUiState,
    darkTheme: Boolean,
    fontScale: Float = 1f,
    width: Dp = 360.dp,
) {
    TioLedgerTheme(darkTheme = darkTheme) {
        val density = LocalDensity.current
        // Preview-only linear font stress; not proof of platform large-font behavior.
        CompositionLocalProvider(
            LocalDensity provides Density(density = density.density, fontScale = fontScale),
        ) {
            Box(modifier = Modifier.size(width = width, height = 800.dp)) {
                AccountsScreen(state = state, onAction = {}, onNavigate = {})
            }
        }
    }
}
