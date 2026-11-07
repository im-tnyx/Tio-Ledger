@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.more

import androidx.compose.runtime.Composable
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun AccountGroupScreenLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        AccountGroupScreen(
            onNavigate = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun AccountGroupScreenDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        AccountGroupScreen(
            onNavigate = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun AccountGroupScreenWithCustomGroupsPreview() {
    val sampleGroups =
        DEFAULT_ACCOUNT_GROUPS +
            listOf(
                AccountGroupEntry(
                    id = "top_up",
                    title = "Top-Up/Prepaid",
                    subtitle = "Other",
                    policy = AccountGroupPolicy.OTHER,
                    isPermanent = false,
                ),
                AccountGroupEntry(
                    id = "investments",
                    title = "Investments",
                    subtitle = "Investments",
                    policy = AccountGroupPolicy.INVESTMENT,
                    isPermanent = false,
                ),
            )
    TioLedgerTheme(darkTheme = false) {
        AccountGroupScreen(
            onNavigate = {},
            onNavigateBack = {},
            initialGroups = sampleGroups,
        )
    }
}
