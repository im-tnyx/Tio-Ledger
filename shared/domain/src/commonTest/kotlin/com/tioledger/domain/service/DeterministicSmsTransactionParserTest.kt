package com.tioledger.domain.service

import com.tioledger.domain.model.SmsIgnoredReason
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseConfidence
import com.tioledger.domain.model.SmsParseRequest
import com.tioledger.domain.model.SmsParseResult
import com.tioledger.domain.model.SmsPaymentRail
import com.tioledger.domain.model.SmsTransactionDirection
import com.tioledger.domain.model.SmsUnsupportedReason
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeterministicSmsTransactionParserTest {
    private val parser = DeterministicSmsTransactionParser()

    @Test
    fun bankDebitProducesHighConfidenceExpense() {
        val result =
            parse(
                "INR 1,250.50 debited from A/c XX1234 on 24/07/2026 14:30 at Grocery Mart. " +
                    "Avl Bal INR 10,000.",
            ).suggestion()

        assertEquals(125_050L, result.amountMinorUnits)
        assertEquals("INR", result.currencyCode)
        assertEquals(SmsTransactionDirection.EXPENSE, result.direction)
        assertEquals(SmsPaymentRail.BANK, result.paymentRail)
        assertEquals("1234", result.accountHint)
        assertEquals("Grocery Mart", result.merchantOrCounterparty)
        assertEquals(timestamp(2026, 7, 24, 14, 30), result.occurredAt)
        assertEquals(SmsParseConfidence.HIGH, result.confidence)
    }

    @Test
    fun bankCreditProducesIncomeSuggestion() {
        val result =
            parse(
                "Rs. 5,000 credited to account XX8899 from ACME PAYROLL on 24-07-2026 09:15.",
            ).suggestion()

        assertEquals(500_000L, result.amountMinorUnits)
        assertEquals(SmsTransactionDirection.INCOME, result.direction)
        assertEquals("8899", result.accountHint)
        assertEquals("ACME PAYROLL", result.merchantOrCounterparty)
        assertEquals(SmsParseConfidence.HIGH, result.confidence)
    }

    @Test
    fun creditCardSpendDetectsCardRailAndUsd() {
        val result =
            parse(
                "USD 42.75 spent on card ending 7788 at BOOK STORE on 24/07/2026 18:05.",
            ).suggestion()

        assertEquals(4_275L, result.amountMinorUnits)
        assertEquals("USD", result.currencyCode)
        assertEquals(SmsTransactionDirection.EXPENSE, result.direction)
        assertEquals(SmsPaymentRail.CARD, result.paymentRail)
        assertEquals("7788", result.accountHint)
        assertEquals("BOOK STORE", result.merchantOrCounterparty)
    }

    @Test
    fun upiPaymentProducesExpenseSuggestion() {
        val result =
            parse(
                "₹799 paid via UPI from a/c XX1234 to FOOD CORNER on 24/07/2026 20:10.",
            ).suggestion()

        assertEquals(79_900L, result.amountMinorUnits)
        assertEquals(SmsTransactionDirection.EXPENSE, result.direction)
        assertEquals(SmsPaymentRail.UPI, result.paymentRail)
        assertEquals("FOOD CORNER", result.merchantOrCounterparty)
    }

    @Test
    fun upiReceiptProducesIncomeSuggestion() {
        val result =
            parse(
                "INR 1500 received via UPI in account XX1234 from RAVI K on 24/07/2026 08:05.",
            ).suggestion()

        assertEquals(150_000L, result.amountMinorUnits)
        assertEquals(SmsTransactionDirection.INCOME, result.direction)
        assertEquals(SmsPaymentRail.UPI, result.paymentRail)
        assertEquals("RAVI K", result.merchantOrCounterparty)
    }

    @Test
    fun walletDebitDetectsWalletRail() {
        val result =
            parse(
                "₹250 debited from wallet Paytm to METRO STORE on 24/07/2026 11:30.",
            ).suggestion()

        assertEquals(25_000L, result.amountMinorUnits)
        assertEquals(SmsPaymentRail.WALLET, result.paymentRail)
        assertEquals("Paytm", result.accountHint)
        assertEquals("METRO STORE", result.merchantOrCounterparty)
    }

    @Test
    fun atmWithdrawalProducesExpenseSuggestion() {
        val result =
            parse(
                "INR 2000 withdrawn from A/c XX1234 at ATM on 24/07/2026 06:45.",
            ).suggestion()

        assertEquals(200_000L, result.amountMinorUnits)
        assertEquals(SmsTransactionDirection.EXPENSE, result.direction)
        assertEquals(SmsPaymentRail.ATM, result.paymentRail)
        assertEquals("1234", result.accountHint)
    }

    @Test
    fun explicitTransferRemainsCandidateAndRequiresDestinationAccount() {
        val result =
            parse(
                "INR 1000 transferred from account XX1234 to account XX5678 on 24/07/2026 12:00.",
            ).suggestion()

        assertEquals(SmsTransactionDirection.TRANSFER_CANDIDATE, result.direction)
        assertTrue(SmsMissingField.DESTINATION_ACCOUNT in result.missingFields)
    }

    @Test
    fun defaultCurrencySupportsUnmarkedAmountWithoutInventingOtherFields() {
        val result =
            parse(
                message = "Paid 250 to Cafe Central",
                defaultCurrencyCode = "inr",
            ).suggestion()

        assertEquals(25_000L, result.amountMinorUnits)
        assertEquals("INR", result.currencyCode)
        assertEquals(SmsParseConfidence.MEDIUM, result.confidence)
        assertTrue(SmsMissingField.ACCOUNT in result.missingFields)
        assertTrue(SmsMissingField.TRANSACTION_TIME in result.missingFields)
    }

    @Test
    fun partialDetectionReturnsLowConfidenceInsteadOfGuessingAmount() {
        val result = parse("Your account XX1234 was charged at STORE").suggestion()

        assertNull(result.amountMinorUnits)
        assertEquals(SmsTransactionDirection.EXPENSE, result.direction)
        assertEquals(SmsParseConfidence.LOW, result.confidence)
        assertTrue(SmsMissingField.AMOUNT in result.missingFields)
        assertTrue(SmsMissingField.CURRENCY in result.missingFields)
    }

    @Test
    fun otpMessageIsIgnoredBeforeFinancialExtraction() {
        val result = parse("Your OTP is 482911 for INR 500 payment. Do not share it.")

        assertEquals(SmsIgnoredReason.OTP_OR_SECURITY_CODE, result.ignoredReason())
    }

    @Test
    fun promotionalMessageIsIgnored() {
        val result = parse("Exclusive offer: get 20% discount this weekend. Shop now.")

        assertEquals(SmsIgnoredReason.PROMOTIONAL, result.ignoredReason())
    }

    @Test
    fun failedTransactionIsIgnored() {
        val result = parse("Your payment failed for INR 750 at STORE due to insufficient funds.")

        assertEquals(SmsIgnoredReason.FAILED_OR_DECLINED_TRANSACTION, result.ignoredReason())
    }

    @Test
    fun balanceOnlyMessageIsIgnored() {
        val result = parse("Available balance in account XX1234 is INR 5,000.")

        assertEquals(SmsIgnoredReason.BALANCE_ONLY, result.ignoredReason())
    }

    @Test
    fun emptyAndUnrelatedMessagesAreUnsupported() {
        val empty = parse("   ")
        val unrelated = parse("Welcome to our service")

        assertEquals(SmsUnsupportedReason.EMPTY_MESSAGE, empty.unsupportedReason())
        assertEquals(SmsUnsupportedReason.INSUFFICIENT_EVIDENCE, unrelated.unsupportedReason())
    }

    @Test
    fun resultNeverContainsTheRawMessage() {
        val privateToken = "PRIVATE-RAW-TOKEN-923847"
        val raw =
            "INR 100 debited from A/c XX1234 at STORE on 24/07/2026 10:00. " +
                "Ref ABC123 $privateToken"
        val result = parse(raw)

        assertFalse(result.toString().contains(raw))
        assertFalse(result.toString().contains(privateToken))
    }

    @Test
    fun invalidTimeZoneFallsBackToReceivedTimeAndMarksTimeMissing() {
        val receivedAt = 1_721_800_000_000L
        val result =
            parser.parse(
                SmsParseRequest(
                    message = "INR 100 debited from A/c XX1234 on 24/07/2026 10:00.",
                    receivedAt = receivedAt,
                    timeZoneId = "Invalid/Zone",
                ),
            ).suggestion()

        assertEquals(receivedAt, result.occurredAt)
        assertTrue(SmsMissingField.TRANSACTION_TIME in result.missingFields)
        assertNotEquals(SmsParseConfidence.HIGH, result.confidence)
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

    private fun timestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long =
        LocalDateTime(year, month, day, hour, minute)
            .toInstant(TimeZone.of("Asia/Kolkata"))
            .toEpochMilliseconds()

    private fun SmsParseResult.suggestion() = assertIs<SmsParseResult.Suggestion>(this).value

    private fun SmsParseResult.ignoredReason() = assertIs<SmsParseResult.Ignored>(this).reason

    private fun SmsParseResult.unsupportedReason() = assertIs<SmsParseResult.Unsupported>(this).reason
}
