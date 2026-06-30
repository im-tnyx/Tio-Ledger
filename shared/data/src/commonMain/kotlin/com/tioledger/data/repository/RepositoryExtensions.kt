package com.tioledger.data.repository

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.data.result.DataError
import com.tioledger.data.result.DataResult

fun <T> DataResult<T>.toLedgerResult(mapConstraint: (String) -> LedgerError = { LedgerError.ConstraintViolation }): LedgerResult<T> {
    return when (this) {
        is DataResult.Success -> LedgerResult.Success(value)
        is DataResult.Failure -> {
            val domainError =
                when (val err = this.error) {
                    is DataError.SqlDelightError -> LedgerError.StorageUnavailable
                    is DataError.ConstraintViolation -> mapConstraint(err.message)
                    is DataError.DuplicateTransaction -> LedgerError.DuplicateTransactionId(err.transactionId)
                    is DataError.DatabaseFailure -> LedgerError.StorageUnavailable
                }
            LedgerResult.Failure(domainError)
        }
    }
}

inline fun <T> runDatabaseCatching(block: () -> T): DataResult<T> {
    return try {
        DataResult.Success(block())
    } catch (e: Exception) {
        val msg = e.message ?: "Database execution failed"
        if (msg.contains("CONSTRAINT", ignoreCase = true) || msg.contains("foreign key", ignoreCase = true)) {
            DataResult.Failure(DataError.ConstraintViolation(msg, e))
        } else {
            DataResult.Failure(DataError.DatabaseFailure(msg, e))
        }
    }
}
