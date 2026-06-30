package com.tioledger.application.model

import com.tioledger.core.model.LedgerError
import com.tioledger.domain.event.DomainEvent

sealed interface ApplicationResult<out T> {
    data class Success<out T>(val outcome: UseCaseOutcome<T>) : ApplicationResult<T>

    data class Failure(val error: ApplicationError) : ApplicationResult<Nothing>

    fun isSuccess(): Boolean = this is Success

    fun isFailure(): Boolean = this is Failure

    fun getOrNull(): T? =
        when (this) {
            is Success -> outcome.value
            is Failure -> null
        }

    fun errorOrNull(): ApplicationError? =
        when (this) {
            is Success -> null
            is Failure -> error
        }
}

data class UseCaseOutcome<out T>(
    val value: T,
    val events: List<DomainEvent> = emptyList(),
)

sealed interface ApplicationError {
    data class Validation(val field: String, val reason: String) : ApplicationError

    data class Repository(val error: LedgerError) : ApplicationError

    data class Ledger(val error: LedgerError) : ApplicationError
}
