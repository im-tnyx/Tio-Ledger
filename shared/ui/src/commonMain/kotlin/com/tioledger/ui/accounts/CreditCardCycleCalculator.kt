package com.tioledger.ui.accounts

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class StatementCyclePreview(
    val payablePeriod: String,
    val outstandingPeriod: String,
)

object CreditCardCycleCalculator {
    fun calculate(
        settlementDay: Int,
        paymentDueDay: Int,
        referenceEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
    ): StatementCyclePreview {
        val safeSettlementDay = settlementDay.coerceIn(1, 31)
        val safePaymentDay = paymentDueDay.coerceIn(1, 31)

        val today =
            Instant.fromEpochMilliseconds(referenceEpochMillis)
                .toLocalDateTime(timeZone).date

        val curYear = today.year
        val curMonth = today.monthNumber

        val prevMonth = if (curMonth == 1) 12 else curMonth - 1
        val prevYear = if (curMonth == 1) curYear - 1 else curYear

        val prev2Month = if (prevMonth == 1) 12 else prevMonth - 1

        val nextMonth = if (curMonth == 12) 1 else curMonth + 1

        val payablePeriodStr =
            if (safeSettlementDay == 1) {
                val lastDayPrev = daysInMonth(prevYear, prevMonth)
                val m = prevMonth.twoDigits()
                "01/$m ~ ${lastDayPrev.twoDigits()}/$m($curMonth $safePaymentDay)"
            } else {
                val s = safeSettlementDay.twoDigits()
                val e = (safeSettlementDay - 1).twoDigits()
                "$s/${prev2Month.twoDigits()} ~ $e/${prevMonth.twoDigits()}($curMonth $safePaymentDay)"
            }

        val outstPeriodStr =
            if (safeSettlementDay == 1) {
                val lastDayCur = daysInMonth(curYear, curMonth)
                val m = curMonth.twoDigits()
                "01/$m ~ ${lastDayCur.twoDigits()}/$m($nextMonth $safePaymentDay)"
            } else {
                val s = safeSettlementDay.twoDigits()
                val e = (safeSettlementDay - 1).twoDigits()
                "$s/${prevMonth.twoDigits()} ~ $e/${curMonth.twoDigits()}($nextMonth $safePaymentDay)"
            }

        return StatementCyclePreview(
            payablePeriod = payablePeriodStr,
            outstandingPeriod = outstPeriodStr,
        )
    }

    private fun daysInMonth(
        year: Int,
        month: Int,
    ): Int =
        when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }

    private fun isLeapYear(year: Int): Boolean = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

    private fun Int.twoDigits(): String = if (this < 10) "0$this" else "$this"
}
