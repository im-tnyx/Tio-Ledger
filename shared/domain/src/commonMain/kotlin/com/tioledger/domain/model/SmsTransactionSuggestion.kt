package com.tioledger.domain.model

/**
 * Direction inferred from message text. Transfer remains a candidate until the user
 * selects both accounts and confirms the normal transfer workflow.
 */
enum class SmsTransactionDirection {
    INCOME,
    EXPENSE,
    TRANSFER_CANDIDATE,
}

enum class SmsPaymentRail {
    BANK,
    CARD,
    UPI,
    WALLET,
    ATM,
}

enum class SmsParseConfidence {
    HIGH,
    MEDIUM,
    LOW,
}

enum class SmsMissingField {
    AMOUNT,
    CURRENCY,
    DIRECTION,
    ACCOUNT,
    CATEGORY,
    DESTINATION_ACCOUNT,
    TRANSACTION_TIME,
}

enum class SmsDetectedField {
    AMOUNT,
    CURRENCY,
    DIRECTION,
    TRANSACTION_TIME,
    ACCOUNT_HINT,
    MERCHANT_OR_COUNTERPARTY,
    PAYMENT_RAIL,
}

data class SmsParseEvidence(
    val field: SmsDetectedField,
    val value: String,
)

data class SmsTransactionSuggestion(
    val amountMinorUnits: Long?,
    val currencyCode: String?,
    val direction: SmsTransactionDirection?,
    val occurredAt: Long?,
    val accountHint: String?,
    val merchantOrCounterparty: String?,
    val paymentRail: SmsPaymentRail?,
    val confidence: SmsParseConfidence,
    val missingFields: List<SmsMissingField>,
    val evidence: List<SmsParseEvidence>,
)

enum class SmsIgnoredReason {
    OTP_OR_SECURITY_CODE,
    PROMOTIONAL,
    FAILED_OR_DECLINED_TRANSACTION,
    BALANCE_ONLY,
}

enum class SmsUnsupportedReason {
    EMPTY_MESSAGE,
    INSUFFICIENT_EVIDENCE,
    UNSUPPORTED_FORMAT,
}

sealed interface SmsParseResult {
    data class Suggestion(
        val value: SmsTransactionSuggestion,
    ) : SmsParseResult

    data class Ignored(
        val reason: SmsIgnoredReason,
        val evidence: List<SmsParseEvidence> = emptyList(),
    ) : SmsParseResult

    data class Unsupported(
        val reason: SmsUnsupportedReason,
        val missingFields: List<SmsMissingField> = emptyList(),
        val evidence: List<SmsParseEvidence> = emptyList(),
    ) : SmsParseResult
}

/**
 * Ephemeral parser input. Raw message text must not be copied into parse results or
 * persisted by default.
 */
data class SmsParseRequest(
    val message: String,
    val receivedAt: Long,
    val timeZoneId: String,
    val defaultCurrencyCode: String? = null,
)

fun interface SmsTransactionParser {
    fun parse(request: SmsParseRequest): SmsParseResult
}
