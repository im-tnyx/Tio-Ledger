@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.more

import androidx.compose.runtime.Composable
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun MoreScreenLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        MoreScreen(onNavigate = {})
    }
}

@Preview
@Composable
private fun MoreScreenDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        MoreScreen(onNavigate = {})
    }
}
