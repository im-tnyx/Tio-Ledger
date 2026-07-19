package com.tioledger.data.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CategoryRepositoryIntegrationTest {
    private lateinit var repository: SQLDelightCategoryRepository

    @BeforeTest
    fun setUp() {
        val driver = createTestSqlDriver()
        TioLedgerDatabase.Schema.create(driver)
        repository = SQLDelightCategoryRepository(TioLedgerDatabase(driver))
    }

    @Test
    fun createsAndReturnsActiveCategoriesInDeterministicOrder() {
        repository.create(category("bills", "Bills", CategoryType.EXPENSE))
        repository.create(category("salary", "Salary", CategoryType.INCOME))
        repository.create(category("food", "Food", CategoryType.EXPENSE, isDefault = true))

        val categories = assertIs<LedgerResult.Success<List<Category>>>(repository.findAll()).value

        assertEquals(listOf("food", "bills", "salary"), categories.map { it.id })
        assertEquals(CategoryType.EXPENSE, categories.first().type)
        assertEquals(true, categories.first().isDefault)
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
