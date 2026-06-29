package com.tioledger.finance.engine

import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.LedgerClass
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.NormalBalance
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.TransactionSplit

sealed interface PostingParams {
    data class Income(val account: Account, val category: Category?, val merchantId: String?) : PostingParams

    data class Expense(val account: Account, val category: Category?, val merchantId: String?) : PostingParams

    data class Transfer(val sourceAccount: Account, val targetAccount: Account) : PostingParams

    data class OpeningBalance(val account: Account) : PostingParams

    data class Adjustment(val account: Account, val deltaAmount: Money, val isDebit: Boolean) : PostingParams
}

data class PostedTransactionParts(
    val splits: List<TransactionSplit>,
    val entries: List<LedgerEntry>,
)

interface LedgerPostingStrategy {
    fun post(
        transactionId: String,
        timestamp: Long,
        description: String?,
        amount: Money,
        params: PostingParams,
        idGenerator: IdGenerator,
        createdAt: Long,
    ): PostedTransactionParts
}

class IncomePostingStrategy : LedgerPostingStrategy {
    override fun post(
        transactionId: String,
        timestamp: Long,
        description: String?,
        amount: Money,
        params: PostingParams,
        idGenerator: IdGenerator,
        createdAt: Long,
    ): PostedTransactionParts {
        val p = params as PostingParams.Income
        val splitId = idGenerator.nextId()

        val split =
            TransactionSplit(
                id = splitId,
                transactionId = transactionId,
                accountId = p.account.id,
                categoryId = p.category?.id,
                amount = amount,
                notes = description,
                createdAt = createdAt,
            )

        val assetEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitId,
                target = PostingTarget.Account(p.account.id, p.account.type.ledgerClass),
                amount = amount,
                entryType = LedgerEntryType.DEBIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = description,
                createdAt = createdAt,
            )

        val incomeEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitId,
                target = PostingTarget.Virtual(p.category?.id, LedgerClass.INCOME),
                amount = amount,
                entryType = LedgerEntryType.CREDIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = description,
                createdAt = createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(split),
            entries = listOf(assetEntry, incomeEntry),
        )
    }
}

class ExpensePostingStrategy : LedgerPostingStrategy {
    override fun post(
        transactionId: String,
        timestamp: Long,
        description: String?,
        amount: Money,
        params: PostingParams,
        idGenerator: IdGenerator,
        createdAt: Long,
    ): PostedTransactionParts {
        val p = params as PostingParams.Expense
        val splitId = idGenerator.nextId()

        val split =
            TransactionSplit(
                id = splitId,
                transactionId = transactionId,
                accountId = p.account.id,
                categoryId = p.category?.id,
                amount = amount,
                notes = description,
                createdAt = createdAt,
            )

        val expenseEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitId,
                target = PostingTarget.Virtual(p.category?.id, LedgerClass.EXPENSE),
                amount = amount,
                entryType = LedgerEntryType.DEBIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = description,
                createdAt = createdAt,
            )

        val assetEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitId,
                target = PostingTarget.Account(p.account.id, p.account.type.ledgerClass),
                amount = amount,
                entryType = LedgerEntryType.CREDIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = description,
                createdAt = createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(split),
            entries = listOf(expenseEntry, assetEntry),
        )
    }
}

class TransferPostingStrategy : LedgerPostingStrategy {
    override fun post(
        transactionId: String,
        timestamp: Long,
        description: String?,
        amount: Money,
        params: PostingParams,
        idGenerator: IdGenerator,
        createdAt: Long,
    ): PostedTransactionParts {
        val p = params as PostingParams.Transfer
        val splitSourceId = idGenerator.nextId()
        val splitTargetId = idGenerator.nextId()

        val splitSource =
            TransactionSplit(
                id = splitSourceId,
                transactionId = transactionId,
                accountId = p.sourceAccount.id,
                categoryId = null,
                amount = amount,
                notes = description,
                createdAt = createdAt,
            )

        val splitTarget =
            TransactionSplit(
                id = splitTargetId,
                transactionId = transactionId,
                accountId = p.targetAccount.id,
                categoryId = null,
                amount = amount,
                notes = description,
                createdAt = createdAt,
            )

        val sourceEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitSourceId,
                target = PostingTarget.Account(p.sourceAccount.id, p.sourceAccount.type.ledgerClass),
                amount = amount,
                entryType = LedgerEntryType.CREDIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = description,
                createdAt = createdAt,
            )

        val targetEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitTargetId,
                target = PostingTarget.Account(p.targetAccount.id, p.targetAccount.type.ledgerClass),
                amount = amount,
                entryType = LedgerEntryType.DEBIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = description,
                createdAt = createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(splitSource, splitTarget),
            entries = listOf(sourceEntry, targetEntry),
        )
    }
}

class OpeningBalancePostingStrategy : LedgerPostingStrategy {
    override fun post(
        transactionId: String,
        timestamp: Long,
        description: String?,
        amount: Money,
        params: PostingParams,
        idGenerator: IdGenerator,
        createdAt: Long,
    ): PostedTransactionParts {
        val p = params as PostingParams.OpeningBalance
        val splitId = idGenerator.nextId()

        val split =
            TransactionSplit(
                id = splitId,
                transactionId = transactionId,
                accountId = p.account.id,
                categoryId = null,
                amount = amount,
                notes = description,
                createdAt = createdAt,
            )

        val normalBalance = p.account.type.ledgerClass.normalBalance

        val accountEntryType =
            when (normalBalance) {
                NormalBalance.DEBIT -> LedgerEntryType.DEBIT
                NormalBalance.CREDIT -> LedgerEntryType.CREDIT
            }

        val equityEntryType =
            when (accountEntryType) {
                LedgerEntryType.DEBIT -> LedgerEntryType.CREDIT
                LedgerEntryType.CREDIT -> LedgerEntryType.DEBIT
            }

        val accountEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitId,
                target = PostingTarget.Account(p.account.id, p.account.type.ledgerClass),
                amount = amount,
                entryType = accountEntryType,
                sourceType = LedgerSourceType.OPENING_BALANCE,
                description = description,
                createdAt = createdAt,
            )

        val equityEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitId,
                target = PostingTarget.Virtual(null, LedgerClass.EQUITY),
                amount = amount,
                entryType = equityEntryType,
                sourceType = LedgerSourceType.OPENING_BALANCE,
                description = description,
                createdAt = createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(split),
            entries = listOf(accountEntry, equityEntry),
        )
    }
}

class AdjustmentPostingStrategy : LedgerPostingStrategy {
    override fun post(
        transactionId: String,
        timestamp: Long,
        description: String?,
        amount: Money,
        params: PostingParams,
        idGenerator: IdGenerator,
        createdAt: Long,
    ): PostedTransactionParts {
        val p = params as PostingParams.Adjustment
        val splitId = idGenerator.nextId()

        val split =
            TransactionSplit(
                id = splitId,
                transactionId = transactionId,
                accountId = p.account.id,
                categoryId = null,
                amount = amount,
                notes = description,
                createdAt = createdAt,
            )

        val accountEntryType = if (p.isDebit) LedgerEntryType.DEBIT else LedgerEntryType.CREDIT
        val equityEntryType = if (p.isDebit) LedgerEntryType.CREDIT else LedgerEntryType.DEBIT

        val accountEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitId,
                target = PostingTarget.Account(p.account.id, p.account.type.ledgerClass),
                amount = amount,
                entryType = accountEntryType,
                sourceType = LedgerSourceType.ADJUSTMENT,
                description = description,
                createdAt = createdAt,
            )

        val equityEntry =
            LedgerEntry(
                id = idGenerator.nextId(),
                transactionId = transactionId,
                splitId = splitId,
                target = PostingTarget.Virtual(null, LedgerClass.EQUITY),
                amount = amount,
                entryType = equityEntryType,
                sourceType = LedgerSourceType.ADJUSTMENT,
                description = description,
                createdAt = createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(split),
            entries = listOf(accountEntry, equityEntry),
        )
    }
}
