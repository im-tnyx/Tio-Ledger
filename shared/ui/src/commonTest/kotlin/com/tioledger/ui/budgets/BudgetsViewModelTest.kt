package com.tioledger.ui.budgets

import com.tioledger.application.usecase.budget.CreateBudgetUseCase
import com.tioledger.application.usecase.budget.ListBudgetSummariesUseCase
import com.tioledger.application.usecase.budget.ListBudgetsUseCase
import com.tioledger.application.usecase.budget.UpdateBudgetUseCase
import com.tioledger.application.usecase.category.ListCategoriesUseCase
import com.tioledger.budget.engine.BudgetPeriodCalculator
import com.tioledger.budget.engine.BudgetProgressCalculator
import com.tioledger.budget.engine.BudgetProgressStatus
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.repository.BudgetRepository
import com.tioledger.domain.repository.CategoryRepository
import com.tioledger.domain.repository.TransactionHistoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BudgetsViewModelTest {
    @Test
    fun loadsBudgetSummariesAndExpenseCategories() {
        val budgetRepository = FakeBudgetRepository(mutableListOf(foodBudget()))
        val categoryRepository = FakeCategoryRepository(listOf(foodCategory()))

        val viewModel = createViewModel(budgetRepository, categoryRepository)

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.loadErrorMessage)
        assertEquals(1, state.budgets.size)
        assertEquals("Food", state.budgets.single().categoryLabel)
        assertEquals("USD 100.00", state.budgets.single().targetLabel)
        assertEquals(BudgetProgressStatus.ON_TRACK, state.budgets.single().status)
        assertEquals(listOf("All expenses", "Food"), state.categoryOptions.map { it.name })
    }

    @Test
    fun distinguishesUnavailableCategoryScopeFromAllExpenses() {
        val budgetRepository = FakeBudgetRepository(mutableListOf(foodBudget()))
        val categoryRepository = FakeCategoryRepository(emptyList())

        val viewModel = createViewModel(budgetRepository, categoryRepository)

        assertEquals("Unavailable category", viewModel.uiState.value.budgets.single().categoryLabel)
        assertEquals(listOf("All expenses"), viewModel.uiState.value.categoryOptions.map { it.name })

        viewModel.onAction(BudgetsAction.EditClicked("budget-food"))

        assertEquals("food", viewModel.uiState.value.editor?.categoryId)
    }

    @Test
    fun createsBudgetAndRefreshesSummary() {
        val budgetRepository = FakeBudgetRepository()
        val categoryRepository = FakeCategoryRepository(listOf(foodCategory()))
        val viewModel = createViewModel(budgetRepository, categoryRepository)

        viewModel.onAction(BudgetsAction.AddClicked)
        viewModel.onAction(BudgetsAction.NameChanged("Food budget"))
        viewModel.onAction(BudgetsAction.AmountChanged("100"))
        viewModel.onAction(BudgetsAction.CurrencyChanged("usd"))
        viewModel.onAction(BudgetsAction.CategorySelected("food"))
        viewModel.onAction(BudgetsAction.PeriodChanged(BudgetPeriodType.MONTHLY))
        viewModel.onAction(BudgetsAction.SaveClicked)

        val created = budgetRepository.budgets.single()
        assertEquals("budget-new", created.id)
        assertEquals(Money(10_000L, CurrencyCode("USD")), created.amount)
        assertEquals("food", created.categoryId)
        assertEquals(1, viewModel.uiState.value.budgets.size)
        assertNull(viewModel.uiState.value.editor)
        assertEquals("Food budget added", viewModel.uiState.value.successMessage)
    }

    @Test
    fun updatesExistingBudgetAndRefreshesSummary() {
        val budgetRepository = FakeBudgetRepository(mutableListOf(foodBudget()))
        val categoryRepository = FakeCategoryRepository(listOf(foodCategory()))
        val viewModel = createViewModel(budgetRepository, categoryRepository)

        viewModel.onAction(BudgetsAction.EditClicked("budget-food"))
        viewModel.onAction(BudgetsAction.AmountChanged("125.50"))
        viewModel.onAction(BudgetsAction.SaveClicked)

        assertEquals(12_550L, budgetRepository.budgets.single().amount.amount)
        assertEquals("USD 125.50", viewModel.uiState.value.budgets.single().targetLabel)
        assertNull(viewModel.uiState.value.editor)
        assertEquals("Food updated", viewModel.uiState.value.successMessage)
    }

    @Test
    fun keepsInvalidAmountInsideEditor() {
        val budgetRepository = FakeBudgetRepository()
        val categoryRepository = FakeCategoryRepository(listOf(foodCategory()))
        val viewModel = createViewModel(budgetRepository, categoryRepository)

        viewModel.onAction(BudgetsAction.AddClicked)
        viewModel.onAction(BudgetsAction.NameChanged("Food"))
        viewModel.onAction(BudgetsAction.AmountChanged("12.345"))
        viewModel.onAction(BudgetsAction.CurrencyChanged("USD"))
        viewModel.onAction(BudgetsAction.SaveClicked)

        assertTrue(budgetRepository.budgets.isEmpty())
        assertEquals("Use at most 2 decimal places.", viewModel.uiState.value.validationErrorMessage)
        assertTrue(viewModel.uiState.value.editor != null)
    }

    private fun createViewModel(
        budgetRepository: FakeBudgetRepository,
        categoryRepository: FakeCategoryRepository,
    ): BudgetsViewModel {
        val transactionRepository = FakeTransactionHistoryRepository()
        return BudgetsViewModel(
            listBudgetSummariesUseCase =
                ListBudgetSummariesUseCase(
                    listBudgetsUseCase = ListBudgetsUseCase(budgetRepository),
                    categoryRepository = categoryRepository,
                    transactionHistoryRepository = transactionRepository,
                    periodCalculator = BudgetPeriodCalculator(),
                    progressCalculator = BudgetProgressCalculator(),
                ),
            listCategoriesUseCase = ListCategoriesUseCase(categoryRepository),
            createBudgetUseCase = CreateBudgetUseCase(budgetRepository, categoryRepository),
            updateBudgetUseCase = UpdateBudgetUseCase(budgetRepository, categoryRepository),
            idGenerator = FixedIdGenerator("budget-new"),
            nowProvider = { JULY_19_2026_UTC },
            timeZoneIdProvider = { "UTC" },
        )
    }

    private fun foodBudget(): Budget =
        Budget(
            id = "budget-food",
            name = "Food",
            amount = Money(10_000L, CurrencyCode("USD")),
            categoryId = "food",
            periodType = BudgetPeriodType.MONTHLY,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun foodCategory(): Category =
        Category(
            id = "food",
            name = "Food",
            type = CategoryType.EXPENSE,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private class FixedIdGenerator(
        private val id: String,
    ) : IdGenerator {
        override fun nextId(): String = id
    }

    private class FakeBudgetRepository(
        val budgets: MutableList<Budget> = mutableListOf(),
    ) : BudgetRepository {
        override fun findAll(): LedgerResult<List<Budget>> = LedgerResult.Success(budgets.toList())

        override fun findById(budgetId: String): LedgerResult<Budget> =
            budgets.firstOrNull { it.id == budgetId }
                ?.let { LedgerResult.Success(it) }
                ?: LedgerResult.Failure(LedgerError.BudgetNotFound(budgetId))

        override fun create(budget: Budget): LedgerResult<Budget> {
            budgets += budget
            return LedgerResult.Success(budget)
        }

        override fun update(budget: Budget): LedgerResult<Budget> {
            val index = budgets.indexOfFirst { it.id == budget.id }
            if (index < 0) return LedgerResult.Failure(LedgerError.BudgetNotFound(budget.id))
            budgets[index] = budget
            return LedgerResult.Success(budget)
        }
    }

    private class FakeCategoryRepository(
        private val categories: List<Category>,
    ) : CategoryRepository {
        override fun findAll(): LedgerResult<List<Category>> = LedgerResult.Success(categories)

        override fun findById(categoryId: String): LedgerResult<Category> =
            categories.firstOrNull { it.id == categoryId }
                ?.let { LedgerResult.Success(it) }
                ?: LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))

        override fun create(category: Category): LedgerResult<Category> = LedgerResult.Success(category)

        override fun update(category: Category): LedgerResult<Category> = LedgerResult.Success(category)
    }

    private class FakeTransactionHistoryRepository : TransactionHistoryRepository {
        override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> = LedgerResult.Success(emptyList())
    }

    private companion object {
        const val JULY_19_2026_UTC = 1_784_419_200_000L
    }
}
