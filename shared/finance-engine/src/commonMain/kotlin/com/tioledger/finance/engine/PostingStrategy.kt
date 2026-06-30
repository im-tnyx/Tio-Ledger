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
    val merchantId: String?
        get() = null

    data class Income(
        val account: Account,
        val category: Category?,
        override val merchantId: String?,
    ) : PostingParams

    data class Expense(
        val account: Account,
        val category: Category?,
        override val merchantId: String?,
    ) : PostingParams

    data class Transfer(val sourceAccount: Account, val targetAccount: Account) : PostingParams

    data class OpeningBalance(val account: Account) : PostingParams

    data class Adjustment(val account: Account, val deltaAmount: Money, val isDebit: Boolean) : PostingParams
}

data class PostingContext<P : PostingParams>(
    val transactionId: String,
    val timestamp: Long,
    val description: String?,
    val amount: Money,
    val params: P,
    val idGenerator: IdGenerator,
    val createdAt: Long,
)

data class PostedTransactionParts(
    val splits: List<TransactionSplit>,
    val entries: List<LedgerEntry>,
)

interface LedgerPostingStrategy<P : PostingParams> {
    fun post(context: PostingContext<P>): PostedTransactionParts
}

class IncomePostingStrategy : LedgerPostingStrategy<PostingParams.Income> {
    override fun post(context: PostingContext<PostingParams.Income>): PostedTransactionParts {
        val params = context.params
        val splitId = context.idGenerator.nextId()

        val split =
            TransactionSplit(
                id = splitId,
                transactionId = context.transactionId,
                accountId = params.account.id,
                categoryId = params.category?.id,
                amount = context.amount,
                notes = context.description,
                createdAt = context.createdAt,
            )

        val assetEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitId,
                target = PostingTarget.Account(params.account.id, params.account.type.ledgerClass),
                amount = context.amount,
                entryType = LedgerEntryType.DEBIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = context.description,
                createdAt = context.createdAt,
            )

        val incomeEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitId,
                target = PostingTarget.Virtual(params.category?.id, LedgerClass.INCOME),
                amount = context.amount,
                entryType = LedgerEntryType.CREDIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = context.description,
                createdAt = context.createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(split),
            entries = listOf(assetEntry, incomeEntry),
        )
    }
}

class ExpensePostingStrategy : LedgerPostingStrategy<PostingParams.Expense> {
    override fun post(context: PostingContext<PostingParams.Expense>): PostedTransactionParts {
        val params = context.params
        val splitId = context.idGenerator.nextId()

        val split =
            TransactionSplit(
                id = splitId,
                transactionId = context.transactionId,
                accountId = params.account.id,
                categoryId = params.category?.id,
                amount = context.amount,
                notes = context.description,
                createdAt = context.createdAt,
            )

        val expenseEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitId,
                target = PostingTarget.Virtual(params.category?.id, LedgerClass.EXPENSE),
                amount = context.amount,
                entryType = LedgerEntryType.DEBIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = context.description,
                createdAt = context.createdAt,
            )

        val assetEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitId,
                target = PostingTarget.Account(params.account.id, params.account.type.ledgerClass),
                amount = context.amount,
                entryType = LedgerEntryType.CREDIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = context.description,
                createdAt = context.createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(split),
            entries = listOf(expenseEntry, assetEntry),
        )
    }
}

class TransferPostingStrategy : LedgerPostingStrategy<PostingParams.Transfer> {
    override fun post(context: PostingContext<PostingParams.Transfer>): PostedTransactionParts {
        val params = context.params
        val splitSourceId = context.idGenerator.nextId()
        val splitTargetId = context.idGenerator.nextId()

        val splitSource =
            TransactionSplit(
                id = splitSourceId,
                transactionId = context.transactionId,
                accountId = params.sourceAccount.id,
                categoryId = null,
                amount = context.amount,
                notes = context.description,
                createdAt = context.createdAt,
            )

        val splitTarget =
            TransactionSplit(
                id = splitTargetId,
                transactionId = context.transactionId,
                accountId = params.targetAccount.id,
                categoryId = null,
                amount = context.amount,
                notes = context.description,
                createdAt = context.createdAt,
            )

        val sourceEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitSourceId,
                target = PostingTarget.Account(params.sourceAccount.id, params.sourceAccount.type.ledgerClass),
                amount = context.amount,
                entryType = LedgerEntryType.CREDIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = context.description,
                createdAt = context.createdAt,
            )

        val targetEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitTargetId,
                target = PostingTarget.Account(params.targetAccount.id, params.targetAccount.type.ledgerClass),
                amount = context.amount,
                entryType = LedgerEntryType.DEBIT,
                sourceType = LedgerSourceType.TRANSACTION,
                description = context.description,
                createdAt = context.createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(splitSource, splitTarget),
            entries = listOf(sourceEntry, targetEntry),
        )
    }
}

class OpeningBalancePostingStrategy : LedgerPostingStrategy<PostingParams.OpeningBalance> {
    override fun post(context: PostingContext<PostingParams.OpeningBalance>): PostedTransactionParts {
        val params = context.params
        val splitId = context.idGenerator.nextId()

        val split =
            TransactionSplit(
                id = splitId,
                transactionId = context.transactionId,
                accountId = params.account.id,
                categoryId = null,
                amount = context.amount,
                notes = context.description,
                createdAt = context.createdAt,
            )

        val normalBalance = params.account.type.ledgerClass.normalBalance

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
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitId,
                target = PostingTarget.Account(params.account.id, params.account.type.ledgerClass),
                amount = context.amount,
                entryType = accountEntryType,
                sourceType = LedgerSourceType.OPENING_BALANCE,
                description = context.description,
                createdAt = context.createdAt,
            )

        val equityEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitId,
                target = PostingTarget.Virtual(null, LedgerClass.EQUITY),
                amount = context.amount,
                entryType = equityEntryType,
                sourceType = LedgerSourceType.OPENING_BALANCE,
                description = context.description,
                createdAt = context.createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(split),
            entries = listOf(accountEntry, equityEntry),
        )
    }
}

class AdjustmentPostingStrategy : LedgerPostingStrategy<PostingParams.Adjustment> {
    override fun post(context: PostingContext<PostingParams.Adjustment>): PostedTransactionParts {
        val params = context.params
        val splitId = context.idGenerator.nextId()

        val split =
            TransactionSplit(
                id = splitId,
                transactionId = context.transactionId,
                accountId = params.account.id,
                categoryId = null,
                amount = context.amount,
                notes = context.description,
                createdAt = context.createdAt,
            )

        val accountEntryType = if (params.isDebit) LedgerEntryType.DEBIT else LedgerEntryType.CREDIT
        val equityEntryType = if (params.isDebit) LedgerEntryType.CREDIT else LedgerEntryType.DEBIT

        val accountEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitId,
                target = PostingTarget.Account(params.account.id, params.account.type.ledgerClass),
                amount = context.amount,
                entryType = accountEntryType,
                sourceType = LedgerSourceType.ADJUSTMENT,
                description = context.description,
                createdAt = context.createdAt,
            )

        val equityEntry =
            LedgerEntry(
                id = context.idGenerator.nextId(),
                transactionId = context.transactionId,
                splitId = splitId,
                target = PostingTarget.Virtual(null, LedgerClass.EQUITY),
                amount = context.amount,
                entryType = equityEntryType,
                sourceType = LedgerSourceType.ADJUSTMENT,
                description = context.description,
                createdAt = context.createdAt,
            )

        return PostedTransactionParts(
            splits = listOf(split),
            entries = listOf(accountEntry, equityEntry),
        )
    }
}
