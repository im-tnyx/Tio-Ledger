package com.tioledger.data.repository

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.data.mapper.toDomain
import com.tioledger.data.result.DataResult
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Budget
import com.tioledger.domain.repository.BudgetRepository

class SQLDelightBudgetRepository(
    private val database: TioLedgerDatabase,
) : BudgetRepository {
    override fun findAll(): LedgerResult<List<Budget>> {
        val result =
            runDatabaseCatching {
                database.budgetsQueries
                    .selectAllBudgets()
                    .executeAsList()
                    .map { it.toDomain() }
            }
        return result.toLedgerResult()
    }

    override fun findById(budgetId: String): LedgerResult<Budget> {
        val result =
            runDatabaseCatching {
                database.budgetsQueries
                    .selectBudgetById(budgetId)
                    .executeAsOneOrNull()
            }
        return when (result) {
            is DataResult.Success -> {
                val budget = result.value
                if (budget != null) {
                    LedgerResult.Success(budget.toDomain())
                } else {
                    LedgerResult.Failure(LedgerError.BudgetNotFound(budgetId))
                }
            }
            is DataResult.Failure -> result.toLedgerResult()
        }
    }

    override fun create(budget: Budget): LedgerResult<Budget> {
        val result =
            runDatabaseCatching {
                database.budgetsQueries.insertBudget(
                    id = budget.id,
                    name = budget.name,
                    amount = budget.amount.amount,
                    currency_code = budget.amount.currency.normalized,
                    category_id = budget.categoryId,
                    period_type = budget.periodType.name,
                    created_at = budget.createdAt,
                    updated_at = budget.updatedAt,
                    entity_version = budget.entityVersion.toLong(),
                    sync_version = budget.syncVersion.toLong(),
                    device_id = budget.deviceId,
                    deleted_at = budget.deletedAt,
                )
                budget
            }
        return result.toLedgerResult()
    }

    override fun update(budget: Budget): LedgerResult<Budget> {
        val existingResult =
            runDatabaseCatching {
                database.budgetsQueries
                    .selectBudgetById(budget.id)
                    .executeAsOneOrNull()
            }
        if (existingResult is DataResult.Success && existingResult.value == null) {
            return LedgerResult.Failure(LedgerError.BudgetNotFound(budget.id))
        }

        val result =
            runDatabaseCatching {
                database.budgetsQueries.updateBudget(
                    name = budget.name,
                    amount = budget.amount.amount,
                    currency_code = budget.amount.currency.normalized,
                    category_id = budget.categoryId,
                    period_type = budget.periodType.name,
                    updated_at = budget.updatedAt,
                    entity_version = budget.entityVersion.toLong(),
                    sync_version = budget.syncVersion.toLong(),
                    device_id = budget.deviceId,
                    deleted_at = budget.deletedAt,
                    id = budget.id,
                )
                budget
            }
        return result.toLedgerResult { LedgerError.BudgetNotFound(budget.id) }
    }
}
