package com.tioledger.finance.engine

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.Money
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType

fun interface PostingRule<in T> {
    fun validate(input: T): LedgerError?
}

object AmountRule : PostingRule<Money> {
    override fun validate(input: Money): LedgerError? {
        return when {
            input.amount < 0L -> LedgerError.NegativeAmount(input.amount)
            input.amount == 0L -> LedgerError.ZeroAmount
            else -> null
        }
    }
}

object AccountRule : PostingRule<Account> {
    override fun validate(input: Account): LedgerError? {
        return if (input.isArchived) {
            LedgerError.AccountArchived(input.id)
        } else {
            null
        }
    }
}

class CurrencyRule(private val expectedCurrency: String) : PostingRule<Money> {
    override fun validate(input: Money): LedgerError? {
        return if (input.currency.toString() != expectedCurrency.uppercase()) {
            LedgerError.CurrencyMismatch(expectedCurrency, input.currency.toString())
        } else {
            null
        }
    }
}

class CategoryRule(private val expectedType: CategoryType) : PostingRule<Category> {
    override fun validate(input: Category): LedgerError? {
        val expectedClass = expectedType.ledgerClass
        val actualClass = input.type.ledgerClass
        return if (actualClass != expectedClass) {
            LedgerError.CategoryTypeMismatch(input.id, expectedClass.name, actualClass.name)
        } else {
            null
        }
    }
}

class PostingValidator {
    fun validateIncome(
        account: Account,
        category: Category?,
        amount: Money,
    ): LedgerError? {
        AmountRule.validate(amount)?.let { return it }
        AccountRule.validate(account)?.let { return it }
        CurrencyRule(account.currencyCode).validate(amount)?.let { return it }
        if (category != null) {
            CategoryRule(CategoryType.INCOME).validate(category)?.let { return it }
        }
        return null
    }

    fun validateExpense(
        account: Account,
        category: Category?,
        amount: Money,
    ): LedgerError? {
        AmountRule.validate(amount)?.let { return it }
        AccountRule.validate(account)?.let { return it }
        CurrencyRule(account.currencyCode).validate(amount)?.let { return it }
        if (category != null) {
            CategoryRule(CategoryType.EXPENSE).validate(category)?.let { return it }
        }
        return null
    }

    fun validateTransfer(
        sourceAccount: Account,
        targetAccount: Account,
        amount: Money,
    ): LedgerError? {
        AmountRule.validate(amount)?.let { return it }
        AccountRule.validate(sourceAccount)?.let { return it }
        AccountRule.validate(targetAccount)?.let { return it }
        CurrencyRule(sourceAccount.currencyCode).validate(amount)?.let { return it }
        CurrencyRule(targetAccount.currencyCode).validate(amount)?.let { return it }

        if (sourceAccount.id == targetAccount.id) {
            return LedgerError.InvalidTransfer("Source and target accounts must be distinct: ${sourceAccount.id}")
        }
        if (sourceAccount.currencyCode.uppercase() != targetAccount.currencyCode.uppercase()) {
            return LedgerError.CurrencyMismatch(sourceAccount.currencyCode, targetAccount.currencyCode)
        }
        return null
    }

    fun validateOpeningBalance(
        account: Account,
        amount: Money,
    ): LedgerError? {
        AmountRule.validate(amount)?.let { return it }
        AccountRule.validate(account)?.let { return it }
        CurrencyRule(account.currencyCode).validate(amount)?.let { return it }
        return null
    }

    fun validateAdjustment(
        account: Account,
        amount: Money,
    ): LedgerError? {
        AmountRule.validate(amount)?.let { return it }
        AccountRule.validate(account)?.let { return it }
        CurrencyRule(account.currencyCode).validate(amount)?.let { return it }
        return null
    }
}
