@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.transactions

import androidx.compose.runtime.Composable
import com.tioledger.domain.model.TransactionType
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun TransactionsLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        TransactionsScreen(
            state = previewTransactionsState(),
            onAction = {},
            onAddTransaction = {},
            onReviewSms = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun TransactionsDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        TransactionsScreen(
            state = previewTransactionsState(),
            onAction = {},
            onAddTransaction = {},
            onReviewSms = {},
            onNavigate = {},
        )
    }
}

private fun previewTransactionsState(): TransactionsUiState =
    TransactionsUiState(
        isLoading = false,
        smsReviewAvailable = true,
        transactions =
            listOf(
                TransactionRowUiModel(
                    id = "expense",
                    title = "Food",
                    subtitle = "2026-07-19 • Cash • Lunch",
                    amount = "-INR 250.00",
                    type = TransactionType.EXPENSE,
                ),
                TransactionRowUiModel(
                    id = "income",
                    title = "Salary",
                    subtitle = "2026-07-18 • Bank",
                    amount = "+INR 45,000.00",
                    type = TransactionType.INCOME,
                ),
                TransactionRowUiModel(
                    id = "transfer",
                    title = "Bank → Wallet",
                    subtitle = "2026-07-17 • Transfer • Wallet top-up",
                    amount = "INR 4,500.00",
                    type = TransactionType.TRANSFER,
                ),
            ),
    )
