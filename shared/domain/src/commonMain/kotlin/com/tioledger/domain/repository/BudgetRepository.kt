package com.tioledger.domain.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.Budget

interface BudgetRepository {
    fun findAll(): LedgerResult<List<Budget>>

    fun findById(budgetId: String): LedgerResult<Budget>

    fun create(budget: Budget): LedgerResult<Budget>

    fun update(budget: Budget): LedgerResult<Budget>
}
