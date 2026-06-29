package com.tioledger.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Money(
    val amount: Long,
    val currency: CurrencyCode,
) : Comparable<Money> {
    operator fun plus(other: Money): Money {
        require(this.currency == other.currency) {
            "Currency mismatch: ${this.currency} vs ${other.currency}"
        }
        return Money(this.amount + other.amount, this.currency)
    }

    operator fun minus(other: Money): Money {
        require(this.currency == other.currency) {
            "Currency mismatch: ${this.currency} vs ${other.currency}"
        }
        return Money(this.amount - other.amount, this.currency)
    }

    operator fun unaryMinus(): Money {
        return Money(-this.amount, this.currency)
    }

    operator fun times(factor: Long): Money {
        return Money(this.amount * factor, this.currency)
    }

    override operator fun compareTo(other: Money): Int {
        require(this.currency == other.currency) {
            "Currency mismatch: ${this.currency} vs ${other.currency}"
        }
        return this.amount.compareTo(other.amount)
    }

    fun isPositive(): Boolean = amount > 0L

    fun isNegative(): Boolean = amount < 0L

    fun isZero(): Boolean = amount == 0L

    companion object {
        fun zero(currency: CurrencyCode): Money = Money(0L, currency)
    }
}
