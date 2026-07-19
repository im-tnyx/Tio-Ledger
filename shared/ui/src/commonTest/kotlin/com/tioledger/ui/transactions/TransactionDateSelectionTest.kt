package com.tioledger.ui.transactions

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class TransactionDateSelectionTest {
    @Test
    fun transactionTimestampOpensPickerOnTheSameLocalCalendarDay() {
        val timeZone = TimeZone.of("Asia/Kolkata")
        val localDate = LocalDate(2026, 7, 19)
        val transactionTimestamp =
            localDate.atStartOfDayIn(timeZone).toEpochMilliseconds() +
                (21L * 60L * 60L * 1_000L)

        val pickerTimestamp =
            transactionTimestampToDatePickerMillis(
                timestamp = transactionTimestamp,
                timeZone = timeZone,
            )

        assertEquals(
            localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
            pickerTimestamp,
        )
    }

    @Test
    fun pickerSelectionBecomesLocalStartOfDayAndStableLabel() {
        val timeZone = TimeZone.of("America/Los_Angeles")
        val selectedDate = LocalDate(2026, 12, 5)
        val pickerTimestamp = selectedDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

        val selection =
            datePickerMillisToTransactionDateSelection(
                pickerTimestamp = pickerTimestamp,
                timeZone = timeZone,
            )

        assertEquals(
            selectedDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
            selection.timestamp,
        )
        assertEquals("2026-12-05", selection.label)
    }
}
