package com.tioledger.data.mapper

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.data.resolver.SystemAccountResolver
import com.tioledger.database.Accounts
import com.tioledger.database.Categories
import com.tioledger.database.Transactions
import com.tioledger.database.query.SelectLedgerEntriesByAccountId
import com.tioledger.database.query.SelectLedgerEntriesByTransactionId
import com.tioledger.database.query.SelectSplitsByTransactionId
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.Transaction
import com.tioledger.domain.model.TransactionSplit
import com.tioledger.domain.model.TransactionType

fun Accounts.toDomain(): Account {
    return Account(
        id = id,
        name = name,
        type = AccountType.valueOf(type),
        currencyCode = currency_code,
        isArchived = is_archived == 1L,
        displayOrder = display_order.toInt(),
        createdAt = created_at,
        updatedAt = updated_at,
        entityVersion = entity_version.toInt(),
        syncVersion = sync_version.toInt(),
        deviceId = device_id,
        updatedBy = updated_by,
        deletedAt = deleted_at,
    )
}

fun Categories.toDomain(): Category {
    return Category(
        id = id,
        name = name,
        type = CategoryType.valueOf(type),
        parentId = parent_id,
        isDefault = is_default == 1L,
        createdAt = created_at,
        updatedAt = updated_at,
        entityVersion = entity_version.toInt(),
        syncVersion = sync_version.toInt(),
        deviceId = device_id,
        deletedAt = deleted_at,
    )
}

fun Transactions.toDomain(): Transaction {
    return Transaction(
        id = id,
        timestamp = timestamp,
        description = description,
        type = TransactionType.valueOf(type),
        merchantId = merchant_id,
        createdBy = created_by,
        isRecurring = is_recurring == 1L,
        createdAt = created_at,
        updatedAt = updated_at,
        entityVersion = entity_version.toInt(),
        syncVersion = sync_version.toInt(),
        deviceId = device_id,
        updatedBy = updated_by,
        deletedAt = deleted_at,
    )
}

fun SelectSplitsByTransactionId.toDomain(): TransactionSplit {
    return TransactionSplit(
        id = id,
        transactionId = transaction_id,
        accountId = account_id,
        categoryId = category_id,
        amount = Money(amount, CurrencyCode(currency_code)),
        notes = notes,
        createdAt = created_at,
    )
}

fun SelectLedgerEntriesByAccountId.toDomain(): LedgerEntry {
    val target =
        if (SystemAccountResolver.isSystemAccountId(account_id)) {
            SystemAccountResolver.toVirtualTarget(account_id, split_category_id)
        } else {
            val type = AccountType.valueOf(account_type)
            PostingTarget.Account(account_id, type.ledgerClass)
        }
    return LedgerEntry(
        id = id,
        transactionId = transaction_id,
        splitId = split_id,
        target = target,
        amount = Money(amount, CurrencyCode(currency_code)),
        entryType = LedgerEntryType.valueOf(entry_type),
        sourceType = LedgerSourceType.valueOf(source_type),
        description = description,
        createdAt = created_at,
    )
}

fun SelectLedgerEntriesByTransactionId.toDomain(): LedgerEntry {
    val target =
        if (SystemAccountResolver.isSystemAccountId(account_id)) {
            SystemAccountResolver.toVirtualTarget(account_id, split_category_id)
        } else {
            val type = AccountType.valueOf(account_type)
            PostingTarget.Account(account_id, type.ledgerClass)
        }
    return LedgerEntry(
        id = id,
        transactionId = transaction_id,
        splitId = split_id,
        target = target,
        amount = Money(amount, CurrencyCode(currency_code)),
        entryType = LedgerEntryType.valueOf(entry_type),
        sourceType = LedgerSourceType.valueOf(source_type),
        description = description,
        createdAt = created_at,
    )
}
