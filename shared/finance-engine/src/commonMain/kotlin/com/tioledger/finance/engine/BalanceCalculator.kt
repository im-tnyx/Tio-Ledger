package com.tioledger.finance.engine

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.NormalBalance

class BalanceCalculator {
    fun calculateBalance(
        entries: List<LedgerEntry>,
        normalBalance: NormalBalance,
        currency: CurrencyCode,
    ): LedgerResult<Money> {
        // Assert all entries belong to the correct currency code
        for (entry in entries) {
            if (entry.amount.currency != currency) {
                return LedgerResult.Failure(
                    LedgerError.CurrencyMismatch(currency.toString(), entry.amount.currency.toString()),
                )
            }
        }

        val debits = entries.filter { it.entryType == LedgerEntryType.DEBIT }.sumOf { it.amount.amount }
        val credits = entries.filter { it.entryType == LedgerEntryType.CREDIT }.sumOf { it.amount.amount }

        val netAmount =
            when (normalBalance) {
                NormalBalance.DEBIT -> debits - credits
                NormalBalance.CREDIT -> credits - debits
            }

        return LedgerResult.Success(Money(netAmount, currency))
    }
}
