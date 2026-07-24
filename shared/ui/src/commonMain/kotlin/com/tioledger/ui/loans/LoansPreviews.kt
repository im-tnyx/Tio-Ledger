@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.loans

import androidx.compose.runtime.Composable
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerClass
import com.tioledger.ui.design.TioLedgerTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun LoansLightPreview() {
    TioLedgerTheme(darkTheme = false) {
        LoansScreen(
            state = LoansPreviewData.populated,
            onAction = {},
            onOpenDetails = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun LoansDarkPreview() {
    TioLedgerTheme(darkTheme = true) {
        LoansScreen(
            state = LoansPreviewData.populated,
            onAction = {},
            onOpenDetails = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun LoansEmptyPreview() {
    TioLedgerTheme(darkTheme = false) {
        LoansScreen(
            state = LoansUiState(isLoading = false),
            onAction = {},
            onOpenDetails = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun LoanEditorPreview() {
    TioLedgerTheme(darkTheme = false) {
        LoansScreen(
            state =
                LoansPreviewData.populated.copy(
                    editor =
                        LoanEditorUiState(
                            name = "Home Loan",
                            principal = "2500000",
                            annualInterestRatePercent = "8.75",
                            tenureMonths = "240",
                            startDate = "2026-07-20",
                            loanAccountId = "loan-account",
                            disbursedAccountId = "bank-account",
                        ),
                ),
            onAction = {},
            onOpenDetails = {},
            onNavigate = {},
        )
    }
}

@Preview
@Composable
private fun LoanDetailsPreview() {
    TioLedgerTheme(darkTheme = false) {
        LoanDetailsScreen(
            state = LoanDetailsUiState(details = LoansPreviewData.details),
            onRetry = {},
            onNavigateBack = {},
        )
    }
}

private object LoansPreviewData {
    val accountOptions =
        listOf(
            LoanAccountOption(
                id = "loan-account",
                name = "Home Loan Liability",
                type = AccountType.LOAN_LINKED,
                ledgerClass = LedgerClass.LIABILITY,
                currencyCode = "INR",
            ),
            LoanAccountOption(
                id = "bank-account",
                name = "Salary Bank",
                type = AccountType.BANK,
                ledgerClass = LedgerClass.ASSET,
                currencyCode = "INR",
            ),
        )

    val populated =
        LoansUiState(
            isLoading = false,
            loans =
                listOf(
                    LoanRowUiModel(
                        id = "home-loan",
                        name = "Home Loan",
                        statusLabel = "Active",
                        principalLabel = "INR 2500000.00",
                        scheduledEmiLabel = "INR 22093.00",
                        outstandingLabel = "INR 2474300.00",
                        remainingInstallmentsLabel = "238 remaining",
                        nextDueDateLabel = "2026-08-20",
                    ),
                    LoanRowUiModel(
                        id = "education-loan",
                        name = "Education Loan",
                        statusLabel = "Active",
                        principalLabel = "INR 500000.00",
                        scheduledEmiLabel = "INR 10300.00",
                        outstandingLabel = "INR 420000.00",
                        remainingInstallmentsLabel = "44 remaining",
                        nextDueDateLabel = "2026-08-05",
                    ),
                ),
            accountOptions = accountOptions,
        )

    val details =
        LoanDetailsUiModel(
            id = "home-loan",
            name = "Home Loan",
            statusLabel = "Active",
            outstandingLabel = "INR 2474300.00",
            scheduledEmiLabel = "INR 22093.00",
            totalInterestLabel = "INR 2802320.00",
            totalPayableLabel = "INR 5302320.00",
            principalLabel = "INR 2500000.00",
            annualRateLabel = "8.75%",
            tenureLabel = "240 months",
            startDateLabel = "2026-07-20",
            loanAccountLabel = "Home Loan Liability",
            disbursedAccountLabel = "Salary Bank",
            remainingInstallmentsLabel = "238",
            nextDueDateLabel = "2026-08-20",
            schedule =
                listOf(
                    LoanInstallmentUiModel(
                        id = "installment-1",
                        installmentNumber = 1,
                        dueDateLabel = "2026-08-20",
                        paymentLabel = "INR 22093.00",
                        principalLabel = "INR 3864.00",
                        interestLabel = "INR 18229.00",
                        openingBalanceLabel = "INR 2500000.00",
                        closingBalanceLabel = "INR 2496136.00",
                        statusLabel = "Pending",
                    ),
                    LoanInstallmentUiModel(
                        id = "installment-2",
                        installmentNumber = 2,
                        dueDateLabel = "2026-09-20",
                        paymentLabel = "INR 22093.00",
                        principalLabel = "INR 3892.00",
                        interestLabel = "INR 18201.00",
                        openingBalanceLabel = "INR 2496136.00",
                        closingBalanceLabel = "INR 2492244.00",
                        statusLabel = "Pending",
                    ),
                ),
        )
}
