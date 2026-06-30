package com.tioledger.data.result

sealed interface DataResult<out T> {
    data class Success<out T>(val value: T) : DataResult<T>

    data class Failure(val error: DataError) : DataResult<Nothing>
}

sealed interface DataError {
    data class SqlDelightError(val message: String, val cause: Throwable? = null) : DataError

    data class ConstraintViolation(val message: String, val cause: Throwable? = null) : DataError

    data class DuplicateTransaction(val transactionId: String, val cause: Throwable? = null) : DataError

    data class DatabaseFailure(val message: String, val cause: Throwable? = null) : DataError
}
