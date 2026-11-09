package com.tioledger.ui.transactions

import com.tioledger.application.usecase.currency.GetMainCurrencyConfigUseCase
import com.tioledger.application.usecase.currency.SaveMainCurrencyConfigUseCase
import com.tioledger.application.usecase.transaction.ListTransactionsUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.model.SYSTEM_SUPPORTED_CURRENCIES
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.CurrencyPreferenceRepository
import com.tioledger.domain.repository.TransactionHistoryRepository
import com.tioledger.ui.more.CurrencyPreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionsViewModelTest {
    @Test
    fun loadTransactionsBuildsNewestFirstRows() {
        val repository =
            FakeTransactionHistoryRepository(
                LedgerResult.Success(
                    listOf(
                        singleSplitRecord(
                            id = "expense",
                            timestamp = 0L,
                            type = TransactionType.EXPENSE,
                            amount = 2_500L,
                            accountName = "Cash",
                            categoryName = "Food",
                            description = "Lunch",
                        ),
                        singleSplitRecord(
                            id = "income",
                            timestamp = DAY_MILLIS,
                            type = TransactionType.INCOME,
                            amount = 12_345L,
                            accountName = "Bank",
                            categoryName = "Salary",
                            description = "Monthly salary",
                        ),
                    ),
                ),
            )

        val state = TransactionsViewModel(ListTransactionsUseCase(repository)).uiState.value

        assertFalse(state.isLoading)
        assertEquals(listOf("income", "expense"), state.transactions.map { it.id })
        assertEquals("Salary", state.transactions.first().title)
        assertEquals("+USD 123.45", state.transactions.first().amount)
        assertEquals("1970-01-02 • Bank • Monthly salary", state.transactions.first().subtitle)
        assertEquals("-USD 25.00", state.transactions.last().amount)
    }

    @Test
    fun transferRowUsesPersistedLedgerDirection() {
        val repository =
            FakeTransactionHistoryRepository(
                LedgerResult.Success(
                    listOf(
                        TransactionHistoryRecord(
                            id = "transfer",
                            timestamp = 0L,
                            description = "Wallet top-up",
                            type = TransactionType.TRANSFER,
                            splits =
                                listOf(
                                    historySplit(
                                        id = "source",
                                        accountId = "cash",
                                        accountName = "Cash",
                                        accountType = AccountType.CASH,
                                        amount = 5_000L,
                                        entryType = LedgerEntryType.CREDIT,
                                    ),
                                    historySplit(
                                        id = "destination",
                                        accountId = "bank",
                                        accountName = "Bank",
                                        accountType = AccountType.BANK,
                                        amount = 5_000L,
                                        entryType = LedgerEntryType.DEBIT,
                                    ),
                                ),
                        ),
                    ),
                ),
            )

        val row = TransactionsViewModel(ListTransactionsUseCase(repository)).uiState.value.transactions.single()

        assertEquals("Cash → Bank", row.title)
        assertEquals("USD 50.00", row.amount)
        assertEquals("1970-01-01 • Transfer • Wallet top-up", row.subtitle)
    }

    @Test
    fun repositoryFailureAndRetryAreVisible() {
        val repository = FakeTransactionHistoryRepository(LedgerResult.Failure(LedgerError.StorageUnavailable))
        val viewModel = TransactionsViewModel(ListTransactionsUseCase(repository))

        assertEquals("Unable to load transactions.", viewModel.uiState.value.errorMessage)

        repository.result = LedgerResult.Success(emptyList())
        viewModel.onAction(TransactionsAction.Retry)

        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun preferredCurrencySymbolIsOnlyUsedForMatchingTransactionCurrency() {
        val inr =
            singleSplitRecord("inr", 2L, TransactionType.EXPENSE, 12_345L, "Cash INR", "Food", "Meal")
                .copy(
                    splits =
                        listOf(
                            historySplit(
                                "split-inr",
                                "inr",
                                "Cash INR",
                                AccountType.CASH,
                                12_345L,
                                categoryId = "food",
                                categoryName = "Food",
                                entryType = LedgerEntryType.CREDIT,
                            ).copy(amount = Money(12_345L, CurrencyCode("INR"))),
                        ),
                )
        val usd =
            singleSplitRecord("usd", 1L, TransactionType.EXPENSE, 6_789L, "Cash USD", "Food", "Meal")
                .copy(
                    splits =
                        listOf(
                            historySplit(
                                "split-usd",
                                "usd",
                                "Cash USD",
                                AccountType.CASH,
                                6_789L,
                                categoryId = "food",
                                categoryName = "Food",
                                entryType = LedgerEntryType.CREDIT,
                            ),
                        ),
                )
        val repository = FakeTransactionHistoryRepository(LedgerResult.Success(listOf(inr, usd)))
        val currencyRepository =
            object : CurrencyPreferenceRepository {
                override fun getConfig(): MainCurrencyConfig = MainCurrencyConfig(currency = SYSTEM_SUPPORTED_CURRENCIES.first())

                override fun saveConfig(config: MainCurrencyConfig): Boolean = true
            }
        val currencyStore =
            CurrencyPreferenceStore(
                GetMainCurrencyConfigUseCase(currencyRepository),
                SaveMainCurrencyConfigUseCase(currencyRepository),
            )

        val rows =
            TransactionsViewModel(
                ListTransactionsUseCase(repository),
                currencyPreferenceStore = currencyStore,
            ).uiState.value.transactions

        assertEquals(listOf("-₹ 123.45", "-USD 67.89"), rows.map { it.amount })
    }
}

private fun singleSplitRecord(
    id: String,
    timestamp: Long,
    type: TransactionType,
    amount: Long,
    accountName: String,
    categoryName: String,
    description: String,
): TransactionHistoryRecord =
    TransactionHistoryRecord(
        id = id,
        timestamp = timestamp,
        description = description,
        type = type,
        splits =
            listOf(
                historySplit(
                    id = "split-$id",
                    accountId = "account-$id",
                    accountName = accountName,
                    accountType = AccountType.CASH,
                    amount = amount,
                    categoryId = "category-$id",
                    categoryName = categoryName,
                    entryType =
                        if (type == TransactionType.INCOME) {
                            LedgerEntryType.DEBIT
                        } else {
                            LedgerEntryType.CREDIT
                        },
                ),
            ),
    )

private fun historySplit(
    id: String,
    accountId: String,
    accountName: String,
    accountType: AccountType,
    amount: Long,
    categoryId: String? = null,
    categoryName: String? = null,
    entryType: LedgerEntryType,
): TransactionHistorySplit =
    TransactionHistorySplit(
        id = id,
        accountId = accountId,
        accountName = accountName,
        accountType = accountType,
        amount = Money(amount, CurrencyCode("USD")),
        categoryId = categoryId,
        categoryName = categoryName,
        entryType = entryType,
    )

private class FakeTransactionHistoryRepository(
    var result: LedgerResult<List<TransactionHistoryRecord>>,
) : TransactionHistoryRepository {
    override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> = result
}

private const val DAY_MILLIS = 86_400_000L
