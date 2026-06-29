package com.tioledger.core.model

sealed interface LedgerError {
    data class AccountNotFound(val accountId: String) : LedgerError

    data class AccountArchived(val accountId: String) : LedgerError

    data class CategoryNotFound(val categoryId: String) : LedgerError

    data class CategoryTypeMismatch(val categoryId: String, val expectedType: String, val actualType: String) : LedgerError

    data class AccountCurrencyMismatch(val accountId: String, val expectedCurrency: String, val actualCurrency: String) : LedgerError

    data class CurrencyMismatch(val currency1: String, val currency2: String) : LedgerError

    data class NegativeAmount(val amount: Long) : LedgerError

    data object ZeroAmount : LedgerError

    data class UnbalancedTransaction(val debitSum: Long, val creditSum: Long) : LedgerError

    data class InvalidTransactionType(val type: String) : LedgerError

    data class InvalidTransfer(val reason: String) : LedgerError

    data class InvalidAdjustment(val reason: String) : LedgerError

    data class DuplicateAccountId(val accountId: String) : LedgerError
}
