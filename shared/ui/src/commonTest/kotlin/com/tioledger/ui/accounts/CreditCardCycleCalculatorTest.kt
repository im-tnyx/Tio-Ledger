package com.tioledger.ui.accounts

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class CreditCardCycleCalculatorTest {
    @Test
    fun calculateCyclesForFirstOfMonthMatchesReferenceScreenshot() {
        // Fixed epoch for 2026-11-07T12:00:00Z
        val nov2026Epoch = Instant.parse("2026-11-07T12:00:00Z").toEpochMilliseconds()

        val preview =
            CreditCardCycleCalculator.calculate(
                settlementDay = 1,
                paymentDueDay = 1,
                referenceEpochMillis = nov2026Epoch,
                timeZone = TimeZone.UTC,
            )

        assertEquals("01/10 ~ 31/10(11 1)", preview.payablePeriod)
        assertEquals("01/11 ~ 30/11(12 1)", preview.outstandingPeriod)
    }

    @Test
    fun calculateCyclesForMidMonthSettlement() {
        val nov2026Epoch = Instant.parse("2026-11-07T12:00:00Z").toEpochMilliseconds()

        val preview =
            CreditCardCycleCalculator.calculate(
                settlementDay = 15,
                paymentDueDay = 5,
                referenceEpochMillis = nov2026Epoch,
                timeZone = TimeZone.UTC,
            )

        assertEquals("15/09 ~ 14/10(11 5)", preview.payablePeriod)
        assertEquals("15/10 ~ 14/11(12 5)", preview.outstandingPeriod)
    }
}
