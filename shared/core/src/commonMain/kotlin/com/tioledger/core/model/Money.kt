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
        return Money(this.amount.checkedAdd(other.amount), this.currency)
    }

    operator fun minus(other: Money): Money {
        require(this.currency == other.currency) {
            "Currency mismatch: ${this.currency} vs ${other.currency}"
        }
        return Money(this.amount.checkedSubtract(other.amount), this.currency)
    }

    operator fun unaryMinus(): Money {
        return Money(this.amount.checkedNegate(), this.currency)
    }

    operator fun times(factor: Long): Money {
        return Money(this.amount.checkedMultiply(factor), this.currency)
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

private const val LONG_OVERFLOW_MESSAGE = "Long overflow"

private fun Long.checkedAdd(other: Long): Long {
    if (other > 0L && this > Long.MAX_VALUE - other) {
        throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    }
    if (other < 0L && this < Long.MIN_VALUE - other) {
        throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    }
    return this + other
}

private fun Long.checkedSubtract(other: Long): Long {
    if (other > 0L && this < Long.MIN_VALUE + other) {
        throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    }
    if (other < 0L && this > Long.MAX_VALUE + other) {
        throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    }
    return this - other
}

private fun Long.checkedNegate(): Long {
    if (this == Long.MIN_VALUE) {
        throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    }
    return -this
}

private fun Long.checkedMultiply(other: Long): Long {
    if (this == 0L || other == 0L) {
        return 0L
    }
    if (this == Long.MIN_VALUE && other == -1L) {
        throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    }
    if (other == Long.MIN_VALUE && this == -1L) {
        throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    }

    val result = this * other
    if (result / other != this) {
        throw ArithmeticException(LONG_OVERFLOW_MESSAGE)
    }
    return result
}
