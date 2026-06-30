package com.tioledger.application.internal

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.event.DomainEvent

internal inline fun <T, R> LedgerResult<T>.mapRepositoryResult(
    events: (T) -> List<DomainEvent> = { emptyList() },
    transform: (T) -> R,
): ApplicationResult<R> =
    when (this) {
        is LedgerResult.Success -> ApplicationResult.Success(UseCaseOutcome(transform(value), events(value)))
        is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(error))
    }
