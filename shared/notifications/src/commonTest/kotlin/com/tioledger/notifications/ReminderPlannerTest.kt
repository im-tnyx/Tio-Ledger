package com.tioledger.notifications

import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanStatus
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

class ReminderPlannerTest {
    private val planner = ReminderPlanner()
    private val inr = CurrencyCode("INR")

    @Test
    fun plansEmiLeadAndDueDayAtLocalNineAcrossDstBoundary() {
        val timeZoneId = "America/New_York"
        val dueDate = LocalDate(2026, 3, 9)
        val result =
            planner.plan(
                emiCandidates = listOf(emiCandidate(dueDate = dueDate)),
                budgetCandidates = emptyList(),
                preferences = ReminderPreferencesSnapshot(true, false),
                context =
                    ReminderPlanningContext(
                        currentTimestamp = localTimestamp(LocalDate(2026, 3, 1), 0, timeZoneId),
                        timeZoneId = timeZoneId,
                    ),
            )

        val plans = assertIs<ReminderPlanningResult.Success>(result).plans
        assertEquals(2, plans.size)
        assertEquals(localTimestamp(LocalDate(2026, 3, 6), 9, timeZoneId), plans[0].deliveryTimestamp)
        assertEquals(localTimestamp(dueDate, 9, timeZoneId), plans[1].deliveryTimestamp)
        assertEquals(ReminderIdentity.Emi("loan-1", "installment-1", 3), plans[0].identity)
        assertEquals(ReminderIdentity.Emi("loan-1", "installment-1", 0), plans[1].identity)
        assertEquals(ReminderDestination.LoanDetails("loan-1"), plans[0].destination)
        assertEquals(utcTimestamp(dueDate), assertIs<ReminderContent.Emi>(plans[0].content).dueDate)
    }

    @Test
    fun filtersIneligibleEmiCandidatesAndOmitsPastDeliveryTimes() {
        val timeZoneId = "UTC"
        val dueDate = LocalDate(2026, 8, 6)
        val candidates =
            listOf(
                emiCandidate(dueDate = dueDate),
                emiCandidate(
                    installmentId = "paid",
                    dueDate = LocalDate(2026, 8, 10),
                    installmentStatus = LoanInstallmentStatus.PAID,
                ),
                emiCandidate(
                    loanId = "closed",
                    installmentId = "closed-installment",
                    dueDate = LocalDate(2026, 8, 10),
                    loanStatus = LoanStatus.CLOSED,
                ),
            )
        val result =
            planner.plan(
                emiCandidates = candidates,
                budgetCandidates = emptyList(),
                preferences = ReminderPreferencesSnapshot(true, false),
                context =
                    ReminderPlanningContext(
                        currentTimestamp = localTimestamp(dueDate, 10, timeZoneId),
                        timeZoneId = timeZoneId,
                    ),
            )

        assertEquals(emptyList(), assertIs<ReminderPlanningResult.Success>(result).plans)
    }

    @Test
    fun keepsEmiIdentityStableWhenDueDateChanges() {
        val context =
            ReminderPlanningContext(
                currentTimestamp = utcTimestamp(LocalDate(2026, 8, 1)),
                timeZoneId = "UTC",
            )
        val original =
            successPlans(
                planner.plan(
                    emiCandidates = listOf(emiCandidate(dueDate = LocalDate(2026, 8, 10))),
                    budgetCandidates = emptyList(),
                    preferences = ReminderPreferencesSnapshot(true, false),
                    context = context,
                ),
            )
        val revised =
            successPlans(
                planner.plan(
                    emiCandidates = listOf(emiCandidate(dueDate = LocalDate(2026, 8, 12))),
                    budgetCandidates = emptyList(),
                    preferences = ReminderPreferencesSnapshot(true, false),
                    context = context,
                ),
            )

        assertEquals(original.map { it.identity }, revised.map { it.identity })
        assertNotEquals(original.map { it.deliveryTimestamp }, revised.map { it.deliveryTimestamp })
    }

    @Test
    fun plansBudgetTransitionsOnceAndSuppressesDeliveredIdentity() {
        val periodStart = LocalDate(2026, 8, 1)
        val warning = budgetCandidate(BudgetProgressStatus.WARNING, periodStart)
        val reached = budgetCandidate(BudgetProgressStatus.REACHED, periodStart)
        val exceeded = budgetCandidate(BudgetProgressStatus.EXCEEDED, periodStart)
        val onTrack = budgetCandidate(BudgetProgressStatus.ON_TRACK, periodStart)
        val delivered = ReminderIdentity.Budget("budget-1", BudgetPeriodType.MONTHLY, periodStart, BudgetProgressStatus.REACHED).key
        val context =
            ReminderPlanningContext(
                currentTimestamp = utcTimestamp(LocalDate(2026, 8, 6)),
                timeZoneId = "UTC",
                deliveredBudgetIdentityKeys = setOf(delivered),
            )

        val first =
            successPlans(
                planner.plan(
                    emiCandidates = emptyList(),
                    budgetCandidates = listOf(exceeded, onTrack, reached, warning),
                    preferences = ReminderPreferencesSnapshot(false, true),
                    context = context,
                ),
            )
        val second =
            successPlans(
                planner.plan(
                    emiCandidates = emptyList(),
                    budgetCandidates = listOf(exceeded, onTrack, reached, warning),
                    preferences = ReminderPreferencesSnapshot(false, true),
                    context = context,
                ),
            )

        assertEquals(first, second)
        assertEquals(
            listOf(
                ReminderIdentity.Budget("budget-1", BudgetPeriodType.MONTHLY, periodStart, BudgetProgressStatus.EXCEEDED),
                ReminderIdentity.Budget("budget-1", BudgetPeriodType.MONTHLY, periodStart, BudgetProgressStatus.WARNING),
            ),
            first.map { it.identity },
        )
        assertEquals(context.currentTimestamp, first.single { it.identity == first[0].identity }.deliveryTimestamp)
        assertEquals(ReminderDestination.Budgets, first[0].destination)
    }

    @Test
    fun budgetIdentityKeyRendersCanonicalTypeAndIsoCalendarDate() {
        val identity =
            ReminderIdentity.Budget(
                budgetId = "budget-1",
                periodType = BudgetPeriodType.MONTHLY,
                periodStartDate = LocalDate(2026, 9, 1),
                status = BudgetProgressStatus.EXCEEDED,
            )

        assertEquals("budget|budget-1|MONTHLY|2026-09-01|EXCEEDED", identity.key)
        assertEquals(
            "budget|budget-1|WEEKLY|2026-01-05|WARNING",
            ReminderIdentity.Budget("budget-1", BudgetPeriodType.WEEKLY, LocalDate(2026, 1, 5), BudgetProgressStatus.WARNING).key,
        )
        assertEquals(
            "budget|budget-1|YEARLY|2026-01-01|REACHED",
            ReminderIdentity.Budget("budget-1", BudgetPeriodType.YEARLY, LocalDate(2026, 1, 1), BudgetProgressStatus.REACHED).key,
        )
    }

    @Test
    fun budgetIdentityDoesNotDependOnPlanningTimeZone() {
        val candidate = budgetCandidate(BudgetProgressStatus.EXCEEDED, LocalDate(2026, 9, 1))
        val currentTimestamp = utcTimestamp(LocalDate(2026, 9, 10))

        val kolkata = successPlans(planBudgets(listOf(candidate), ReminderPlanningContext(currentTimestamp, "Asia/Kolkata")))
        val newYork = successPlans(planBudgets(listOf(candidate), ReminderPlanningContext(currentTimestamp, "America/New_York")))

        assertEquals(kolkata.map { it.identity }, newYork.map { it.identity })
        assertEquals("budget|budget-1|MONTHLY|2026-09-01|EXCEEDED", newYork.single().identity.key)
    }

    @Test
    fun deliveredCanonicalKeySuppressesSameStateAfterTimeZoneChange() {
        val candidate = budgetCandidate(BudgetProgressStatus.EXCEEDED, LocalDate(2026, 9, 1))
        val currentTimestamp = utcTimestamp(LocalDate(2026, 9, 10))
        val delivered = successPlans(planBudgets(listOf(candidate), ReminderPlanningContext(currentTimestamp, "Asia/Kolkata")))

        val afterTimeZoneChange =
            planBudgets(
                listOf(candidate),
                ReminderPlanningContext(
                    currentTimestamp = currentTimestamp,
                    timeZoneId = "America/New_York",
                    deliveredBudgetIdentityKeys = setOf(delivered.single().identity.key),
                ),
            )

        assertEquals(emptyList(), successPlans(afterTimeZoneChange))
    }

    @Test
    fun periodTypeAndPeriodStartDateDistinguishBudgetIdentities() {
        val january = LocalDate(2026, 1, 1)
        val monthly = budgetCandidate(BudgetProgressStatus.WARNING, january, BudgetPeriodType.MONTHLY)
        val yearly = budgetCandidate(BudgetProgressStatus.WARNING, january, BudgetPeriodType.YEARLY)
        val nextMonth = budgetCandidate(BudgetProgressStatus.WARNING, LocalDate(2026, 2, 1), BudgetPeriodType.MONTHLY)
        val context = ReminderPlanningContext(utcTimestamp(LocalDate(2026, 1, 10)), "UTC")

        val keys = successPlans(planBudgets(listOf(monthly, yearly, nextMonth), context)).map { it.identity.key }
        val suppressedMonthly =
            successPlans(
                planBudgets(
                    listOf(monthly, yearly, nextMonth),
                    context.copy(deliveredBudgetIdentityKeys = setOf(keys.first())),
                ),
            ).map { it.identity.key }

        assertEquals(3, keys.toSet().size)
        assertEquals(keys.drop(1), suppressedMonthly)
    }

    @Test
    fun disabledPreferencesReturnNoPlansWithoutValidatingDisabledCandidates() {
        val result =
            planner.plan(
                emiCandidates = listOf(emiCandidate(loanId = "")),
                budgetCandidates = listOf(budgetCandidate(BudgetProgressStatus.WARNING, LocalDate(2026, 8, 1), budgetId = "")),
                preferences = ReminderPreferencesSnapshot(false, false),
                context = ReminderPlanningContext(0L, "UTC"),
            )

        assertEquals(emptyList(), assertIs<ReminderPlanningResult.Success>(result).plans)
    }

    @Test
    fun rejectsInvalidTimeZone() {
        val result =
            planner.plan(
                emiCandidates = emptyList(),
                budgetCandidates = emptyList(),
                preferences = ReminderPreferencesSnapshot(true, true),
                context = ReminderPlanningContext(0L, "Not/AZone"),
            )

        val failure = assertIs<ReminderPlanningResult.Failure>(result)
        val error = assertIs<ReminderPlanningError.InvalidInput>(failure.error)
        assertEquals("timeZoneId", error.field)
    }

    private fun emiCandidate(
        loanId: String = "loan-1",
        installmentId: String = "installment-1",
        dueDate: LocalDate = LocalDate(2026, 8, 10),
        loanStatus: LoanStatus = LoanStatus.ACTIVE,
        installmentStatus: LoanInstallmentStatus = LoanInstallmentStatus.PENDING,
    ): EmiReminderCandidate =
        EmiReminderCandidate(
            loanId = loanId,
            loanName = "Home loan",
            loanStatus = loanStatus,
            installmentId = installmentId,
            dueDate = utcTimestamp(dueDate),
            installmentStatus = installmentStatus,
            payment = Money(25_000L, inr),
        )

    private fun budgetCandidate(
        status: BudgetProgressStatus,
        periodStartDate: LocalDate,
        periodType: BudgetPeriodType = BudgetPeriodType.MONTHLY,
        budgetId: String = "budget-1",
    ): BudgetReminderCandidate =
        BudgetReminderCandidate(
            budgetId = budgetId,
            budgetName = "Food",
            periodType = periodType,
            periodStartDate = periodStartDate,
            status = status,
            target = Money(10_000L, inr),
            spent = Money(8_000L, inr),
            remaining = Money(2_000L, inr),
        )

    private fun planBudgets(
        candidates: List<BudgetReminderCandidate>,
        context: ReminderPlanningContext,
    ): ReminderPlanningResult =
        planner.plan(
            emiCandidates = emptyList(),
            budgetCandidates = candidates,
            preferences = ReminderPreferencesSnapshot(false, true),
            context = context,
        )

    private fun successPlans(result: ReminderPlanningResult): List<ReminderPlan> = assertIs<ReminderPlanningResult.Success>(result).plans

    private fun utcTimestamp(date: LocalDate): Long = localTimestamp(date, 0, "UTC")

    private fun localTimestamp(
        date: LocalDate,
        hour: Int,
        timeZoneId: String,
    ): Long =
        LocalDateTime(date, LocalTime(hour, 0))
            .toInstant(TimeZone.of(timeZoneId))
            .toEpochMilliseconds()
}
