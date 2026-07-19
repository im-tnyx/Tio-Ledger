package com.tioledger.application.usecase.budget

import com.tioledger.application.internal.mapRepositoryResult
import com.tioledger.application.internal.normalizedCurrencyCode
import com.tioledger.application.internal.normalizedId
import com.tioledger.application.internal.validateCurrencyCode
import com.tioledger.application.internal.validateId
import com.tioledger.application.internal.validateName
import com.tioledger.application.internal.validateOptionalId
import com.tioledger.application.internal.validateTimestamp
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.event.DomainEvent
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.repository.BudgetRepository
import com.tioledger.domain.repository.CategoryRepository

class ListBudgetsUseCase(private val budgetRepository: BudgetRepository) {
    operator fun invoke(): ApplicationResult<List<Budget>> {
        return when (val result = budgetRepository.findAll()) {
            is LedgerResult.Success -> {
                val budgets =
                    result.value
                        .asSequence()
                        .filter { it.deletedAt == null }
                        .sortedWith(
                            compareBy<Budget> { it.name.lowercase() }
                                .thenBy { it.periodType.name }
                                .thenBy { it.id },
                        )
                        .toList()
                ApplicationResult.Success(UseCaseOutcome(value = budgets))
            }
            is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(result.error))
        }
    }
}

data class CreateBudgetCommand(
    val id: String,
    val name: String,
    val amount: Long,
    val currencyCode: String,
    val categoryId: String? = null,
    val periodType: BudgetPeriodType,
    val createdAt: Long,
    val deviceId: String? = null,
)

class CreateBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(command: CreateBudgetCommand): ApplicationResult<Budget> {
        validateBudgetInput(
            id = command.id,
            name = command.name,
            amount = command.amount,
            currencyCode = command.currencyCode,
            categoryId = command.categoryId,
            periodType = command.periodType,
            timestamp = command.createdAt,
        )?.let { return ApplicationResult.Failure(it) }

        validateExpenseCategory(command.categoryId)?.let { return ApplicationResult.Failure(it) }
        validateDuplicateScope(command.categoryId, command.periodType)?.let { return ApplicationResult.Failure(it) }

        val budget =
            Budget(
                id = normalizedId(command.id),
                name = command.name.trim(),
                amount = Money(command.amount, CurrencyCode(normalizedCurrencyCode(command.currencyCode))),
                categoryId = command.categoryId?.let(::normalizedId),
                periodType = command.periodType,
                createdAt = command.createdAt,
                updatedAt = command.createdAt,
                deviceId = command.deviceId,
            )

        return budgetRepository.create(budget).mapRepositoryResult(
            events = { created -> listOf(DomainEvent.BudgetCreated(created.id, command.createdAt)) },
            transform = { it },
        )
    }

    private fun validateExpenseCategory(categoryId: String?): ApplicationError? {
        if (categoryId == null) return null
        return when (val result = categoryRepository.findById(normalizedId(categoryId))) {
            is LedgerResult.Success -> {
                when {
                    result.value.deletedAt != null ->
                        ApplicationError.Validation(
                            field = "categoryId",
                            reason = "archived category cannot be budgeted",
                        )
                    result.value.type != CategoryType.EXPENSE ->
                        ApplicationError.Validation(
                            field = "categoryId",
                            reason = "budget category must be an expense category",
                        )
                    else -> null
                }
            }
            is LedgerResult.Failure -> ApplicationError.Repository(result.error)
        }
    }

    private fun validateDuplicateScope(
        categoryId: String?,
        periodType: BudgetPeriodType,
    ): ApplicationError? {
        val normalizedCategoryId = categoryId?.let(::normalizedId)
        return when (val result = budgetRepository.findAll()) {
            is LedgerResult.Success -> {
                val duplicateExists =
                    result.value.any { budget ->
                        budget.deletedAt == null &&
                            budget.categoryId == normalizedCategoryId &&
                            budget.periodType == periodType
                    }
                if (duplicateExists) {
                    ApplicationError.Validation("categoryId", "budget already exists for this category and period")
                } else {
                    null
                }
            }
            is LedgerResult.Failure -> ApplicationError.Repository(result.error)
        }
    }
}

data class UpdateBudgetCommand(
    val budgetId: String,
    val name: String,
    val amount: Long,
    val currencyCode: String,
    val categoryId: String? = null,
    val periodType: BudgetPeriodType,
    val updatedAt: Long,
)

class UpdateBudgetUseCase(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
) {
    operator fun invoke(command: UpdateBudgetCommand): ApplicationResult<Budget> {
        validateBudgetInput(
            id = command.budgetId,
            name = command.name,
            amount = command.amount,
            currencyCode = command.currencyCode,
            categoryId = command.categoryId,
            periodType = command.periodType,
            timestamp = command.updatedAt,
        )?.let { return ApplicationResult.Failure(it) }

        val budgetId = normalizedId(command.budgetId)
        val existing =
            when (val result = budgetRepository.findById(budgetId)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }
        if (existing.deletedAt != null) {
            return ApplicationResult.Failure(ApplicationError.Validation("budgetId", "archived budget cannot be updated"))
        }

        validateExpenseCategory(command.categoryId)?.let { return ApplicationResult.Failure(it) }
        validateDuplicateScope(budgetId, command.categoryId, command.periodType)?.let {
            return ApplicationResult.Failure(it)
        }

        val updated =
            existing.copy(
                name = command.name.trim(),
                amount = Money(command.amount, CurrencyCode(normalizedCurrencyCode(command.currencyCode))),
                categoryId = command.categoryId?.let(::normalizedId),
                periodType = command.periodType,
                updatedAt = command.updatedAt,
            )

        return budgetRepository.update(updated).mapRepositoryResult(
            events = { budget -> listOf(DomainEvent.BudgetUpdated(budget.id, command.updatedAt)) },
            transform = { it },
        )
    }

    private fun validateExpenseCategory(categoryId: String?): ApplicationError? {
        if (categoryId == null) return null
        return when (val result = categoryRepository.findById(normalizedId(categoryId))) {
            is LedgerResult.Success -> {
                when {
                    result.value.deletedAt != null ->
                        ApplicationError.Validation(
                            field = "categoryId",
                            reason = "archived category cannot be budgeted",
                        )
                    result.value.type != CategoryType.EXPENSE ->
                        ApplicationError.Validation(
                            field = "categoryId",
                            reason = "budget category must be an expense category",
                        )
                    else -> null
                }
            }
            is LedgerResult.Failure -> ApplicationError.Repository(result.error)
        }
    }

    private fun validateDuplicateScope(
        budgetId: String,
        categoryId: String?,
        periodType: BudgetPeriodType,
    ): ApplicationError? {
        val normalizedCategoryId = categoryId?.let(::normalizedId)
        return when (val result = budgetRepository.findAll()) {
            is LedgerResult.Success -> {
                val duplicateExists =
                    result.value.any { budget ->
                        budget.id != budgetId &&
                            budget.deletedAt == null &&
                            budget.categoryId == normalizedCategoryId &&
                            budget.periodType == periodType
                    }
                if (duplicateExists) {
                    ApplicationError.Validation("categoryId", "budget already exists for this category and period")
                } else {
                    null
                }
            }
            is LedgerResult.Failure -> ApplicationError.Repository(result.error)
        }
    }
}

private fun validateBudgetInput(
    id: String,
    name: String,
    amount: Long,
    currencyCode: String,
    categoryId: String?,
    periodType: BudgetPeriodType,
    timestamp: Long,
): ApplicationError.Validation? {
    validateId(id, "id")?.let { return it }
    validateName(name)?.let { return it }
    if (amount <= 0L) return ApplicationError.Validation("amount", "must be greater than zero")
    validateCurrencyCode(currencyCode)?.let { return it }
    validateOptionalId(categoryId, "categoryId")?.let { return it }
    if (periodType == BudgetPeriodType.CUSTOM) {
        return ApplicationError.Validation("periodType", "custom budget periods are not supported in v1")
    }
    validateTimestamp(timestamp, "timestamp")?.let { return it }
    return null
}
