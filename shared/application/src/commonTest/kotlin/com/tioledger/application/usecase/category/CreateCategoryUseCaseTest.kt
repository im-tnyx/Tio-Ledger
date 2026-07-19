package com.tioledger.application.usecase.category

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.repository.CategoryRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CreateCategoryUseCaseTest {
    @Test
    fun rejectsCaseInsensitiveDuplicateWithinSameType() {
        val repository =
            FakeCategoryRepository(
                categories = mutableListOf(category("food", "Food", CategoryType.EXPENSE)),
            )

        val result =
            CreateCategoryUseCase(repository)(
                CreateCategoryCommand(
                    id = "new-food",
                    name = " food ",
                    type = CategoryType.EXPENSE,
                    createdAt = 2L,
                ),
            )

        val failure = assertIs<ApplicationResult.Failure>(result)
        assertEquals(
            ApplicationError.Validation("name", "category already exists for this type"),
            failure.error,
        )
        assertEquals(0, repository.createCalls)
    }

    @Test
    fun allowsSameNameForDifferentTypeAndTrimsName() {
        val repository =
            FakeCategoryRepository(
                categories = mutableListOf(category("expense-food", "Food", CategoryType.EXPENSE)),
            )

        val result =
            CreateCategoryUseCase(repository)(
                CreateCategoryCommand(
                    id = "income-food",
                    name = " Food ",
                    type = CategoryType.INCOME,
                    createdAt = 2L,
                ),
            )

        val created = assertIs<ApplicationResult.Success<Category>>(result).outcome.value
        assertEquals("Food", created.name)
        assertEquals(CategoryType.INCOME, created.type)
        assertEquals(1, repository.createCalls)
    }
}

private class FakeCategoryRepository(
    private val categories: MutableList<Category>,
) : CategoryRepository {
    var createCalls: Int = 0
        private set

    override fun findAll(): LedgerResult<List<Category>> = LedgerResult.Success(categories.toList())

    override fun findById(categoryId: String): LedgerResult<Category> =
        categories.firstOrNull { it.id == categoryId }
            ?.let { category -> LedgerResult.Success(category) }
            ?: LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))

    override fun create(category: Category): LedgerResult<Category> {
        createCalls += 1
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
): Category =
    Category(
        id = id,
        name = name,
        type = type,
        createdAt = 1L,
        updatedAt = 1L,
    )
