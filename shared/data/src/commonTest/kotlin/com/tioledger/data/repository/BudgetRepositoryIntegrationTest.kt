package com.tioledger.data.repository

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BudgetRepositoryIntegrationTest {
    private lateinit var database: TioLedgerDatabase
    private lateinit var repository: SQLDelightBudgetRepository

    @BeforeTest
    fun setUp() {
        val driver = createTestSqlDriver()
        TioLedgerDatabase.Schema.create(driver)
        database = TioLedgerDatabase(driver)
        database.tioLedgerDatabaseQueries.insertCurrency("INR", "Indian Rupee", "₹", 2L, "en-IN")
        SQLDelightCategoryRepository(database).create(
            Category(
                id = "food",
                name = "Food",
                type = CategoryType.EXPENSE,
                createdAt = 1L,
                updatedAt = 1L,
            ),
        )
        repository = SQLDelightBudgetRepository(database)
    }

    @Test
    fun createsUpdatesAndReturnsBudgetsInDeterministicOrder() {
        repository.create(budget("travel", "Travel", 80_000L, null))
        repository.create(budget("food", "Food", 50_000L, "food"))

        val budgets = assertIs<LedgerResult.Success<List<Budget>>>(repository.findAll()).value
        assertEquals(listOf("food", "travel"), budgets.map { it.id })
        assertEquals("food", budgets.first().categoryId)

        val updated = budgets.first().copy(name = "Dining", amount = Money(60_000L, CurrencyCode("INR")), updatedAt = 2L)
        assertEquals(updated, assertIs<LedgerResult.Success<Budget>>(repository.update(updated)).value)
        assertEquals(updated, assertIs<LedgerResult.Success<Budget>>(repository.findById("food")).value)

        val missing = assertIs<LedgerResult.Failure>(repository.findById("missing"))
        assertEquals(LedgerError.BudgetNotFound("missing"), missing.error)
    }
}

private fun budget(
    id: String,
    name: String,
    amount: Long,
    categoryId: String?,
): Budget =
    Budget(
        id = id,
        name = name,
        amount = Money(amount, CurrencyCode("INR")),
        categoryId = categoryId,
        periodType = BudgetPeriodType.MONTHLY,
        createdAt = 1L,
        updatedAt = 1L,
    )
