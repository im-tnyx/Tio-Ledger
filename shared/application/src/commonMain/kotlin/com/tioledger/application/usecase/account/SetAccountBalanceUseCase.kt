package com.tioledger.application.usecase.account

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.application.usecase.transaction.RecordAdjustmentCommand
import com.tioledger.application.usecase.transaction.RecordAdjustmentUseCase
import com.tioledger.core.model.Money
import com.tioledger.domain.model.LedgerClass
import kotlin.math.abs

data class SetAccountBalanceCommand(
    val accountId: String,
    val targetBalance: Money,
    val timestamp: Long,
)

class SetAccountBalanceUseCase(
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
    private val recordAdjustmentUseCase: RecordAdjustmentUseCase,
) {
    operator fun invoke(command: SetAccountBalanceCommand): ApplicationResult<Unit> {
        val overview =
            when (val result = listAccountSummariesUseCase()) {
                is ApplicationResult.Success -> result.outcome.value
                is ApplicationResult.Failure -> return result
            }
        val summary =
            overview.accounts.firstOrNull { it.account.id == command.accountId }
                ?: return ApplicationResult.Failure(ApplicationError.Validation("accountId", "account is no longer available"))
        if (command.targetBalance.currency != summary.balance.currency) {
            return ApplicationResult.Failure(ApplicationError.Validation("balance", "currency must match account currency"))
        }
        val current = summary.balance.amount
        val target = command.targetBalance.amount
        if ((current < 0L && target > Long.MAX_VALUE + current) ||
            (current > 0L && target < Long.MIN_VALUE + current)
        ) {
            return ApplicationResult.Failure(ApplicationError.Validation("balance", "difference is too large to record"))
        }
        val delta = target - current
        if (delta == Long.MIN_VALUE) {
            return ApplicationResult.Failure(ApplicationError.Validation("balance", "difference is too large to record"))
        }
        if (delta == 0L) return ApplicationResult.Success(UseCaseOutcome(Unit))

        val isDebit = if (summary.account.type.ledgerClass == LedgerClass.ASSET) delta > 0L else delta < 0L
        return when (
            val result =
                recordAdjustmentUseCase(
                    RecordAdjustmentCommand(
                        timestamp = command.timestamp,
                        description = "Account balance correction",
                        amount = Money(abs(delta), command.targetBalance.currency),
                        accountId = command.accountId,
                        isDebit = isDebit,
                        createdAt = command.timestamp,
                    ),
                )
        ) {
            is ApplicationResult.Success -> ApplicationResult.Success(UseCaseOutcome(Unit))
            is ApplicationResult.Failure -> result
        }
    }
}
