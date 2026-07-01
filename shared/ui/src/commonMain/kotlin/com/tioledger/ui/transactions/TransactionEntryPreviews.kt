@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.transactions

import androidx.compose.runtime.Composable
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun TransactionEntryLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        TransactionEntryScreen(
            state =
                TransactionEntryUiState(
                    transactionType = TransactionType.Expense,
                    amount = "1250",
                    selectedAccount = "Cash",
                    selectedCategory = "Food",
                    selectedDate = "Today",
                    note = "Lunch",
                    canSave = true,
                ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun TransactionEntryDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        TransactionEntryScreen(
            state =
                TransactionEntryUiState(
                    transactionType = TransactionType.Income,
                    amount = "45000",
                    selectedAccount = "Bank",
                    selectedCategory = "Salary",
                    selectedDate = "Today",
                    note = "Monthly salary",
                    canSave = true,
                ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
