package com.tioledger.ui.reports

import com.tioledger.application.usecase.analytics.SpendingReportPeriod
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportsViewModelTest {
    @Test
    fun loadsMonthlyReportWithCashFlowByDefault() {
        val repository =
            FakeReportsRepository(
                records =
                    listOf(
                        reportRecord("income", TransactionType.INCOME, 125_000L, accountName = "Salary Bank"),
                        reportRecord("expense", TransactionType.EXPENSE, 2_500L, categoryId = "food", categoryName = "Food"),
                    ),
            )

        val viewModel = reportsViewModel(repository)
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(SpendingReportPeriod.MONTHLY, state.selectedPeriod)
        assertEquals("Monthly", state.report?.periodLabel)
        val section = state.report?.currencySections?.single()
        assertEquals("INR", section?.currencyCode)
        assertEquals("Food", section?.categoryBreakdown?.single()?.label)
        assertEquals(31, section?.cashFlowRows?.size)
        val july19 = section?.cashFlowRows?.get(18)
        assertEquals("2026-07-19", july19?.label)
        assertEquals("INR 1250.00", july19?.incomeLabel)
        assertEquals("INR 25.00", july19?.expenseLabel)
        assertEquals("+INR 1225.00", july19?.netLabel)
    }

    @Test
    fun periodSelectionReloadsReportAndUsesYearMonthLabels() {
        val repository =
            FakeReportsRepository(
                records = listOf(reportRecord("expense", TransactionType.EXPENSE, 1_000L, categoryId = "food", categoryName = "Food")),
            )
        val viewModel = reportsViewModel(repository)

        viewModel.onAction(ReportsAction.PeriodSelected(SpendingReportPeriod.YEARLY))

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(SpendingReportPeriod.YEARLY, state.selectedPeriod)
        assertEquals("Yearly", state.report?.periodLabel)
        val cashFlowRows = state.report?.currencySections?.single()?.cashFlowRows.orEmpty()
        assertEquals(12, cashFlowRows.size)
        assertEquals("2026-07", cashFlowRows[6].label)
        assertEquals("-INR 10.00", cashFlowRows[6].netLabel)
    }

    @Test
    fun repositoryFailureShowsErrorState() {
        val repository = FakeReportsRepository()
        repository.result = LedgerResult.Failure(LedgerError.Unknown("db unavailable"))

        val viewModel = reportsViewModel(repository)

        assertTrue(viewModel.uiState.value.loadErrorMessage != null)
        assertEquals("Unable to load reports.", viewModel.uiState.value.loadErrorMessage)
        assertTrue(viewModel.uiState.value.report == null)
    }
}
