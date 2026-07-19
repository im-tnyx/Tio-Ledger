package com.tioledger.application.usecase.transaction

import com.tioledger.application.model.ApplicationResult
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.TransactionHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ListTransactionsUseCaseTest {
    @Test
    fun returnsNewestFirstSummariesWithTransferDirection() {
        val repository =
            FakeTransactionHistoryRepository(
                records =
                    listOf(
                        historyRecord(
                            id = "income",
                            timestamp = 1_000L,
                            type = TransactionType.INCOME,
                            splits =
                                listOf(
                                    historySplit(
                                        id = "income-split",
                                        accountId = "bank",
                                        accountName = "Bank",
                                        entryType = LedgerEntryType.DEBIT,
                                        categoryId = "salary",
                                        categoryName = "Salary",
                                    ),
                                ),
                        ),
                        historyRecord(
                            id = "transfer",
                            timestamp = 2_000L,
                            type = TransactionType.TRANSFER,
                            splits =
                                listOf(
                                    historySplit(
                                        id = "target-split",
                                        accountId = "cash",
                                        accountName = "Cash",
                                        entryType = LedgerEntryType.DEBIT,
                                    ),
                                    historySplit(
                                        id = "source-split",
                                        accountId = "bank",
                                        accountName = "Bank",
                                        entryType = LedgerEntryType.CREDIT,
                                    ),
                                ),
                        ),
                    ),
            )

        val result = ListTransactionsUseCase(repository)()
        val summaries = assertIs<ApplicationResult.Success<List<TransactionSummary>>>(result).outcome.value

        assertEquals(listOf("transfer", "income"), summaries.map { it.id })
        assertEquals("Bank", summaries.first().sourceAccountName)
        assertEquals("Cash", summaries.first().destinationAccountName)
        assertEquals("Salary", summaries.last().categoryName)
    }
}

private class FakeTransactionHistoryRepository(
    private val records: List<TransactionHistoryRecord>,
) : TransactionHistoryRepository {
    override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> = LedgerResult.Success(records)
}

private fun historyRecord(
    id: String,
    timestamp: Long,
    type: TransactionType,
    splits: List<TransactionHistorySplit>,
): TransactionHistoryRecord =
    TransactionHistoryRecord(
        id = id,
        timestamp = timestamp,
        description = id,
        type = type,
        splits = splits,
    )

private fun historySplit(
    id: String,
    accountId: String,
    accountName: String,
    entryType: LedgerEntryType,
    categoryId: String? = null,
    categoryName: String? = null,
): TransactionHistorySplit =
    TransactionHistorySplit(
        id = id,
        accountId = accountId,
        accountName = accountName,
        accountType = AccountType.BANK,
        amount = Money(1_000L, CurrencyCode("USD")),
        categoryId = categoryId,
        categoryName = categoryName,
        entryType = entryType,
    )
