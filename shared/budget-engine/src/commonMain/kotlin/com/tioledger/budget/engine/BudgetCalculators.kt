package com.tioledger.budget.engine

import com.tioledger.core.model.Money
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

data class BudgetPeriodWindow(
    val startInclusive: Long,
    val endExclusive: Long,
)

enum class BudgetProgressStatus {
    ON_TRACK,
    WARNING,
    REACHED,
    EXCEEDED,
}

data class BudgetProgress(
    val target: Money,
    val spent: Money,
    val remaining: Money,
    val utilizationPermille: Int,
    val status: BudgetProgressStatus,
)

class BudgetPeriodCalculator {
    fun currentPeriod(
        periodType: BudgetPeriodType,
        anchorTimestamp: Long,
        timeZoneId: String,
    ): BudgetPeriodWindow {
        require(anchorTimestamp >= 0L) { "anchorTimestamp must be zero or greater" }
        require(timeZoneId.isNotBlank()) { "timeZoneId must not be blank" }
        require(periodType != BudgetPeriodType.CUSTOM) { "custom budget periods are not supported in v1" }

        val timeZone = TimeZone.of(timeZoneId)
        val anchorDate =
            Instant
                .fromEpochMilliseconds(anchorTimestamp)
                .toLocalDateTime(timeZone)
                .date
        val (startDate, endDate) = periodDates(periodType, anchorDate)
        return BudgetPeriodWindow(
            startInclusive = startDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            endExclusive = endDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        )
    }

    private fun periodDates(
        periodType: BudgetPeriodType,
        anchorDate: LocalDate,
    ): Pair<LocalDate, LocalDate> =
        when (periodType) {
            BudgetPeriodType.WEEKLY -> {
                val start = LocalDate.fromEpochDays(anchorDate.toEpochDays() - anchorDate.dayOfWeek.ordinal)
                start to LocalDate.fromEpochDays(start.toEpochDays() + DAYS_PER_WEEK)
            }
            BudgetPeriodType.MONTHLY -> {
                val start = LocalDate(anchorDate.year, anchorDate.monthNumber, 1)
                val end =
                    if (anchorDate.monthNumber == MONTHS_PER_YEAR) {
                        LocalDate(anchorDate.year + 1, 1, 1)
                    } else {
                        LocalDate(anchorDate.year, anchorDate.monthNumber + 1, 1)
                    }
                start to end
            }
            BudgetPeriodType.YEARLY -> {
                LocalDate(anchorDate.year, 1, 1) to LocalDate(anchorDate.year + 1, 1, 1)
            }
            BudgetPeriodType.CUSTOM -> error("custom budget periods are not supported in v1")
        }

    private companion object {
        const val DAYS_PER_WEEK = 7
        const val MONTHS_PER_YEAR = 12
    }
}

class BudgetProgressCalculator {
    fun calculate(
        budget: Budget,
        period: BudgetPeriodWindow,
        transactions: List<TransactionHistoryRecord>,
    ): BudgetProgress {
        require(period.startInclusive < period.endExclusive) { "budget period start must precede end" }
        require(budget.amount.isPositive()) { "budget target must be positive" }

        val spent =
            transactions
                .asSequence()
                .filter { it.type == TransactionType.EXPENSE }
                .filter { it.timestamp >= period.startInclusive && it.timestamp < period.endExclusive }
                .flatMap { it.splits.asSequence() }
                .filter { split -> budget.categoryId == null || split.categoryId == budget.categoryId }
                .map { it.amount }
                .filter { it.currency == budget.amount.currency && it.isPositive() }
                .fold(Money.zero(budget.amount.currency)) { total, amount -> total + amount }

        val remaining = budget.amount - spent
        val utilizationPermille = utilizationPermille(spent.amount, budget.amount.amount)
        val warningThreshold = budget.amount.amount - (budget.amount.amount / WARNING_DIVISOR)
        val status =
            when {
                spent.amount > budget.amount.amount -> BudgetProgressStatus.EXCEEDED
                spent.amount == budget.amount.amount -> BudgetProgressStatus.REACHED
                spent.amount >= warningThreshold -> BudgetProgressStatus.WARNING
                else -> BudgetProgressStatus.ON_TRACK
            }

        return BudgetProgress(
            target = budget.amount,
            spent = spent,
            remaining = remaining,
            utilizationPermille = utilizationPermille,
            status = status,
        )
    }

    private fun utilizationPermille(
        spent: Long,
        target: Long,
    ): Int {
        if (spent <= 0L) return 0

        val whole = spent / target
        if (whole >= Int.MAX_VALUE.toLong() / PERMILLE_SCALE) return Int.MAX_VALUE

        val wholeScaled = whole * PERMILLE_SCALE
        val fraction = scaledFraction(spent % target, target, PERMILLE_SCALE.toInt())
        return (wholeScaled + fraction)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun scaledFraction(
        numerator: Long,
        denominator: Long,
        scale: Int,
    ): Long {
        var accumulator = 0L
        var result = 0L
        repeat(scale) {
            if (accumulator >= denominator - numerator) {
                accumulator -= denominator - numerator
                result += 1L
            } else {
                accumulator += numerator
            }
        }
        return result
    }

    private companion object {
        const val PERMILLE_SCALE = 1_000L
        const val WARNING_DIVISOR = 5L
    }
}
