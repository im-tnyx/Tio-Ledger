@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.accounts

import androidx.compose.runtime.Composable
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun AccountsEmptyLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        AccountsScreen(state = AccountsPreviewData.empty, onAction = {})
    }
}

@Preview
@Composable
private fun AccountsOneAccountLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        AccountsScreen(state = AccountsPreviewData.oneAccount, onAction = {})
    }
}

@Preview
@Composable
private fun AccountsMultipleDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        AccountsScreen(state = AccountsPreviewData.multipleAccounts, onAction = {})
    }
}

@Preview
@Composable
private fun AccountsLargeBalancePreview() {
    TioLedgerTheme(darkTheme = false) {
        AccountsScreen(state = AccountsPreviewData.largeBalance, onAction = {})
    }
}
