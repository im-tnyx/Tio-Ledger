package com.tioledger.loan.engine

import com.tioledger.core.model.Money
import kotlinx.datetime.LocalDate

enum class LoanPaymentFrequency {
    MONTHLY,
    QUARTERLY,
    ANNUALLY,
}

data class LoanTerms(
    val principal: Money,
    val annualInterestRateBasisPoints: Int,
    val tenureMonths: Int,
    val startDate: LocalDate,
    val paymentFrequency: LoanPaymentFrequency = LoanPaymentFrequency.MONTHLY,
)

data class AmortizationInstallment(
    val installmentNumber: Int,
    val dueDate: LocalDate,
    val openingBalance: Money,
    val payment: Money,
    val principalComponent: Money,
    val interestComponent: Money,
    val closingBalance: Money,
    val isFinal: Boolean,
)

data class LoanQuote(
    val terms: LoanTerms,
    val emi: Money,
    val totalInterest: Money,
    val totalPayable: Money,
    val schedule: List<AmortizationInstallment>,
)

sealed interface LoanCalculationError {
    data object InvalidPrincipal : LoanCalculationError

    data object NegativeInterestRate : LoanCalculationError

    data object InvalidTenure : LoanCalculationError

    data class UnsupportedPaymentFrequency(
        val frequency: LoanPaymentFrequency,
    ) : LoanCalculationError

    data object ArithmeticOverflow : LoanCalculationError

    data object DateOutOfRange : LoanCalculationError

    data object ScheduleDidNotClose : LoanCalculationError
}

sealed interface LoanCalculationResult<out T> {
    data class Success<T>(val value: T) : LoanCalculationResult<T>

    data class Failure(val error: LoanCalculationError) : LoanCalculationResult<Nothing>
}

interface LoanCalculator {
    fun calculate(terms: LoanTerms): LoanCalculationResult<LoanQuote>
}

class MonthlyReducingBalanceLoanCalculator : LoanCalculator {
    override fun calculate(terms: LoanTerms): LoanCalculationResult<LoanQuote> {
        validate(terms)?.let { error -> return LoanCalculationResult.Failure(error) }

        return try {
            LoanCalculationResult.Success(calculateQuote(terms))
        } catch (error: LoanCalculationException) {
            LoanCalculationResult.Failure(error.calculationError)
        } catch (_: ArithmeticException) {
            LoanCalculationResult.Failure(LoanCalculationError.ArithmeticOverflow)
        }
    }

    private fun calculateQuote(terms: LoanTerms): LoanQuote {
        val emiAmount = calculateEmiAmount(terms)
        val schedule = generateSchedule(terms, emiAmount)
        val totalInterestAmount =
            schedule.fold(0L) { total, installment ->
                total.checkedAdd(installment.interestComponent.amount)
            }
        val totalPayableAmount = terms.principal.amount.checkedAdd(totalInterestAmount)
        val currency = terms.principal.currency

        return LoanQuote(
            terms = terms,
            emi = Money(emiAmount, currency),
            totalInterest = Money(totalInterestAmount, currency),
            totalPayable = Money(totalPayableAmount, currency),
            schedule = schedule,
        )
    }

    private fun calculateEmiAmount(terms: LoanTerms): Long {
        val principal = terms.principal.amount
        val firstInterest = monthlyInterest(principal, terms.annualInterestRateBasisPoints)
        var lowerBound = 1L
        var upperBound = principal.checkedAdd(firstInterest)

        while (lowerBound < upperBound) {
            val candidate = lowerBound + (upperBound - lowerBound) / 2L
            if (closesWithinTenure(terms, candidate)) {
                upperBound = candidate
            } else {
                lowerBound = candidate + 1L
            }
        }

        return lowerBound
    }

    private fun closesWithinTenure(
        terms: LoanTerms,
        paymentAmount: Long,
    ): Boolean {
        var balance = terms.principal.amount
        repeat(terms.tenureMonths) {
            val interest = monthlyInterest(balance, terms.annualInterestRateBasisPoints)
            val totalDue = balance.checkedAdd(interest)
            if (paymentAmount >= totalDue) return true
            balance = totalDue.checkedSubtract(paymentAmount)
        }
        return balance == 0L
    }

    private fun generateSchedule(
        terms: LoanTerms,
        emiAmount: Long,
    ): List<AmortizationInstallment> {
        val currency = terms.principal.currency
        val installments = mutableListOf<AmortizationInstallment>()
        var balance = terms.principal.amount

        for (installmentNumber in 1..terms.tenureMonths) {
            val openingBalance = balance
            val interest = monthlyInterest(openingBalance, terms.annualInterestRateBasisPoints)
            val totalDue = openingBalance.checkedAdd(interest)
            val payment = minOf(emiAmount, totalDue)
            val principalComponent = payment.checkedSubtract(interest)
            if (principalComponent <= 0L) {
                throw LoanCalculationException(LoanCalculationError.ScheduleDidNotClose)
            }
            val closingBalance = openingBalance.checkedSubtract(principalComponent)
            val isFinal = closingBalance == 0L

            installments +=
                AmortizationInstallment(
                    installmentNumber = installmentNumber,
                    dueDate = terms.startDate.plusMonthsClamped(installmentNumber),
                    openingBalance = Money(openingBalance, currency),
                    payment = Money(payment, currency),
                    principalComponent = Money(principalComponent, currency),
                    interestComponent = Money(interest, currency),
                    closingBalance = Money(closingBalance, currency),
                    isFinal = isFinal,
                )

            balance = closingBalance
            if (isFinal) break
        }

        if (balance != 0L) {
            throw LoanCalculationException(LoanCalculationError.ScheduleDidNotClose)
        }
        return installments
    }

    private fun validate(terms: LoanTerms): LoanCalculationError? =
        when {
            terms.principal.amount <= 0L -> LoanCalculationError.InvalidPrincipal
            terms.annualInterestRateBasisPoints < 0 -> LoanCalculationError.NegativeInterestRate
            terms.tenureMonths < 1 -> LoanCalculationError.InvalidTenure
            terms.paymentFrequency != LoanPaymentFrequency.MONTHLY ->
                LoanCalculationError.UnsupportedPaymentFrequency(terms.paymentFrequency)
            else -> null
        }
}

private fun monthlyInterest(
    openingBalance: Long,
    annualInterestRateBasisPoints: Int,
): Long =
    multiplyDivideHalfUp(
        value = openingBalance,
        multiplier = annualInterestRateBasisPoints.toLong(),
        divisor = MONTHLY_RATE_DIVISOR,
    )

private fun multiplyDivideHalfUp(
    value: Long,
    multiplier: Long,
    divisor: Long,
): Long {
    require(value >= 0L) { "value must not be negative" }
    require(multiplier >= 0L) { "multiplier must not be negative" }
    require(divisor > 0L) { "divisor must be positive" }
    if (value == 0L || multiplier == 0L) return 0L

    val wholeProduct = (value / divisor).checkedMultiply(multiplier)
    val remainderProduct = (value % divisor).checkedMultiply(multiplier)
    val fractionalQuotient = remainderProduct / divisor
    val fractionalRemainder = remainderProduct % divisor
    val roundedFraction =
        fractionalQuotient.checkedAdd(
            if (fractionalRemainder >= (divisor + 1L) / 2L) 1L else 0L,
        )
    return wholeProduct.checkedAdd(roundedFraction)
}

private fun LocalDate.plusMonthsClamped(monthsToAdd: Int): LocalDate {
    val currentMonthIndex =
        year
            .toLong()
            .checkedMultiply(MONTHS_PER_YEAR)
            .checkedAdd(monthNumber.toLong() - 1L)
    val targetMonthIndex = currentMonthIndex.checkedAdd(monthsToAdd.toLong())
    val targetYear = floorDiv(targetMonthIndex, MONTHS_PER_YEAR)
    val targetMonth = floorMod(targetMonthIndex, MONTHS_PER_YEAR).toInt() + 1
    if (targetYear < Int.MIN_VALUE.toLong() || targetYear > Int.MAX_VALUE.toLong()) {
        throw LoanCalculationException(LoanCalculationError.DateOutOfRange)
    }
    val targetYearInt = targetYear.toInt()
    val targetDay = minOf(dayOfMonth, daysInMonth(targetYearInt, targetMonth))

    return try {
        LocalDate(targetYearInt, targetMonth, targetDay)
    } catch (_: IllegalArgumentException) {
        throw LoanCalculationException(LoanCalculationError.DateOutOfRange)
    }
}

private fun daysInMonth(
    year: Int,
    month: Int,
): Int =
    when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> throw LoanCalculationException(LoanCalculationError.DateOutOfRange)
    }

private fun isLeapYear(year: Int): Boolean = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

private fun floorDiv(
    value: Long,
    divisor: Long,
): Long {
    val quotient = value / divisor
    val remainder = value % divisor
    return if (remainder < 0L) quotient - 1L else quotient
}

private fun floorMod(
    value: Long,
    divisor: Long,
): Long = value - floorDiv(value, divisor) * divisor

private fun Long.checkedAdd(other: Long): Long {
    if (other > 0L && this > Long.MAX_VALUE - other) throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    if (other < 0L && this < Long.MIN_VALUE - other) throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    return this + other
}

private fun Long.checkedSubtract(other: Long): Long {
    if (other > 0L && this < Long.MIN_VALUE + other) throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    if (other < 0L && this > Long.MAX_VALUE + other) throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    return this - other
}

private fun Long.checkedMultiply(other: Long): Long {
    if (this == 0L || other == 0L) return 0L
    if (this == Long.MIN_VALUE && other == -1L) throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    if (other == Long.MIN_VALUE && this == -1L) throw ArithmeticException(LONG_OVERFLOW_MESSAGE)

    val result = this * other
    if (result / other != this) throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    return result
}

private class LoanCalculationException(
    val calculationError: LoanCalculationError,
) : IllegalStateException()

private const val MONTHS_PER_YEAR = 12L
private const val MONTHLY_RATE_DIVISOR = 120_000L
private const val LONG_OVERFLOW_MESSAGE = "Long overflow"
