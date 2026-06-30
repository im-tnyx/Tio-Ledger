package com.tioledger.application.usecase.transaction

import com.tioledger.application.internal.normalizedId
import com.tioledger.application.internal.validateId
import com.tioledger.application.internal.validateOptionalId
import com.tioledger.application.internal.validateTimestamp
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.event.DomainEvent
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.CategoryRepository
import com.tioledger.domain.repository.TransactionRepository
import com.tioledger.finance.engine.PostedTransaction
import com.tioledger.finance.engine.PostingEngine

data class RecordIncomeCommand(
    val timestamp: Long,
    val description: String?,
    val amount: Money,
    val accountId: String,
    val categoryId: String?,
    val merchantId: String?,
    val createdAt: Long,
)

class RecordIncomeUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val postingEngine: PostingEngine,
) {
    operator fun invoke(command: RecordIncomeCommand): ApplicationResult<TransactionRecord> {
        validateTransactionCommand(command.accountId, command.categoryId, command.timestamp, command.createdAt)?.let {
            return ApplicationResult.Failure(it)
        }

        val accountId = normalizedId(command.accountId)
        val categoryId = command.categoryId?.let(::normalizedId)
        val account = accountRepository.findAccount(accountId) ?: return accountRepository.failure(accountId)
        val category = categoryId?.let { categoryRepository.findCategory(it) ?: return categoryRepository.failure(it) }

        val posted =
            when (
                val result =
                    postingEngine.postIncome(
                        timestamp = command.timestamp,
                        description = command.description,
                        amount = command.amount,
                        account = account,
                        category = category,
                        merchantId = command.merchantId,
                        createdAt = command.createdAt,
                    )
            ) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Ledger(result.error))
            }

        return transactionRepository.recordPostedTransaction(posted, command.createdAt)
    }
}

data class RecordExpenseCommand(
    val timestamp: Long,
    val description: String?,
    val amount: Money,
    val accountId: String,
    val categoryId: String?,
    val merchantId: String?,
    val createdAt: Long,
)

class RecordExpenseUseCase(
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val postingEngine: PostingEngine,
) {
    operator fun invoke(command: RecordExpenseCommand): ApplicationResult<TransactionRecord> {
        validateTransactionCommand(command.accountId, command.categoryId, command.timestamp, command.createdAt)?.let {
            return ApplicationResult.Failure(it)
        }

        val accountId = normalizedId(command.accountId)
        val categoryId = command.categoryId?.let(::normalizedId)
        val account = accountRepository.findAccount(accountId) ?: return accountRepository.failure(accountId)
        val category = categoryId?.let { categoryRepository.findCategory(it) ?: return categoryRepository.failure(it) }

        val posted =
            when (
                val result =
                    postingEngine.postExpense(
                        timestamp = command.timestamp,
                        description = command.description,
                        amount = command.amount,
                        account = account,
                        category = category,
                        merchantId = command.merchantId,
                        createdAt = command.createdAt,
                    )
            ) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Ledger(result.error))
            }

        return transactionRepository.recordPostedTransaction(posted, command.createdAt)
    }
}

data class RecordTransferCommand(
    val timestamp: Long,
    val description: String?,
    val amount: Money,
    val sourceAccountId: String,
    val targetAccountId: String,
    val createdAt: Long,
)

class RecordTransferUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val postingEngine: PostingEngine,
) {
    operator fun invoke(command: RecordTransferCommand): ApplicationResult<TransactionRecord> {
        validateId(command.sourceAccountId, "sourceAccountId")?.let { return ApplicationResult.Failure(it) }
        validateId(command.targetAccountId, "targetAccountId")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.timestamp, "timestamp")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.createdAt, "createdAt")?.let { return ApplicationResult.Failure(it) }

        val sourceAccountId = normalizedId(command.sourceAccountId)
        val targetAccountId = normalizedId(command.targetAccountId)
        val sourceAccount =
            accountRepository.findAccount(sourceAccountId)
                ?: return accountRepository.failure(sourceAccountId)
        val targetAccount =
            accountRepository.findAccount(targetAccountId)
                ?: return accountRepository.failure(targetAccountId)

        val posted =
            when (
                val result =
                    postingEngine.postTransfer(
                        timestamp = command.timestamp,
                        description = command.description,
                        amount = command.amount,
                        sourceAccount = sourceAccount,
                        targetAccount = targetAccount,
                        createdAt = command.createdAt,
                    )
            ) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Ledger(result.error))
            }

        return transactionRepository.recordPostedTransaction(posted, command.createdAt)
    }
}

data class RecordAdjustmentCommand(
    val timestamp: Long,
    val description: String?,
    val amount: Money,
    val accountId: String,
    val isDebit: Boolean,
    val createdAt: Long,
)

class RecordAdjustmentUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val postingEngine: PostingEngine,
) {
    operator fun invoke(command: RecordAdjustmentCommand): ApplicationResult<TransactionRecord> {
        validateId(command.accountId, "accountId")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.timestamp, "timestamp")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.createdAt, "createdAt")?.let { return ApplicationResult.Failure(it) }

        val accountId = normalizedId(command.accountId)
        val account = accountRepository.findAccount(accountId) ?: return accountRepository.failure(accountId)
        val posted =
            when (
                val result =
                    postingEngine.postAdjustment(
                        timestamp = command.timestamp,
                        description = command.description,
                        amount = command.amount,
                        account = account,
                        isDebit = command.isDebit,
                        createdAt = command.createdAt,
                    )
            ) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Ledger(result.error))
            }

        return transactionRepository.recordPostedTransaction(posted, command.createdAt)
    }
}

data class RecordOpeningBalanceCommand(
    val timestamp: Long,
    val description: String?,
    val amount: Money,
    val accountId: String,
    val createdAt: Long,
)

class RecordOpeningBalanceUseCase(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val postingEngine: PostingEngine,
) {
    operator fun invoke(command: RecordOpeningBalanceCommand): ApplicationResult<TransactionRecord> {
        validateId(command.accountId, "accountId")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.timestamp, "timestamp")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.createdAt, "createdAt")?.let { return ApplicationResult.Failure(it) }

        val accountId = normalizedId(command.accountId)
        val account = accountRepository.findAccount(accountId) ?: return accountRepository.failure(accountId)
        val posted =
            when (
                val result =
                    postingEngine.postOpeningBalance(
                        timestamp = command.timestamp,
                        description = command.description,
                        amount = command.amount,
                        account = account,
                        createdAt = command.createdAt,
                    )
            ) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Ledger(result.error))
            }

        return transactionRepository.recordPostedTransaction(posted, command.createdAt)
    }
}

private fun validateTransactionCommand(
    accountId: String,
    categoryId: String?,
    timestamp: Long,
    createdAt: Long,
): ApplicationError.Validation? {
    validateId(accountId, "accountId")?.let { return it }
    validateOptionalId(categoryId, "categoryId")?.let { return it }
    validateTimestamp(timestamp, "timestamp")?.let { return it }
    validateTimestamp(createdAt, "createdAt")?.let { return it }
    return null
}

private fun AccountRepository.findAccount(accountId: String): Account? =
    when (val result = findById(accountId)) {
        is LedgerResult.Success -> result.value
        is LedgerResult.Failure -> null
    }

private fun AccountRepository.failure(accountId: String): ApplicationResult.Failure =
    when (val result = findById(accountId)) {
        is LedgerResult.Success ->
            ApplicationResult.Failure(
                ApplicationError.Validation("accountId", "lookup unexpectedly succeeded for $accountId"),
            )
        is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(result.error))
    }

private fun CategoryRepository.findCategory(categoryId: String): Category? =
    when (val result = findById(categoryId)) {
        is LedgerResult.Success -> result.value
        is LedgerResult.Failure -> null
    }

private fun CategoryRepository.failure(categoryId: String): ApplicationResult.Failure =
    when (val result = findById(categoryId)) {
        is LedgerResult.Success ->
            ApplicationResult.Failure(
                ApplicationError.Validation("categoryId", "lookup unexpectedly succeeded for $categoryId"),
            )
        is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(result.error))
    }

private fun TransactionRepository.recordPostedTransaction(
    posted: PostedTransaction,
    occurredAt: Long,
): ApplicationResult<TransactionRecord> {
    val record = posted.toTransactionRecord()
    return when (val result = record(record)) {
        is LedgerResult.Success ->
            ApplicationResult.Success(
                UseCaseOutcome(
                    value = result.value,
                    events =
                        listOf(
                            DomainEvent.TransactionRecorded(
                                transactionId = result.value.transaction.id,
                                transactionType = result.value.transaction.type,
                                occurredAt = occurredAt,
                            ),
                        ),
                ),
            )
        is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(result.error))
    }
}

private fun PostedTransaction.toTransactionRecord(): TransactionRecord =
    TransactionRecord(
        transaction = transaction,
        splits = splits,
        ledgerEntries = entries,
    )
