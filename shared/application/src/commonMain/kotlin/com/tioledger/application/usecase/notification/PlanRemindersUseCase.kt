package com.tioledger.application.usecase.notification

import com.tioledger.application.internal.validateTimestamp
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.application.usecase.budget.BudgetSummary
import com.tioledger.application.usecase.budget.ListBudgetSummariesUseCase
import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanStatus
import com.tioledger.domain.repository.LoanRepository
import com.tioledger.notifications.BudgetReminderCandidate
import com.tioledger.notifications.EmiReminderCandidate
import com.tioledger.notifications.ReminderContent
import com.tioledger.notifications.ReminderDestination
import com.tioledger.notifications.ReminderPlan
import com.tioledger.notifications.ReminderPlanner
import com.tioledger.notifications.ReminderPlanningContext
import com.tioledger.notifications.ReminderPlanningError
import com.tioledger.notifications.ReminderPlanningResult
import com.tioledger.notifications.ReminderPreferencesSnapshot
import com.tioledger.notifications.ReminderType
import kotlinx.datetime.TimeZone

data class PlanRemindersCommand(
    val currentTimestamp: Long,
    val timeZoneId: String,
    val emiRemindersEnabled: Boolean,
    val budgetRemindersEnabled: Boolean,
    val deliveredBudgetIdentityKeys: Set<String> = emptySet(),
)

enum class ReminderPlanTypeView {
    EMI,
    BUDGET,
}

enum class BudgetReminderStatusView {
    ON_TRACK,
    WARNING,
    REACHED,
    EXCEEDED,
}

sealed interface ReminderDestinationView {
    data class LoanDetails(val loanId: String) : ReminderDestinationView

    data object Budgets : ReminderDestinationView
}

sealed interface ReminderContentView {
    data class Emi(
        val loanName: String,
        val dueDate: Long,
        val payment: Money,
    ) : ReminderContentView

    data class Budget(
        val budgetName: String,
        val status: BudgetReminderStatusView,
        val target: Money,
        val spent: Money,
        val remaining: Money,
    ) : ReminderContentView
}

data class ReminderPlanView(
    val identityKey: String,
    val type: ReminderPlanTypeView,
    val deliveryTimestamp: Long,
    val timeZoneId: String,
    val destination: ReminderDestinationView,
    val content: ReminderContentView,
)

class PlanRemindersUseCase(
    private val loanRepository: LoanRepository,
    private val listBudgetSummariesUseCase: ListBudgetSummariesUseCase,
    private val reminderPlanner: ReminderPlanner,
) {
    operator fun invoke(command: PlanRemindersCommand): ApplicationResult<List<ReminderPlanView>> {
        validateCommand(command)?.let { return ApplicationResult.Failure(it) }

        val emiCandidates =
            if (command.emiRemindersEnabled) {
                when (val result = loadEmiCandidates()) {
                    is CandidateLoadResult.Success -> result.values
                    is CandidateLoadResult.Failure -> return ApplicationResult.Failure(result.error)
                }
            } else {
                emptyList()
            }
        val budgetCandidates =
            if (command.budgetRemindersEnabled) {
                when (
                    val result =
                        listBudgetSummariesUseCase(
                            anchorTimestamp = command.currentTimestamp,
                            timeZoneId = command.timeZoneId,
                        )
                ) {
                    is ApplicationResult.Success -> result.outcome.value.map(BudgetSummary::toReminderCandidate)
                    is ApplicationResult.Failure -> return result
                }
            } else {
                emptyList()
            }

        return when (
            val result =
                reminderPlanner.plan(
                    emiCandidates = emiCandidates,
                    budgetCandidates = budgetCandidates,
                    preferences =
                        ReminderPreferencesSnapshot(
                            emiRemindersEnabled = command.emiRemindersEnabled,
                            budgetRemindersEnabled = command.budgetRemindersEnabled,
                        ),
                    context =
                        ReminderPlanningContext(
                            currentTimestamp = command.currentTimestamp,
                            timeZoneId = command.timeZoneId,
                            deliveredBudgetIdentityKeys = command.deliveredBudgetIdentityKeys,
                        ),
                )
        ) {
            is ReminderPlanningResult.Success ->
                ApplicationResult.Success(
                    UseCaseOutcome(value = result.plans.map(ReminderPlan::toView)),
                )
            is ReminderPlanningResult.Failure ->
                ApplicationResult.Failure(result.error.toApplicationError())
        }
    }

    private fun loadEmiCandidates(): CandidateLoadResult<EmiReminderCandidate> {
        val loans =
            when (val result = loanRepository.findAll()) {
                is LedgerResult.Success ->
                    result.value.filter {
                        it.deletedAt == null && it.status == LoanStatus.ACTIVE
                    }
                is LedgerResult.Failure -> {
                    return CandidateLoadResult.Failure(ApplicationError.Repository(result.error))
                }
            }

        val candidates = mutableListOf<EmiReminderCandidate>()
        loans.forEach { loan ->
            val details =
                when (val result = loanRepository.findDetails(loan.id)) {
                    is LedgerResult.Success -> result.value
                    is LedgerResult.Failure -> {
                        return CandidateLoadResult.Failure(ApplicationError.Repository(result.error))
                    }
                }
            candidates += details.toReminderCandidates()
        }
        return CandidateLoadResult.Success(candidates)
    }

    private fun validateCommand(command: PlanRemindersCommand): ApplicationError.Validation? {
        validateTimestamp(command.currentTimestamp, "currentTimestamp")?.let { return it }
        if (command.timeZoneId.isBlank()) {
            return ApplicationError.Validation("timeZoneId", "must not be blank")
        }
        return try {
            TimeZone.of(command.timeZoneId)
            null
        } catch (error: IllegalArgumentException) {
            ApplicationError.Validation(
                field = "timeZoneId",
                reason = error.message ?: "invalid time zone",
            )
        }
    }

    private sealed interface CandidateLoadResult<out T> {
        data class Success<T>(val values: List<T>) : CandidateLoadResult<T>

        data class Failure(val error: ApplicationError) : CandidateLoadResult<Nothing>
    }
}

private fun LoanDetails.toReminderCandidates(): List<EmiReminderCandidate> =
    schedule.map { installment ->
        EmiReminderCandidate(
            loanId = loan.id,
            loanName = loan.name,
            loanStatus = loan.status,
            installmentId = installment.id,
            dueDate = installment.dueDate,
            installmentStatus = installment.status,
            payment = installment.payment,
        )
    }

private fun BudgetSummary.toReminderCandidate(): BudgetReminderCandidate =
    BudgetReminderCandidate(
        budgetId = id,
        budgetName = name,
        periodStartInclusive = periodStartInclusive,
        status = status,
        target = target,
        spent = spent,
        remaining = remaining,
    )

private fun ReminderPlan.toView(): ReminderPlanView =
    ReminderPlanView(
        identityKey = identity.key,
        type =
            when (type) {
                ReminderType.EMI -> ReminderPlanTypeView.EMI
                ReminderType.BUDGET -> ReminderPlanTypeView.BUDGET
            },
        deliveryTimestamp = deliveryTimestamp,
        timeZoneId = timeZoneId,
        destination = destination.toView(),
        content = content.toView(),
    )

private fun ReminderDestination.toView(): ReminderDestinationView =
    when (this) {
        is ReminderDestination.LoanDetails -> ReminderDestinationView.LoanDetails(loanId)
        ReminderDestination.Budgets -> ReminderDestinationView.Budgets
    }

private fun ReminderContent.toView(): ReminderContentView =
    when (this) {
        is ReminderContent.Emi ->
            ReminderContentView.Emi(
                loanName = loanName,
                dueDate = dueDate,
                payment = payment,
            )
        is ReminderContent.Budget ->
            ReminderContentView.Budget(
                budgetName = budgetName,
                status = status.toView(),
                target = target,
                spent = spent,
                remaining = remaining,
            )
    }

private fun BudgetProgressStatus.toView(): BudgetReminderStatusView =
    when (this) {
        BudgetProgressStatus.ON_TRACK -> BudgetReminderStatusView.ON_TRACK
        BudgetProgressStatus.WARNING -> BudgetReminderStatusView.WARNING
        BudgetProgressStatus.REACHED -> BudgetReminderStatusView.REACHED
        BudgetProgressStatus.EXCEEDED -> BudgetReminderStatusView.EXCEEDED
    }

private fun ReminderPlanningError.toApplicationError(): ApplicationError =
    when (this) {
        is ReminderPlanningError.InvalidInput ->
            if (field == "currentTimestamp" || field == "timeZoneId") {
                ApplicationError.Validation(field = field, reason = reason)
            } else {
                ApplicationError.Ledger(
                    LedgerError.Unknown("invalid reminder candidate $field: $reason"),
                )
            }
    }
