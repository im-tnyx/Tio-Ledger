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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CurrencyCode) return false
        return this.normalized == other.normalized
    }

    override fun hashCode(): Int {
        return normalized.hashCode()
    }
}
