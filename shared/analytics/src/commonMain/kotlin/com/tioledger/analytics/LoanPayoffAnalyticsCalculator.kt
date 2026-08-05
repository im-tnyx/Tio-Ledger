package com.tioledger.analytics

import com.tioledger.core.model.Money
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.model.LoanInstallmentStatus

data class LoanPayoffAnalytics(
    val originalPrincipal: Money,
    val principalPaid: Money,
    val principalRemaining: Money,
    val principalProgressBasisPoints: Int,
    val interestPaid: Money,
    val interestRemaining: Money,
    val totalScheduledInterest: Money,
    val amountPaid: Money,
    val amountRemaining: Money,
    val paidInstallments: Int,
    val remainingInstallments: Int,
    val nextDueDate: Long?,
    val projectedPayoffDate: Long?,
)

class LoanPayoffAnalyticsCalculator {
    fun calculate(details: LoanDetails): LoanPayoffAnalytics {
        val principal = details.loan.principal
        require(principal.amount > 0L) { "loan principal must be positive" }
        requireScheduleCurrency(details)

        val orderedSchedule =
            details.schedule.sortedWith(
                compareBy<LoanInstallment> { it.installmentNumber }
                    .thenBy { it.dueDate }
                    .thenBy { it.id },
            )
        val paidSchedule = orderedSchedule.filter { it.status == LoanInstallmentStatus.PAID }
        val remainingSchedule = orderedSchedule.filter { it.status != LoanInstallmentStatus.PAID }
        val zero = Money.zero(principal.currency)

        val principalPaid = paidSchedule.sumMoney(zero) { it.principalComponent }
        require(principalPaid.amount <= principal.amount) {
            "paid principal must not exceed original principal"
        }
        val principalRemaining = principal - principalPaid
        val interestPaid = paidSchedule.sumMoney(zero) { it.interestComponent }
        val interestRemaining = remainingSchedule.sumMoney(zero) { it.interestComponent }
        val totalScheduledInterest = interestPaid + interestRemaining
        val amountPaid = paidSchedule.sumMoney(zero) { it.payment }
        val amountRemaining = remainingSchedule.sumMoney(zero) { it.payment }

        return LoanPayoffAnalytics(
            originalPrincipal = principal,
            principalPaid = principalPaid,
            principalRemaining = principalRemaining,
            principalProgressBasisPoints =
                progressBasisPoints(
                    paid = principalPaid.amount,
                    total = principal.amount,
                ),
            interestPaid = interestPaid,
            interestRemaining = interestRemaining,
            totalScheduledInterest = totalScheduledInterest,
            amountPaid = amountPaid,
            amountRemaining = amountRemaining,
            paidInstallments = paidSchedule.size,
            remainingInstallments = remainingSchedule.size,
            nextDueDate = remainingSchedule.minOfOrNull(LoanInstallment::dueDate),
            projectedPayoffDate =
                remainingSchedule.maxOfOrNull(LoanInstallment::dueDate)
                    ?: orderedSchedule.maxOfOrNull(LoanInstallment::dueDate),
        )
    }

    private fun requireScheduleCurrency(details: LoanDetails) {
        val expectedCurrency = details.loan.principal.currency
        details.schedule.forEach { installment ->
            val values =
                listOf(
                    installment.openingBalance,
                    installment.payment,
                    installment.principalComponent,
                    installment.interestComponent,
                    installment.closingBalance,
                )
            val mismatch = values.firstOrNull { it.currency != expectedCurrency }
            require(mismatch == null) {
                "loan schedule currency must match principal currency"
            }
        }
    }

    private fun progressBasisPoints(
        paid: Long,
        total: Long,
    ): Int {
        require(paid >= 0L) { "paid principal must not be negative" }
        require(total > 0L) { "total principal must be positive" }
        if (paid == 0L) return 0
        if (paid >= total) return BASIS_POINT_SCALE

        val whole = paid / total
        val remainder = paid % total
        val scaledWhole = whole * BASIS_POINT_SCALE
        val scaledFraction = scaledFractionHalfUp(remainder, total, BASIS_POINT_SCALE)
        return (scaledWhole + scaledFraction).coerceIn(0, BASIS_POINT_SCALE)
    }

    private fun scaledFractionHalfUp(
        numerator: Long,
        denominator: Long,
        scale: Int,
    ): Int {
        var accumulator = 0L
        var result = 0
        repeat(scale) {
            if (accumulator >= denominator - numerator) {
                accumulator -= denominator - numerator
                result += 1
            } else {
                accumulator += numerator
            }
        }
        if (accumulator >= (denominator + 1L) / 2L && result < scale) {
            result += 1
        }
        return result
    }

    private fun List<LoanInstallment>.sumMoney(
        zero: Money,
        selector: (LoanInstallment) -> Money,
    ): Money = fold(zero) { total, installment -> total + selector(installment) }

    private companion object {
        const val BASIS_POINT_SCALE = 10_000
    }
}
