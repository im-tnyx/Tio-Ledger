package com.tioledger.domain.service

import com.tioledger.domain.model.SmsIgnoredReason
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseRequest
import com.tioledger.domain.model.SmsParseResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeterministicSmsTransactionParserSafetyTest {
    private val parser = DeterministicSmsTransactionParser()

    @Test
    fun punctuationOtpMessageIsIgnoredBeforeAmountFallback() {
        val result = parse("Your OTP: 482911 for login verification.")

        assertEquals(
            SmsIgnoredReason.OTP_OR_SECURITY_CODE,
            assertIs<SmsParseResult.Ignored>(result).reason,
        )
    }

    @Test
    fun maskedAccountAndDateDigitsAreNotUsedAsUnmarkedAmount() {
        val result =
            assertIs<SmsParseResult.Suggestion>(
                parse(
                    message = "A/c XX1234 was debited on 24/07/2026 at STORE",
                    defaultCurrencyCode = "INR",
                ),
            ).value

        assertNull(result.amountMinorUnits)
        assertTrue(SmsMissingField.AMOUNT in result.missingFields)
        assertTrue(SmsMissingField.CURRENCY in result.missingFields)
    }

    private fun parse(
        message: String,
        defaultCurrencyCode: String? = null,
    ): SmsParseResult =
        parser.parse(
            SmsParseRequest(
                message = message,
                receivedAt = 1_721_800_000_000L,
                timeZoneId = "Asia/Kolkata",
                defaultCurrencyCode = defaultCurrencyCode,
            ),
        )
}
