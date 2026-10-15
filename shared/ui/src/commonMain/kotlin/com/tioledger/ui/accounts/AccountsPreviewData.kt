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
                        totals = listOf("INR 12,500.00"),
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
                        totals = listOf("INR 5,500.00"),
                        accounts =
                            listOf(
                                AccountRowUiModel("cash", "Cash", "Cash", "INR 5,500.00", "INR", false),
                            ),
                    ),
                    AccountGroupUiModel(
                        type = AccountType.BANK,
                        title = "Accounts",
                        totals = listOf("INR 1,36,800.00"),
                        accounts =
                            listOf(
                                AccountRowUiModel("sbi", "SBI", "Accounts", "INR 82,400.00", "INR", false),
                                AccountRowUiModel("icici", "ICICI", "Accounts", "INR 54,400.00", "INR", false),
                            ),
                    ),
                    AccountGroupUiModel(
                        type = AccountType.CREDIT_CARD,
                        title = "Credit Card",
                        totals = listOf("INR 18,450.00"),
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

    // Display-only fixtures; no repositories, ViewModel actions or persisted accounts.
    val mixedCurrency =
        AccountsUiState(
            isLoading = false,
            summary = AccountsSummaryUiModel("INR 1,250.00", "INR 180.00", "INR 1,070.00", "Multiple currencies"),
            groups =
                listOf(
                    AccountGroupUiModel(
                        type = AccountType.CASH,
                        title = "Cash",
                        totals = listOf("INR 1,250.00", "USD 250.00"),
                        accounts =
                            listOf(
                                AccountRowUiModel("cash-inr", "India Cash", "Cash", "INR 1,250.00", "INR", false),
                                AccountRowUiModel("cash-usd", "Travel Cash", "Cash", "USD 250.00", "USD", false),
                            ),
                    ),
                    AccountGroupUiModel(
                        type = AccountType.CREDIT_CARD,
                        title = "Credit Card",
                        totals = listOf("INR 180.00", "USD 80.00"),
                        accounts =
                            listOf(
                                AccountRowUiModel("card-inr", "India Card", "Credit Card", "INR 180.00", "INR", true),
                                AccountRowUiModel("card-usd", "Travel Card", "Credit Card", "USD 80.00", "USD", true),
                            ),
                    ),
                ),
        )

    val mixedCurrencyZero =
        mixedCurrency.copy(
            summary = AccountsSummaryUiModel("INR 0.00", "INR 0.00", "INR 0.00", "Multiple currencies"),
            groups =
                mixedCurrency.groups.map { group ->
                    group.copy(
                        totals = listOf("INR 0.00", "USD 0.00"),
                        accounts = group.accounts.map { it.copy(balance = "${it.currencyCode} 0.00") },
                    )
                },
        )

    val mixedCurrencyLargeSigned =
        mixedCurrency.copy(
            summary =
                AccountsSummaryUiModel(
                    assets = "-INR 1234567890.12",
                    liabilities = "INR 0.00",
                    total = "-INR 1234567890.12",
                    currencyLabel = "Multiple currencies",
                ),
            groups =
                listOf(
                    mixedCurrency.groups.first().copy(
                        totals = listOf("-INR 1234567890.12", "USD 9876543210.98"),
                        accounts =
                            listOf(
                                AccountRowUiModel(
                                    "cash-inr",
                                    "Long negative India Cash balance",
                                    "Cash",
                                    "-INR 1234567890.12",
                                    "INR",
                                    false,
                                ),
                                AccountRowUiModel(
                                    "cash-usd",
                                    "Long positive Travel Cash balance",
                                    "Cash",
                                    "USD 9876543210.98",
                                    "USD",
                                    false,
                                ),
                            ),
                    ),
                ),
        )
}
