@file:Suppress("FunctionName")

package com.tioledger.ui.shell

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import com.tioledger.ui.design.TioLedgerTheme

@Composable
fun TioRootScaffold(
    darkTheme: Boolean,
    content: @Composable (PaddingValues) -> Unit,
) {
    TioLedgerTheme(darkTheme = darkTheme) {
        Scaffold(content = content)
    }
}
