package com.tioledger.application.usecase.budget

import com.tioledger.application.internal.validateTimestamp
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.budget.engine.BudgetPeriodCalculator
import com.tioledger.budget.engine.BudgetProgressCalculator
import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.repository.CategoryRepository
import com.tioledger.domain.repository.TransactionHistoryRepository

data class BudgetSummary(
    val id: String,
    val name: String,
    val target: Money,
    val categoryId: String?,
    val categoryName: String?,
    val periodType: BudgetPeriodType,
    val periodStartInclusive: Long,
    val periodEndExclusive: Long,
    val spent: Money,
    val remaining: Money,
    val utilizationPermille: Int,
    val status: BudgetProgressStatus,
)

class ListBudgetSummariesUseCase(
    private val listBudgetsUseCase: ListBudgetsUseCase,
    private val categoryRepository: CategoryRepository,
    private val transactionHistoryRepository: TransactionHistoryRepository,
    private val periodCalculator: BudgetPeriodCalculator,
    private val progressCalculator: BudgetProgressCalculator,
) {
    operator fun invoke(
        anchorTimestamp: Long,
        timeZoneId: String,
    ): ApplicationResult<List<BudgetSummary>> {
        validateTimestamp(anchorTimestamp, "anchorTimestamp")?.let {
            return ApplicationResult.Failure(it)
        }
        if (timeZoneId.isBlank()) {
            return ApplicationResult.Failure(
                ApplicationError.Validation("timeZoneId", "must not be blank"),
            )
        }

        val budgets =
            when (val result = listBudgetsUseCase()) {
                is ApplicationResult.Success -> result.outcome.value
                is ApplicationResult.Failure -> return result
            }
        if (budgets.any { it.periodType == BudgetPeriodType.CUSTOM }) {
            return ApplicationResult.Failure(
                ApplicationError.Validation("periodType", "custom budget periods are not supported in v1"),
            )
        }

        val categories =
            when (val result = categoryRepository.findAll()) {
                is LedgerResult.Success -> result.value.associateBy { it.id }
                is LedgerResult.Failure -> {
                    return ApplicationResult.Failure(ApplicationError.Repository(result.error))
                }
            }
        val transactions =
            when (val result = transactionHistoryRepository.findAll()) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> {
                    return ApplicationResult.Failure(ApplicationError.Repository(result.error))
                }
            }

        return try {
            val summaries =
                budgets.map { budget ->
                    budget.toSummary(
                        categoryName = budget.categoryId?.let { categories[it]?.name },
                        anchorTimestamp = anchorTimestamp,
                        timeZoneId = timeZoneId,
                        transactions = transactions,
                    )
                }
            ApplicationResult.Success(UseCaseOutcome(value = summaries))
        } catch (error: IllegalArgumentException) {
            ApplicationResult.Failure(
                ApplicationError.Validation(
                    field = "timeZoneId",
                    reason = error.message ?: "invalid time zone",
                ),
            )
        } catch (error: ArithmeticException) {
            ApplicationResult.Failure(
                ApplicationError.Ledger(
                    LedgerError.Unknown(error.message ?: "budget calculation overflow"),
                ),
            )
        }
    }

    private fun Budget.toSummary(
        categoryName: String?,
        anchorTimestamp: Long,
        timeZoneId: String,
        transactions: List<com.tioledger.domain.model.TransactionHistoryRecord>,
    ): BudgetSummary {
        val period = periodCalculator.currentPeriod(periodType, anchorTimestamp, timeZoneId)
        val progress = progressCalculator.calculate(this, period, transactions)
        return BudgetSummary(
            id = id,
            name = name,
            target = progress.target,
            categoryId = categoryId,
            categoryName = categoryName,
            periodType = periodType,
            periodStartInclusive = period.startInclusive,
            periodEndExclusive = period.endExclusive,
            spent = progress.spent,
            remaining = progress.remaining,
            utilizationPermille = progress.utilizationPermille,
            status = progress.status,
        )
    }
}
