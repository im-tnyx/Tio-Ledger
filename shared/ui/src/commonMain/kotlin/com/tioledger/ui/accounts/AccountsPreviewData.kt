package com.tioledger.ui.accounts

import com.tioledger.domain.model.AccountType

object AccountsPreviewData {
    val empty =
        AccountsUiState(
            isLoading = false,
            summary = AccountsSummaryUiModel("INR 0.00", "INR 0.00", "INR 0.00", "INR"),
        )

    val oneAccount =
        AccountsUiState(
            isLoading = false,
            summary = AccountsSummaryUiModel("INR 12,500.00", "INR 0.00", "INR 12,500.00", "INR"),
            groups =
                listOf(
                    AccountGroupUiModel(
                        type = AccountType.CASH,
                        title = "Cash",
                        total = "INR 12,500.00",
                        accounts =
                            listOf(
                                AccountRowUiModel(
                                    id = "cash",
                                    name = "Cash",
                                    typeLabel = "Cash",
                                    balance = "INR 12,500.00",
                                    currencyCode = "INR",
                                    isLiability = false,
                                ),
                            ),
                    ),
                ),
        )

    val multipleAccounts =
        AccountsUiState(
            isLoading = false,
            summary = AccountsSummaryUiModel("INR 1,42,300.00", "INR 18,450.00", "INR 1,23,850.00", "INR"),
            groups =
                listOf(
                    AccountGroupUiModel(
                        type = AccountType.CASH,
                        title = "Cash",
                        total = "INR 5,500.00",
                        accounts =
                            listOf(
                                AccountRowUiModel("cash", "Cash", "Cash", "INR 5,500.00", "INR", false),
                            ),
                    ),
                    AccountGroupUiModel(
                        type = AccountType.BANK,
                        title = "Accounts",
                        total = "INR 1,36,800.00",
                        accounts =
                            listOf(
                                AccountRowUiModel("sbi", "SBI", "Accounts", "INR 82,400.00", "INR", false),
                                AccountRowUiModel("icici", "ICICI", "Accounts", "INR 54,400.00", "INR", false),
                            ),
                    ),
                    AccountGroupUiModel(
                        type = AccountType.CREDIT_CARD,
                        title = "Credit Card",
                        total = "INR 18,450.00",
                        accounts =
                            listOf(
                                AccountRowUiModel("card", "HDFC Card", "Credit Card", "INR 18,450.00", "INR", true),
                            ),
                    ),
                ),
        )

    val largeBalance =
        multipleAccounts.copy(
            summary =
                AccountsSummaryUiModel(
                    assets = "INR 98,76,54,321.00",
                    liabilities = "INR 12,34,567.00",
                    total = "INR 98,64,19,754.00",
                    currencyLabel = "INR",
                ),
        )
}
