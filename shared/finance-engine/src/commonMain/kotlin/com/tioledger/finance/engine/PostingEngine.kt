package com.tioledger.finance.engine

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.Transaction
import com.tioledger.domain.model.TransactionSplit
import com.tioledger.domain.model.TransactionType

data class PostingSummary(
    val debitTotal: Money,
    val creditTotal: Money,
    val isBalanced: Boolean,
    val currency: CurrencyCode,
)

data class PostedTransaction(
    val transaction: Transaction,
    val splits: List<TransactionSplit>,
    val entries: List<LedgerEntry>,
    val summary: PostingSummary,
)

class PostingEngine(
    private val idGenerator: IdGenerator,
    private val validator: PostingValidator = PostingValidator(),
) {
    private val strategyMap =
        mapOf(
            TransactionType.INCOME to IncomePostingStrategy(),
            TransactionType.EXPENSE to ExpensePostingStrategy(),
            TransactionType.TRANSFER to TransferPostingStrategy(),
            TransactionType.ADJUSTMENT to AdjustmentPostingStrategy(),
        )

    private val openingBalanceStrategy = OpeningBalancePostingStrategy()

    fun postIncome(
        timestamp: Long,
        description: String?,
        amount: Money,
        account: Account,
        category: Category?,
        merchantId: String?,
        createdAt: Long,
    ): LedgerResult<PostedTransaction> {
        validator.validateIncome(account, category, amount)?.let { return LedgerResult.Failure(it) }
        val strategy = strategyMap[TransactionType.INCOME] ?: return LedgerResult.Failure(LedgerError.InvalidTransactionType("INCOME"))
        val params = PostingParams.Income(account, category, merchantId)
        return executeStrategy(TransactionType.INCOME, timestamp, description, amount, params, strategy, createdAt)
    }

    fun postExpense(
        timestamp: Long,
        description: String?,
        amount: Money,
        account: Account,
        category: Category?,
        merchantId: String?,
        createdAt: Long,
    ): LedgerResult<PostedTransaction> {
        validator.validateExpense(account, category, amount)?.let { return LedgerResult.Failure(it) }
        val strategy = strategyMap[TransactionType.EXPENSE] ?: return LedgerResult.Failure(LedgerError.InvalidTransactionType("EXPENSE"))
        val params = PostingParams.Expense(account, category, merchantId)
        return executeStrategy(TransactionType.EXPENSE, timestamp, description, amount, params, strategy, createdAt)
    }

    fun postTransfer(
        timestamp: Long,
        description: String?,
        amount: Money,
        sourceAccount: Account,
        targetAccount: Account,
        createdAt: Long,
    ): LedgerResult<PostedTransaction> {
        validator.validateTransfer(sourceAccount, targetAccount, amount)?.let { return LedgerResult.Failure(it) }
        val strategy = strategyMap[TransactionType.TRANSFER] ?: return LedgerResult.Failure(LedgerError.InvalidTransactionType("TRANSFER"))
        val params = PostingParams.Transfer(sourceAccount, targetAccount)
        return executeStrategy(TransactionType.TRANSFER, timestamp, description, amount, params, strategy, createdAt)
    }

    fun postOpeningBalance(
        timestamp: Long,
        description: String?,
        amount: Money,
        account: Account,
        createdAt: Long,
    ): LedgerResult<PostedTransaction> {
        validator.validateOpeningBalance(account, amount)?.let { return LedgerResult.Failure(it) }
        val params = PostingParams.OpeningBalance(account)
        return executeStrategy(
            type = TransactionType.ADJUSTMENT,
            timestamp = timestamp,
            description = description,
            amount = amount,
            params = params,
            strategy = openingBalanceStrategy,
            createdAt = createdAt,
        )
    }

    fun postAdjustment(
        timestamp: Long,
        description: String?,
        amount: Money,
        account: Account,
        isDebit: Boolean,
        createdAt: Long,
    ): LedgerResult<PostedTransaction> {
        validator.validateAdjustment(account, amount)?.let { return LedgerResult.Failure(it) }
        val strategy =
            strategyMap[TransactionType.ADJUSTMENT]
                ?: return LedgerResult.Failure(LedgerError.InvalidTransactionType("ADJUSTMENT"))
        val params = PostingParams.Adjustment(account, amount, isDebit)
        return executeStrategy(
            type = TransactionType.ADJUSTMENT,
            timestamp = timestamp,
            description = description,
            amount = amount,
            params = params,
            strategy = strategy,
            createdAt = createdAt,
        )
    }

    private fun executeStrategy(
        type: TransactionType,
        timestamp: Long,
        description: String?,
        amount: Money,
        params: PostingParams,
        strategy: LedgerPostingStrategy,
        createdAt: Long,
    ): LedgerResult<PostedTransaction> {
        val txId = idGenerator.nextId()
        val parts = strategy.post(txId, timestamp, description, amount, params, idGenerator, createdAt)

        // Validate Debit == Credit invariant
        val debits = parts.entries.filter { it.entryType == LedgerEntryType.DEBIT }.sumOf { it.amount.amount }
        val credits = parts.entries.filter { it.entryType == LedgerEntryType.CREDIT }.sumOf { it.amount.amount }
        val isBalanced = debits == credits

        if (!isBalanced) {
            return LedgerResult.Failure(LedgerError.UnbalancedTransaction(debits, credits))
        }

        val summary =
            PostingSummary(
                debitTotal = Money(debits, amount.currency),
                creditTotal = Money(credits, amount.currency),
                isBalanced = true,
                currency = amount.currency,
            )

        val tx =
            Transaction(
                id = txId,
                timestamp = timestamp,
                description = description,
                type = type,
                merchantId =
                    when (params) {
                        is PostingParams.Income -> params.merchantId
                        is PostingParams.Expense -> params.merchantId
                        else -> null
                    },
                createdBy = "MANUAL",
                isRecurring = false,
                createdAt = createdAt,
                updatedAt = createdAt,
            )

        return LedgerResult.Success(
            PostedTransaction(
                transaction = tx,
                splits = parts.splits,
                entries = parts.entries,
                summary = summary,
            ),
        )
    }
}
