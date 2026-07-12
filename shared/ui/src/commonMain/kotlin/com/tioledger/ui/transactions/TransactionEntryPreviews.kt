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
                    amount = "1250.00",
                    note = "Lunch",
                    selectedDate = "Today",
                    accountOptions =
                        listOf(
                            TransactionAccountOption(
                                id = "cash",
                                name = "Cash",
                                currencyCode = "INR",
                                subtitle = "Cash • INR 1250.00",
                            ),
                        ),
                    categoryOptions =
                        listOf(
                            TransactionCategoryOption(
                                id = "food",
                                name = "Food",
                                subtitle = "expense",
                            ),
                        ),
                    selectedAccountId = "cash",
                    selectedCategoryId = "food",
                    isLoading = false,
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
                    transactionType = TransactionType.Transfer,
                    amount = "4500.00",
                    note = "Wallet top-up",
                    selectedDate = "Today",
                    accountOptions =
                        listOf(
                            TransactionAccountOption(
                                id = "bank",
                                name = "Bank",
                                currencyCode = "INR",
                                subtitle = "Bank • INR 25000.00",
                            ),
                            TransactionAccountOption(
                                id = "wallet",
                                name = "Wallet",
                                currencyCode = "INR",
                                subtitle = "Wallet • INR 4500.00",
                            ),
                        ),
                    selectedAccountId = "bank",
                    selectedTargetAccountId = "wallet",
                    isLoading = false,
                ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
