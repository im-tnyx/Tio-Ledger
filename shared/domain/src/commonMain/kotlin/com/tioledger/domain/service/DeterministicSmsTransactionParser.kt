package com.tioledger.domain.service

import com.tioledger.domain.model.SmsDetectedField
import com.tioledger.domain.model.SmsIgnoredReason
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseConfidence
import com.tioledger.domain.model.SmsParseEvidence
import com.tioledger.domain.model.SmsParseRequest
import com.tioledger.domain.model.SmsParseResult
import com.tioledger.domain.model.SmsPaymentRail
import com.tioledger.domain.model.SmsTransactionDirection
import com.tioledger.domain.model.SmsTransactionParser
import com.tioledger.domain.model.SmsTransactionSuggestion
import com.tioledger.domain.model.SmsUnsupportedReason
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class DeterministicSmsTransactionParser : SmsTransactionParser {
    override fun parse(request: SmsParseRequest): SmsParseResult {
        val normalized = request.message.normalizeMessage()
        if (normalized.isBlank()) {
            return SmsParseResult.Unsupported(SmsUnsupportedReason.EMPTY_MESSAGE)
        }

        classifyIgnored(normalized)?.let { return SmsParseResult.Ignored(it) }

        val amountDetection = detectAmount(normalized, request.defaultCurrencyCode)
        val direction = detectDirection(normalized)
        val paymentRail = detectPaymentRail(normalized)
        val occurredAtDetection = detectOccurredAt(normalized, request)
        val accountHint = detectAccountHint(normalized)
        val merchantOrCounterparty = detectMerchantOrCounterparty(normalized, direction)

        if (amountDetection == null && direction == null) {
            return SmsParseResult.Unsupported(
                reason = SmsUnsupportedReason.INSUFFICIENT_EVIDENCE,
                missingFields =
                    listOf(
                        SmsMissingField.AMOUNT,
                        SmsMissingField.CURRENCY,
                        SmsMissingField.DIRECTION,
                    ),
            )
        }

        val evidence = buildList {
            amountDetection?.let {
                add(SmsParseEvidence(SmsDetectedField.AMOUNT, it.amountMinorUnits.toString()))
                add(SmsParseEvidence(SmsDetectedField.CURRENCY, it.currencyCode))
            }
            direction?.let { add(SmsParseEvidence(SmsDetectedField.DIRECTION, it.name)) }
            occurredAtDetection.explicitTimestamp?.let {
                add(SmsParseEvidence(SmsDetectedField.TRANSACTION_TIME, it.toString()))
            }
            accountHint?.let { add(SmsParseEvidence(SmsDetectedField.ACCOUNT_HINT, it)) }
            merchantOrCounterparty?.let {
                add(SmsParseEvidence(SmsDetectedField.MERCHANT_OR_COUNTERPARTY, it))
            }
            paymentRail?.let { add(SmsParseEvidence(SmsDetectedField.PAYMENT_RAIL, it.name)) }
        }

        val missingFields = buildList {
            if (amountDetection == null) add(SmsMissingField.AMOUNT)
            if (amountDetection?.currencyCode == null) add(SmsMissingField.CURRENCY)
            if (direction == null) add(SmsMissingField.DIRECTION)
            if (accountHint == null) add(SmsMissingField.ACCOUNT)
            add(SmsMissingField.CATEGORY)
            if (direction == SmsTransactionDirection.TRANSFER_CANDIDATE) {
                add(SmsMissingField.DESTINATION_ACCOUNT)
            }
            if (occurredAtDetection.explicitTimestamp == null) add(SmsMissingField.TRANSACTION_TIME)
        }

        return SmsParseResult.Suggestion(
            SmsTransactionSuggestion(
                amountMinorUnits = amountDetection?.amountMinorUnits,
                currencyCode = amountDetection?.currencyCode,
                direction = direction,
                occurredAt = occurredAtDetection.resolvedTimestamp,
                accountHint = accountHint,
                merchantOrCounterparty = merchantOrCounterparty,
                paymentRail = paymentRail,
                confidence =
                    confidenceFor(
                        amountDetection = amountDetection,
                        direction = direction,
                        paymentRail = paymentRail,
                        explicitTimestamp = occurredAtDetection.explicitTimestamp,
                        accountHint = accountHint,
                    ),
                missingFields = missingFields,
                evidence = evidence,
            ),
        )
    }

    private fun classifyIgnored(message: String): SmsIgnoredReason? {
        val lower = message.lowercase()
        return when {
            OTP_MARKERS.any(lower::contains) -> SmsIgnoredReason.OTP_OR_SECURITY_CODE
            FAILURE_MARKERS.any(lower::contains) -> SmsIgnoredReason.FAILED_OR_DECLINED_TRANSACTION
            isBalanceOnly(lower) -> SmsIgnoredReason.BALANCE_ONLY
            isPromotional(lower) -> SmsIgnoredReason.PROMOTIONAL
            else -> null
        }
    }

    private fun isBalanceOnly(message: String): Boolean {
        val mentionsBalance = BALANCE_MARKERS.any(message::contains)
        val mentionsTransaction = EXPENSE_MARKERS.any(message::contains) || INCOME_MARKERS.any(message::contains)
        return mentionsBalance && !mentionsTransaction
    }

    private fun isPromotional(message: String): Boolean {
        val mentionsPromotion = PROMOTIONAL_MARKERS.any(message::contains)
        val mentionsTransaction = EXPENSE_MARKERS.any(message::contains) || INCOME_MARKERS.any(message::contains)
        return mentionsPromotion && !mentionsTransaction
    }

    private fun detectAmount(
        message: String,
        defaultCurrencyCode: String?,
    ): AmountDetection? {
        CURRENCY_BEFORE_AMOUNT.find(message)?.let { match ->
            val currency = currencyCodeFor(match.groupValues[1]) ?: return@let
            val amount = parseMinorUnits(match.groupValues[2]) ?: return@let
            return AmountDetection(amount, currency)
        }

        AMOUNT_BEFORE_CURRENCY.find(message)?.let { match ->
            val amount = parseMinorUnits(match.groupValues[1]) ?: return@let
            val currency = currencyCodeFor(match.groupValues[2]) ?: return@let
            return AmountDetection(amount, currency)
        }

        if (!defaultCurrencyCode.isNullOrBlank()) {
            UNMARKED_AMOUNT.find(message)?.let { match ->
                val amount = parseMinorUnits(match.groupValues[1]) ?: return@let
                return AmountDetection(amount, defaultCurrencyCode.trim().uppercase())
            }
        }

        return null
    }

    private fun parseMinorUnits(rawAmount: String): Long? {
        val normalized = rawAmount.replace(",", "")
        val parts = normalized.split('.')
        if (parts.size > 2) return null

        val majorUnits = parts[0].toLongOrNull() ?: return null
        val fraction = parts.getOrNull(1).orEmpty()
        if (fraction.length > 2) return null
        val fractionUnits =
            when (fraction.length) {
                0 -> 0L
                1 -> fraction.toLongOrNull()?.times(10L) ?: return null
                else -> fraction.toLongOrNull() ?: return null
            }

        if (majorUnits > Long.MAX_VALUE / 100L) return null
        if (majorUnits == Long.MAX_VALUE / 100L && fractionUnits > Long.MAX_VALUE % 100L) return null
        val minorUnits = majorUnits * 100L + fractionUnits
        return minorUnits.takeIf { it > 0L }
    }

    private fun currencyCodeFor(token: String): String? =
        when (token.trim().lowercase().removeSuffix(".")) {
            "₹", "rs", "inr" -> "INR"
            "$", "usd" -> "USD"
            "€", "eur" -> "EUR"
            "£", "gbp" -> "GBP"
            else -> null
        }

    private fun detectDirection(message: String): SmsTransactionDirection? {
        val lower = message.lowercase()
        return when {
            TRANSFER_MARKERS.any(lower::contains) -> SmsTransactionDirection.TRANSFER_CANDIDATE
            EXPENSE_MARKERS.any(lower::contains) -> SmsTransactionDirection.EXPENSE
            INCOME_MARKERS.any(lower::contains) -> SmsTransactionDirection.INCOME
            else -> null
        }
    }

    private fun detectPaymentRail(message: String): SmsPaymentRail? {
        val lower = message.lowercase()
        return when {
            "upi" in lower || "vpa" in lower -> SmsPaymentRail.UPI
            "atm" in lower || "cash withdrawal" in lower -> SmsPaymentRail.ATM
            "card" in lower || "pos" in lower -> SmsPaymentRail.CARD
            "wallet" in lower || "paytm" in lower || "mobikwik" in lower -> SmsPaymentRail.WALLET
            BANK_MARKERS.any(lower::contains) -> SmsPaymentRail.BANK
            else -> null
        }
    }

    private fun detectAccountHint(message: String): String? =
        ACCOUNT_HINT.find(message)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.length >= 3 }

    private fun detectMerchantOrCounterparty(
        message: String,
        direction: SmsTransactionDirection?,
    ): String? {
        val regex =
            when (direction) {
                SmsTransactionDirection.INCOME -> FROM_PARTY
                SmsTransactionDirection.EXPENSE, SmsTransactionDirection.TRANSFER_CANDIDATE -> TO_OR_AT_PARTY
                null -> TO_OR_AT_PARTY
            }
        return regex.find(message)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim(' ', '.', ',', ';', ':', '-')
            ?.takeIf { it.length in 2..60 }
    }

    private fun detectOccurredAt(
        message: String,
        request: SmsParseRequest,
    ): TimestampDetection {
        val explicit =
            DATE_TIME.find(message)?.let { match ->
                val day = match.groupValues[1].toIntOrNull() ?: return@let null
                val month = match.groupValues[2].toIntOrNull() ?: return@let null
                val year = normalizeYear(match.groupValues[3].toIntOrNull() ?: return@let null)
                val hour = match.groupValues[4].toIntOrNull() ?: 0
                val minute = match.groupValues[5].toIntOrNull() ?: 0
                val second = match.groupValues[6].toIntOrNull() ?: 0
                runCatching {
                    LocalDateTime(year, month, day, hour, minute, second)
                        .toInstant(TimeZone.of(request.timeZoneId))
                        .toEpochMilliseconds()
                }.getOrNull()
            }

        val fallback = request.receivedAt.takeIf { it >= 0L }
        return TimestampDetection(explicitTimestamp = explicit, resolvedTimestamp = explicit ?: fallback)
    }

    private fun normalizeYear(year: Int): Int =
        when (year) {
            in 0..69 -> 2000 + year
            in 70..99 -> 1900 + year
            else -> year
        }

    private fun confidenceFor(
        amountDetection: AmountDetection?,
        direction: SmsTransactionDirection?,
        paymentRail: SmsPaymentRail?,
        explicitTimestamp: Long?,
        accountHint: String?,
    ): SmsParseConfidence =
        when {
            amountDetection != null &&
                direction != null &&
                explicitTimestamp != null &&
                (accountHint != null || paymentRail != null) -> SmsParseConfidence.HIGH
            amountDetection != null && direction != null -> SmsParseConfidence.MEDIUM
            else -> SmsParseConfidence.LOW
        }

    private data class AmountDetection(
        val amountMinorUnits: Long,
        val currencyCode: String,
    )

    private data class TimestampDetection(
        val explicitTimestamp: Long?,
        val resolvedTimestamp: Long?,
    )

    private companion object {
        val CURRENCY_BEFORE_AMOUNT =
            Regex("(?i)(₹|rs\\.?|inr|usd|\\$|eur|€|gbp|£)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)")
        val AMOUNT_BEFORE_CURRENCY =
            Regex("(?i)([0-9][0-9,]*(?:\\.[0-9]{1,2})?)\\s*(inr|usd|eur|gbp)")
        val UNMARKED_AMOUNT = Regex("(?<![A-Za-z0-9])([0-9][0-9,]*(?:\\.[0-9]{1,2})?)(?![A-Za-z0-9])")
        val DATE_TIME =
            Regex("(?<!\\d)(\\d{1,2})[-/](\\d{1,2})[-/](\\d{2,4})(?:\\s+(\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?(?!\\d)")
        val ACCOUNT_HINT =
            Regex("(?i)(?:a/c|acct|account|card|wallet)(?:\\s*(?:no\\.?|ending)?)?\\s*[:*-]*\\s*(?:x+|\\*+)?([A-Za-z0-9-]{3,20})")
        val TO_OR_AT_PARTY =
            Regex("(?i)\\b(?:to|at)\\s+([A-Za-z0-9][A-Za-z0-9 &._'-]{1,59}?)(?=\\s+(?:on|ref|upi|avl|available|balance|via|using)\\b|[.;]|$)")
        val FROM_PARTY =
            Regex("(?i)\\bfrom\\s+([A-Za-z0-9][A-Za-z0-9 &._'-]{1,59}?)(?=\\s+(?:on|ref|upi|avl|available|balance|via|using)\\b|[.;]|$)")

        val OTP_MARKERS =
            listOf(" otp ", "one time password", "verification code", "security code", "do not share")
        val FAILURE_MARKERS =
            listOf("transaction failed", "payment failed", "declined", "unsuccessful", "could not be processed")
        val BALANCE_MARKERS =
            listOf("available balance", "avl bal", "current balance", "account balance", "balance is")
        val PROMOTIONAL_MARKERS =
            listOf("limited period offer", "exclusive offer", "discount", "shop now", "apply now", "pre-approved")
        val EXPENSE_MARKERS =
            listOf("debited", "spent", "purchase", "paid", "sent", "withdrawn", "cash withdrawal", "charged")
        val INCOME_MARKERS =
            listOf("credited", "received", "deposited", "refund", "cashback credited")
        val TRANSFER_MARKERS =
            listOf("transferred from", "fund transfer from", "transfer between")
        val BANK_MARKERS =
            listOf("bank", "a/c", "acct", "account", "neft", "imps", "rtgs")
    }
}

private fun String.normalizeMessage(): String =
    trim()
        .replace(Regex("\\s+"), " ")
        .let { " $it " }
