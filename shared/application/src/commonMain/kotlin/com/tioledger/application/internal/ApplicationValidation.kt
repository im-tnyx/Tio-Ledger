package com.tioledger.application.internal

import com.tioledger.application.model.ApplicationError

internal fun validateId(
    value: String,
    field: String,
): ApplicationError.Validation? {
    return if (value.isBlank()) {
        ApplicationError.Validation(field, "must not be blank")
    } else {
        null
    }
}

internal fun validateName(
    value: String,
    field: String = "name",
): ApplicationError.Validation? {
    return if (value.isBlank()) {
        ApplicationError.Validation(field, "must not be blank")
    } else {
        null
    }
}

internal fun validateOptionalId(
    value: String?,
    field: String,
): ApplicationError.Validation? {
    return if (value != null && value.isBlank()) {
        ApplicationError.Validation(field, "must not be blank when provided")
    } else {
        null
    }
}

internal fun validateTimestamp(
    value: Long,
    field: String,
): ApplicationError.Validation? {
    return if (value < 0L) {
        ApplicationError.Validation(field, "must be zero or greater")
    } else {
        null
    }
}

internal fun validateCurrencyCode(value: String): ApplicationError.Validation? {
    val normalized = value.trim().uppercase()
    return when {
        value.isBlank() -> ApplicationError.Validation("currencyCode", "must not be blank")
        normalized.length != 3 -> ApplicationError.Validation("currencyCode", "must be exactly 3 letters")
        !normalized.all { it in 'A'..'Z' } -> ApplicationError.Validation("currencyCode", "must contain only letters")
        else -> null
    }
}

internal fun normalizedCurrencyCode(value: String): String = value.trim().uppercase()

internal fun normalizedId(value: String): String = value.trim()
