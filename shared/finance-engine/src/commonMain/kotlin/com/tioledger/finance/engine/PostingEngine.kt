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
    private val strategyRegistry: PostingStrategyRegistry = PostingStrategyRegistry(),
) {
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
        val params = PostingParams.Income(account, category, merchantId)
        return executeStrategy(
            type = TransactionType.INCOME,
            timestamp = timestamp,
            description = description,
            amount = amount,
            params = params,
            strategy = strategyRegistry.resolveIncome(),
            createdAt = createdAt,
        )
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
        val params = PostingParams.Expense(account, category, merchantId)
        return executeStrategy(
            type = TransactionType.EXPENSE,
            timestamp = timestamp,
            description = description,
            amount = amount,
            params = params,
            strategy = strategyRegistry.resolveExpense(),
            createdAt = createdAt,
        )
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
        val params = PostingParams.Transfer(sourceAccount, targetAccount)
        return executeStrategy(
            type = TransactionType.TRANSFER,
            timestamp = timestamp,
            description = description,
            amount = amount,
            params = params,
            strategy = strategyRegistry.resolveTransfer(),
            createdAt = createdAt,
        )
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
            strategy = strategyRegistry.resolveOpeningBalance(),
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
        val params = PostingParams.Adjustment(account, amount, isDebit)
        return executeStrategy(
            type = TransactionType.ADJUSTMENT,
            timestamp = timestamp,
            description = description,
            amount = amount,
            params = params,
            strategy = strategyRegistry.resolveAdjustment(),
            createdAt = createdAt,
        )
    }

    private fun <P : PostingParams> executeStrategy(
        type: TransactionType,
        timestamp: Long,
        description: String?,
        amount: Money,
        params: P,
        strategy: LedgerPostingStrategy<P>,
        createdAt: Long,
    ): LedgerResult<PostedTransaction> {
        val txId = idGenerator.nextId()
        val parts =
            strategy.post(
                PostingContext(
                    transactionId = txId,
                    timestamp = timestamp,
                    description = description,
                    amount = amount,
                    params = params,
                    idGenerator = idGenerator,
                    createdAt = createdAt,
                ),
            )

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
                merchantId = params.merchantId,
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
