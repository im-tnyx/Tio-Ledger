package com.tioledger.application.usecase.loan

import com.tioledger.analytics.LoanPayoffAnalytics
import com.tioledger.analytics.LoanPayoffAnalyticsCalculator
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.LedgerError
import com.tioledger.domain.model.LoanDetails

data class LoanDetailsAnalyticsView(
    val details: LoanDetailsView,
    val payoff: LoanPayoffAnalytics,
)

class GetLoanDetailsAnalyticsUseCase(
    private val getLoanDetailsUseCase: GetLoanDetailsUseCase,
    private val payoffCalculator: LoanPayoffAnalyticsCalculator,
) {
    operator fun invoke(loanId: String): ApplicationResult<LoanDetailsAnalyticsView> =
        when (val result = getLoanDetailsUseCase(loanId)) {
            is ApplicationResult.Success -> calculate(result)
            is ApplicationResult.Failure -> result
        }

    private fun calculate(
        result: ApplicationResult.Success<LoanDetailsView>,
    ): ApplicationResult<LoanDetailsAnalyticsView> =
        try {
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
                    value = LoanDetailsAnalyticsView(details = details, payoff = payoff),
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
