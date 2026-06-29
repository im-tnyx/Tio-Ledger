package com.tioledger.core.model

sealed interface LedgerResult<out T> {
    data class Success<out T>(val value: T) : LedgerResult<T>

    data class Failure(val error: LedgerError) : LedgerResult<Nothing>

    fun isSuccess(): Boolean = this is Success

    fun isFailure(): Boolean = this is Failure

    fun getOrNull(): T? =
        when (this) {
            is Success -> value
            is Failure -> null
        }

    fun errorOrNull(): LedgerError? =
        when (this) {
            is Success -> null
            is Failure -> error
        }
}
