package com.tioledger.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CurrencyCode(val code: String) {
    val normalized: String = code.uppercase()

    init {
        require(normalized.length == 3) { "Currency code must be exactly 3 characters: $code" }
        require(normalized.all { it in 'A'..'Z' }) { "Currency code must contain only alphabetic letters: $code" }
    }

    override fun toString(): String = normalized
}
