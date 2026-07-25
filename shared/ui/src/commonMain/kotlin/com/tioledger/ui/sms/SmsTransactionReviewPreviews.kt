@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.sms

import androidx.compose.runtime.Composable
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseConfidence
import com.tioledger.domain.model.SmsPaymentRail
import com.tioledger.domain.model.SmsTransactionDirection
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun SmsTransactionReviewInputLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        SmsTransactionReviewScreen(
            state =
                SmsTransactionReviewUiState(
                    messageInput = "INR 1,250.50 debited from account 1234 at Grocery Mart",
                ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}

@Preview
@Composable
private fun SmsTransactionReviewLowConfidenceDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        SmsTransactionReviewScreen(
            state =
                SmsTransactionReviewUiState(
                    stage = SmsReviewStage.REVIEW,
                    confidence = SmsParseConfidence.LOW,
                    direction = SmsTransactionDirection.EXPENSE,
                    amount = "1250.50",
                    currencyCode = "INR",
                    occurredAt = 1_721_800_000_000L,
                    selectedDate = "2024-07-24",
                    accountOptions =
                        listOf(
                            SmsReviewAccountUiModel(
                                id = "bank-1234",
                                name = "Everyday account",
                                type = AccountType.BANK,
                                currencyCode = "INR",
                            ),
                        ),
                    categoryOptions =
                        listOf(
                            SmsReviewCategoryUiModel(
                                id = "food",
                                name = "Food",
                                type = CategoryType.EXPENSE,
                            ),
                        ),
                    selectedAccountId = "bank-1234",
                    note = "Grocery Mart",
                    paymentRail = SmsPaymentRail.CARD,
                    unresolvedFields = listOf(SmsMissingField.CATEGORY, SmsMissingField.TRANSACTION_TIME),
                    evidence =
                        listOf(
                            SmsReviewEvidenceUiModel("Amount", "INR 1,250.50"),
                            SmsReviewEvidenceUiModel("Account hint", "1234"),
                            SmsReviewEvidenceUiModel("Payment rail", "Card"),
                        ),
                ),
            onAction = {},
            onNavigateBack = {},
        )
    }
}
