package com.tioledger.notifications

import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.core.model.Money
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanStatus

enum class ReminderType {
    EMI,
    BUDGET,
}

sealed interface ReminderIdentity {
    val key: String

    data class Emi(
        val loanId: String,
        val installmentId: String,
        val leadDays: Int,
    ) : ReminderIdentity {
        override val key: String = "emi|$loanId|$installmentId|$leadDays"
    }

    data class Budget(
        val budgetId: String,
        val periodStartInclusive: Long,
        val status: BudgetProgressStatus,
    ) : ReminderIdentity {
        override val key: String = "budget|$budgetId|$periodStartInclusive|${status.name}"
    }
}

sealed interface ReminderDestination {
    data class LoanDetails(val loanId: String) : ReminderDestination

    data object Budgets : ReminderDestination
}

sealed interface ReminderContent {
    data class Emi(
        val loanName: String,
        val dueDate: Long,
        val payment: Money,
    ) : ReminderContent

    data class Budget(
        val budgetName: String,
        val status: BudgetProgressStatus,
        val target: Money,
        val spent: Money,
        val remaining: Money,
    ) : ReminderContent
}

data class ReminderPlan(
    val identity: ReminderIdentity,
    val type: ReminderType,
    val deliveryTimestamp: Long,
    val timeZoneId: String,
    val destination: ReminderDestination,
    val content: ReminderContent,
)

data class EmiReminderCandidate(
    val loanId: String,
    val loanName: String,
    val loanStatus: LoanStatus,
    val installmentId: String,
    val dueDate: Long,
    val installmentStatus: LoanInstallmentStatus,
    val payment: Money,
)

data class BudgetReminderCandidate(
    val budgetId: String,
    val budgetName: String,
    val periodStartInclusive: Long,
    val status: BudgetProgressStatus,
    val target: Money,
    val spent: Money,
    val remaining: Money,
)

data class ReminderPreferencesSnapshot(
    val emiRemindersEnabled: Boolean,
    val budgetRemindersEnabled: Boolean,
)

data class ReminderPlanningContext(
    val currentTimestamp: Long,
    val timeZoneId: String,
    val deliveredBudgetIdentityKeys: Set<String> = emptySet(),
)

sealed interface ReminderPlanningError {
    data class InvalidInput(
        val field: String,
        val reason: String,
    ) : ReminderPlanningError
}

sealed interface ReminderPlanningResult {
    data class Success(val plans: List<ReminderPlan>) : ReminderPlanningResult

    data class Failure(val error: ReminderPlanningError) : ReminderPlanningResult
}
