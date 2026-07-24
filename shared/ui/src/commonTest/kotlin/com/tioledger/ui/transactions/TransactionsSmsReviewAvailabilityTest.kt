package com.tioledger.ui.transactions

import com.tioledger.application.usecase.transaction.ListTransactionsUseCase
import com.tioledger.core.feature.FeatureFlag
import com.tioledger.core.feature.StaticFeatureFlagProvider
import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.repository.TransactionHistoryRepository
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TransactionsSmsReviewAvailabilityTest {
    @Test
    fun SMSReviewEntryIsHiddenByDefault() {
        val viewModel =
            TransactionsViewModel(
                listTransactionsUseCase = ListTransactionsUseCase(EmptyTransactionHistoryRepository),
            )

        assertFalse(viewModel.uiState.value.smsReviewAvailable)
    }

    @Test
    fun SMSReviewEntryIsVisibleOnlyWhenFeatureIsEnabled() {
        val viewModel =
            TransactionsViewModel(
                listTransactionsUseCase = ListTransactionsUseCase(EmptyTransactionHistoryRepository),
                featureFlagProvider =
                    StaticFeatureFlagProvider(
                        setOf(FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW),
                    ),
            )

        assertTrue(viewModel.uiState.value.smsReviewAvailable)
    }
}

private data object EmptyTransactionHistoryRepository : TransactionHistoryRepository {
    override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> = LedgerResult.Success(emptyList())
}
