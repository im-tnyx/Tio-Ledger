package com.tioledger.application.usecase.loan

import com.tioledger.analytics.LoanPayoffAnalytics
import com.tioledger.analytics.LoanPayoffAnalyticsCalculator
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.Money
import com.tioledger.domain.model.LoanDetails

data class LoanPayoffAnalyticsView(
    val originalPrincipal: Money,
    val principalPaid: Money,
    val principalRemaining: Money,
    val principalProgressBasisPoints: Int,
    val interestPaid: Money,
    val interestRemaining: Money,
    val totalScheduledInterest: Money,
    val amountPaid: Money,
    val amountRemaining: Money,
    val paidInstallments: Int,
    val remainingInstallments: Int,
    val nextDueDate: Long?,
    val projectedPayoffDate: Long?,
)

data class LoanDetailsAnalyticsView(
    val details: LoanDetailsView,
    val payoff: LoanPayoffAnalyticsView,
)

private typealias LoanDetailsSuccess = ApplicationResult.Success<LoanDetailsView>
private typealias LoanDetailsAnalyticsResult = ApplicationResult<LoanDetailsAnalyticsView>

class GetLoanDetailsAnalyticsUseCase(
    private val getLoanDetailsUseCase: GetLoanDetailsUseCase,
    private val payoffCalculator: LoanPayoffAnalyticsCalculator,
) {
    operator fun invoke(loanId: String): ApplicationResult<LoanDetailsAnalyticsView> =
        when (val result = getLoanDetailsUseCase(loanId)) {
            is ApplicationResult.Success -> calculate(result)
            is ApplicationResult.Failure -> result
        }

    private fun calculate(result: LoanDetailsSuccess): LoanDetailsAnalyticsResult {
        return try {
            val details = result.outcome.value
            val payoff =
                payoffCalculator.calculate(
                    LoanDetails(
                        loan = details.overview.loan,
                        schedule = details.schedule,
                    ),
                )
            ApplicationResult.Success(
                UseCaseOutcome(
                    value =
                        LoanDetailsAnalyticsView(
                            details = details,
                            payoff = payoff.toApplicationView(),
                        ),
                    events = result.outcome.events,
                ),
            )
        } catch (_: ArithmeticException) {
            ApplicationResult.Failure(
                ApplicationError.Ledger(
                    LedgerError.Unknown("loan payoff analytics exceed the supported numeric range"),
                ),
            )
        } catch (_: IllegalArgumentException) {
            ApplicationResult.Failure(
                ApplicationError.Ledger(
                    LedgerError.Unknown("loan payoff analytics could not be calculated"),
                ),
            )
        }
    }
}

private fun LoanPayoffAnalytics.toApplicationView(): LoanPayoffAnalyticsView =
    LoanPayoffAnalyticsView(
        originalPrincipal = originalPrincipal,
        principalPaid = principalPaid,
        principalRemaining = principalRemaining,
        principalProgressBasisPoints = principalProgressBasisPoints,
        interestPaid = interestPaid,
        interestRemaining = interestRemaining,
        totalScheduledInterest = totalScheduledInterest,
        amountPaid = amountPaid,
        amountRemaining = amountRemaining,
        paidInstallments = paidInstallments,
        remainingInstallments = remainingInstallments,
        nextDueDate = nextDueDate,
        projectedPayoffDate = projectedPayoffDate,
    )
