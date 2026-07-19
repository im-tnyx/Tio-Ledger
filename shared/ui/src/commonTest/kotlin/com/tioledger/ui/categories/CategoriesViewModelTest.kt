package com.tioledger.ui.categories

import com.tioledger.application.usecase.category.CreateCategoryUseCase
import com.tioledger.application.usecase.category.ListCategoriesUseCase
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.repository.CategoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CategoriesViewModelTest {
    @Test
    fun initialLoadBuildsIncomeAndExpenseGroups() {
        val repository =
            FakeCategoryRepository(
                mutableListOf(
                    category("salary", "Salary", CategoryType.INCOME, isDefault = true),
                    category("food", "Food", CategoryType.EXPENSE),
                ),
            )

        val viewModel = viewModel(repository)
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals(2, state.groups.size)
        assertTrue(state.groups.any { it.type == CategoryType.INCOME })
        assertTrue(state.groups.any { it.type == CategoryType.EXPENSE })
    }

    @Test
    fun createCategoryPersistsAndRefreshesList() {
        val repository = FakeCategoryRepository(mutableListOf())
        val viewModel = viewModel(repository)

        viewModel.onAction(CategoriesAction.AddClicked)
        viewModel.onAction(CategoriesAction.NameChanged(" Travel "))
        viewModel.onAction(CategoriesAction.TypeChanged(CategoryType.EXPENSE))
        viewModel.onAction(CategoriesAction.SaveClicked)

        val state = viewModel.uiState.value
        assertFalse(state.isCreateDialogVisible)
        assertFalse(state.isSaving)
        assertEquals("Travel added", state.successMessage)
        assertEquals("Travel", repository.categories.single().name)
        assertEquals("Travel", state.groups.single().categories.single().name)
    }

    @Test
    fun duplicateCategoryKeepsDialogOpenWithValidationMessage() {
        val repository =
            FakeCategoryRepository(
                mutableListOf(category("food", "Food", CategoryType.EXPENSE)),
            )
        val viewModel = viewModel(repository)

        viewModel.onAction(CategoriesAction.AddClicked)
        viewModel.onAction(CategoriesAction.NameChanged("food"))
        viewModel.onAction(CategoriesAction.SaveClicked)

        val state = viewModel.uiState.value
        assertTrue(state.isCreateDialogVisible)
        assertFalse(state.isSaving)
        assertNotNull(state.validationErrorMessage)
        assertEquals(1, repository.categories.size)
    }
}

private fun viewModel(repository: FakeCategoryRepository): CategoriesViewModel =
    CategoriesViewModel(
        listCategoriesUseCase = ListCategoriesUseCase(repository),
        createCategoryUseCase = CreateCategoryUseCase(repository),
        idGenerator = FixedIdGenerator("new-category"),
        nowProvider = { 10L },
    )

private class FixedIdGenerator(private val id: String) : IdGenerator {
    override fun nextId(): String = id
}

private class FakeCategoryRepository(
    val categories: MutableList<Category>,
) : CategoryRepository {
    override fun findAll(): LedgerResult<List<Category>> = LedgerResult.Success(categories.toList())

    override fun findById(categoryId: String): LedgerResult<Category> =
        categories.firstOrNull { it.id == categoryId }?.let(LedgerResult::Success)
            ?: LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))

    override fun create(category: Category): LedgerResult<Category> {
        categories += category
        return LedgerResult.Success(category)
    }

    override fun update(category: Category): LedgerResult<Category> {
        val index = categories.indexOfFirst { it.id == category.id }
        if (index < 0) return LedgerResult.Failure(LedgerError.CategoryNotFound(category.id))
        categories[index] = category
        return LedgerResult.Success(category)
    }
}

private fun category(
    id: String,
    name: String,
    type: CategoryType,
    isDefault: Boolean = false,
): Category =
    Category(
        id = id,
        name = name,
        type = type,
        isDefault = isDefault,
        createdAt = 1L,
        updatedAt = 1L,
    )
