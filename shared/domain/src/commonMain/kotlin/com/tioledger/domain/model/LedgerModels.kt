package com.tioledger.domain.model

import com.tioledger.core.model.Money
import kotlinx.serialization.Serializable

const val SYSTEM_INCOME_ID = "system-income-account-uuid"
const val SYSTEM_EXPENSE_ID = "system-expense-account-uuid"
const val SYSTEM_OPENING_BALANCE_ID = "system-opening-balance-uuid"
const val SYSTEM_ADJUSTMENT_ID = "system-adjustment-uuid"

@Serializable
enum class NormalBalance {
    DEBIT,
    CREDIT,
}

@Serializable
enum class LedgerClass {
    ASSET,
    LIABILITY,
    EQUITY,
    INCOME,
    EXPENSE,
    ;

    val normalBalance: NormalBalance
        get() =
            when (this) {
                ASSET, EXPENSE -> NormalBalance.DEBIT
                LIABILITY, EQUITY, INCOME -> NormalBalance.CREDIT
            }
}

@Serializable
enum class AccountType {
    CASH,
    BANK,
    CREDIT_CARD,
    WALLET,
    LOAN_LINKED,
    INVESTMENT,
    ;

    val ledgerClass: LedgerClass
        get() =
            when (this) {
                CASH, BANK, WALLET, INVESTMENT -> LedgerClass.ASSET
                CREDIT_CARD, LOAN_LINKED -> LedgerClass.LIABILITY
            }
}

@Serializable
enum class CategoryType {
    INCOME,
    EXPENSE,
    ;

    val ledgerClass: LedgerClass
        get() =
            when (this) {
                INCOME -> LedgerClass.INCOME
                EXPENSE -> LedgerClass.EXPENSE
            }
}

@Serializable
enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER,
    LOAN_DISBURSEMENT,
    REPAYMENT,
    ADJUSTMENT,
}

@Serializable
enum class LedgerEntryType {
    DEBIT,
    CREDIT,
}

@Serializable
enum class LedgerSourceType {
    TRANSACTION,
    EMI,
    PREPAYMENT,
    ADJUSTMENT,
    OPENING_BALANCE,
    SYSTEM,
}

@Serializable
sealed interface PostingTarget {
    @Serializable
    data class Account(val accountId: String, val ledgerClass: LedgerClass) : PostingTarget

    @Serializable
    data class Virtual(val categoryId: String?, val ledgerClass: LedgerClass) : PostingTarget
}

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val isArchived: Boolean = false,
    val displayOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
    val entityVersion: Int = 1,
    val syncVersion: Int = 0,
    val deviceId: String? = null,
    val updatedBy: String? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val type: CategoryType,
    val parentId: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val entityVersion: Int = 1,
    val syncVersion: Int = 0,
    val deviceId: String? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class Transaction(
    val id: String,
    val timestamp: Long,
    val description: String? = null,
    val type: TransactionType,
    val merchantId: String? = null,
    val createdBy: String = "MANUAL",
    val isRecurring: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val entityVersion: Int = 1,
    val syncVersion: Int = 0,
    val deviceId: String? = null,
    val updatedBy: String? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class TransactionSplit(
    val id: String,
    val transactionId: String,
    val accountId: String,
    val categoryId: String? = null,
    val amount: Money,
    val notes: String? = null,
    val createdAt: Long,
)

@Serializable
data class LedgerEntry(
    val id: String,
    val transactionId: String,
    val splitId: String? = null,
    val target: PostingTarget,
    val amount: Money,
    val entryType: LedgerEntryType,
    val sourceType: LedgerSourceType,
    val description: String? = null,
    val createdAt: Long,
)
