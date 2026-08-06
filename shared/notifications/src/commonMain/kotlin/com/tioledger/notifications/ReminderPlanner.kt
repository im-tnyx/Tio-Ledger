package com.tioledger.notifications

import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanStatus
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

class ReminderPlanner {
    fun plan(
        emiCandidates: List<EmiReminderCandidate>,
        budgetCandidates: List<BudgetReminderCandidate>,
        preferences: ReminderPreferencesSnapshot,
        context: ReminderPlanningContext,
    ): ReminderPlanningResult {
        validateContext(context)?.let { return ReminderPlanningResult.Failure(it) }
        if (preferences.emiRemindersEnabled) {
            validateEmiCandidates(emiCandidates)?.let { return ReminderPlanningResult.Failure(it) }
        }
        if (preferences.budgetRemindersEnabled) {
            validateBudgetCandidates(budgetCandidates)?.let { return ReminderPlanningResult.Failure(it) }
        }

        val timeZone =
            try {
                TimeZone.of(context.timeZoneId)
            } catch (error: IllegalArgumentException) {
                return ReminderPlanningResult.Failure(
                    ReminderPlanningError.InvalidInput(
                        field = "timeZoneId",
                        reason = error.message ?: "invalid time zone",
                    ),
                )
            }

        return try {
            val plans = mutableListOf<ReminderPlan>()
            if (preferences.emiRemindersEnabled) {
                plans += planEmiReminders(emiCandidates, context, timeZone)
            }
            if (preferences.budgetRemindersEnabled) {
                plans += planBudgetReminders(budgetCandidates, context)
            }
            ReminderPlanningResult.Success(
                plans =
                    plans.sortedWith(
                        compareBy<ReminderPlan> { it.deliveryTimestamp }
                            .thenBy { it.identity.key },
                    ),
            )
        } catch (error: IllegalArgumentException) {
            ReminderPlanningResult.Failure(
                ReminderPlanningError.InvalidInput(
                    field = "candidate",
                    reason = error.message ?: "invalid reminder candidate",
                ),
            )
        } catch (error: ArithmeticException) {
            ReminderPlanningResult.Failure(
                ReminderPlanningError.InvalidInput(
                    field = "candidate",
                    reason = error.message ?: "reminder date overflow",
                ),
            )
        }
    }

    private fun planEmiReminders(
        candidates: List<EmiReminderCandidate>,
        context: ReminderPlanningContext,
        timeZone: TimeZone,
    ): List<ReminderPlan> =
        candidates
            .asSequence()
            .filter { it.loanStatus == LoanStatus.ACTIVE }
            .filter { it.installmentStatus == LoanInstallmentStatus.PENDING }
            .flatMap { candidate ->
                val dueDate =
                    Instant
                        .fromEpochMilliseconds(candidate.dueDate)
                        .toLocalDateTime(TimeZone.UTC)
                        .date
                EMI_LEAD_DAYS.asSequence().mapNotNull { leadDays ->
                    candidate.toPlan(
                        dueDate = dueDate,
                        leadDays = leadDays,
                        context = context,
                        timeZone = timeZone,
                    )
                }
            }.toList()

    private fun EmiReminderCandidate.toPlan(
        dueDate: LocalDate,
        leadDays: Int,
        context: ReminderPlanningContext,
        timeZone: TimeZone,
    ): ReminderPlan? {
        val deliveryDate = LocalDate.fromEpochDays(dueDate.toEpochDays() - leadDays)
        val deliveryTimestamp =
            LocalDateTime(
                date = deliveryDate,
                time = LocalTime(hour = DELIVERY_HOUR, minute = 0),
            ).toInstant(timeZone)
                .toEpochMilliseconds()
        if (deliveryTimestamp < context.currentTimestamp) return null

        return ReminderPlan(
            identity =
                ReminderIdentity.Emi(
                    loanId = loanId,
                    installmentId = installmentId,
                    leadDays = leadDays,
                ),
            type = ReminderType.EMI,
            deliveryTimestamp = deliveryTimestamp,
            timeZoneId = context.timeZoneId,
            destination = ReminderDestination.LoanDetails(loanId),
            content =
                ReminderContent.Emi(
                    loanName = loanName,
                    dueDate = dueDate.atUtcStartOfDay(),
                    payment = payment,
                ),
        )
    }

    private fun planBudgetReminders(
        candidates: List<BudgetReminderCandidate>,
        context: ReminderPlanningContext,
    ): List<ReminderPlan> =
        candidates
            .asSequence()
            .filter { it.status in ELIGIBLE_BUDGET_STATUSES }
            .map { candidate -> candidate.toPlan(context) }
            .filterNot { it.identity.key in context.deliveredBudgetIdentityKeys }
            .toList()

    private fun BudgetReminderCandidate.toPlan(context: ReminderPlanningContext): ReminderPlan =
        ReminderPlan(
            identity =
                ReminderIdentity.Budget(
                    budgetId = budgetId,
                    periodStartInclusive = periodStartInclusive,
                    status = status,
                ),
            type = ReminderType.BUDGET,
            deliveryTimestamp = context.currentTimestamp,
            timeZoneId = context.timeZoneId,
            destination = ReminderDestination.Budgets,
            content =
                ReminderContent.Budget(
                    budgetName = budgetName,
                    status = status,
                    target = target,
                    spent = spent,
                    remaining = remaining,
                ),
        )

    private fun validateContext(context: ReminderPlanningContext): ReminderPlanningError.InvalidInput? =
        when {
            context.currentTimestamp < 0L ->
                ReminderPlanningError.InvalidInput(
                    field = "currentTimestamp",
                    reason = "must be zero or greater",
                )
            context.timeZoneId.isBlank() ->
                ReminderPlanningError.InvalidInput(
                    field = "timeZoneId",
                    reason = "must not be blank",
                )
            else -> null
        }

    private fun validateEmiCandidates(
        candidates: List<EmiReminderCandidate>,
    ): ReminderPlanningError.InvalidInput? {
        candidates.forEachIndexed { index, candidate ->
            when {
                candidate.loanId.isBlank() -> return invalidCandidate("emiCandidates[$index].loanId")
                candidate.installmentId.isBlank() -> return invalidCandidate("emiCandidates[$index].installmentId")
                candidate.loanName.isBlank() -> return invalidCandidate("emiCandidates[$index].loanName")
                candidate.dueDate < 0L -> return invalidCandidate("emiCandidates[$index].dueDate")
            }
        }
        return null
    }

    private fun validateBudgetCandidates(
        candidates: List<BudgetReminderCandidate>,
    ): ReminderPlanningError.InvalidInput? {
        candidates.forEachIndexed { index, candidate ->
            when {
                candidate.budgetId.isBlank() -> return invalidCandidate("budgetCandidates[$index].budgetId")
                candidate.budgetName.isBlank() -> return invalidCandidate("budgetCandidates[$index].budgetName")
                candidate.periodStartInclusive < 0L -> {
                    return invalidCandidate("budgetCandidates[$index].periodStartInclusive")
                }
            }
        }
        return null
    }

    private fun invalidCandidate(field: String): ReminderPlanningError.InvalidInput =
        ReminderPlanningError.InvalidInput(
            field = field,
            reason = "must be valid",
        )

    private fun LocalDate.atUtcStartOfDay(): Long =
        LocalDateTime(
            date = this,
            time = LocalTime(hour = 0, minute = 0),
        ).toInstant(TimeZone.UTC)
            .toEpochMilliseconds()

    private companion object {
        const val DELIVERY_HOUR = 9
        val EMI_LEAD_DAYS = listOf(3, 0)
        val ELIGIBLE_BUDGET_STATUSES =
            setOf(
                BudgetProgressStatus.WARNING,
                BudgetProgressStatus.REACHED,
                BudgetProgressStatus.EXCEEDED,
            )
    }
}
