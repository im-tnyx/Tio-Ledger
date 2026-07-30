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
    fun loadsMonthlyReportByDefault() {
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
        assertEquals("INR", state.report?.currencySections?.single()?.currencyCode)
        assertEquals("Food", state.report?.currencySections?.single()?.categoryBreakdown?.single()?.label)
    }

    @Test
    fun periodSelectionReloadsReport() {
        val repository =
            FakeReportsRepository(
                records = listOf(reportRecord("expense", TransactionType.EXPENSE, 1_000L, categoryId = "food", categoryName = "Food")),
            )
        val viewModel = reportsViewModel(repository)

        viewModel.onAction(ReportsAction.PeriodSelected(SpendingReportPeriod.WEEKLY))

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(SpendingReportPeriod.WEEKLY, viewModel.uiState.value.selectedPeriod)
        assertEquals("Weekly", viewModel.uiState.value.report?.periodLabel)
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
