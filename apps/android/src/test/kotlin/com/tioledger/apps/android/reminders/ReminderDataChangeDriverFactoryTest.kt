package com.tioledger.apps.android.reminders

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.transaction.RecordExpenseCommand
import com.tioledger.application.usecase.transaction.RecordExpenseUseCase
import com.tioledger.bootstrap.database.DatabaseDriverFactory
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.data.repository.SQLDelightAccountRepository
import com.tioledger.data.repository.SQLDelightBudgetRepository
import com.tioledger.data.repository.SQLDelightCategoryRepository
import com.tioledger.data.repository.SQLDelightLoanRepository
import com.tioledger.data.repository.SQLDelightTransactionRepository
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Budget
import com.tioledger.domain.model.BudgetPeriodType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanCompoundingFrequency
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanEmiCalculationMethod
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanInterestType
import com.tioledger.domain.model.LoanPaymentFrequency
import com.tioledger.domain.model.LoanStatus
import com.tioledger.domain.model.SYSTEM_ADJUSTMENT_ID
import com.tioledger.domain.model.SYSTEM_EXPENSE_ID
import com.tioledger.domain.model.SYSTEM_INCOME_ID
import com.tioledger.domain.model.SYSTEM_OPENING_BALANCE_ID
import com.tioledger.finance.engine.PostingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ReminderDataChangeDriverFactoryTest {
    private val inr = CurrencyCode("INR")
    private var changes = 0
    private lateinit var driver: SqlDriver
    private lateinit var database: TioLedgerDatabase

    @Before
    fun setUp() {
        driver = ReminderDataChangeDriverFactory(InMemoryDriverFactory) { changes += 1 }.createDriver()
        database = TioLedgerDatabase(driver)
        database.tioLedgerDatabaseQueries.insertCurrency("INR", "Indian Rupee", "INR", 2L, "en-IN")
        listOf(SYSTEM_INCOME_ID, SYSTEM_EXPENSE_ID, SYSTEM_OPENING_BALANCE_ID, SYSTEM_ADJUSTMENT_ID)
            .forEach(::insertSystemAccount)
        val accounts = SQLDelightAccountRepository(database)
        accounts.create(account("wallet", AccountType.CASH))
        accounts.create(account("loan-account", AccountType.LOAN_LINKED))
        SQLDelightCategoryRepository(database).create(
            Category(id = "food", name = "Food", type = CategoryType.EXPENSE, createdAt = 1L, updatedAt = 1L),
        )
    }

    @Test
    fun nonReminderWritesDoNotReportDataChanges() {
        assertEquals(0, changes)
    }

    @Test
    fun onlyReminderRelevantTableKeysAreObserved() {
        driver.notifyListeners("accounts", "categories", "currencies")
        assertEquals(0, changes)

        REMINDER_RELEVANT_TABLE_KEYS.forEach { key ->
            val before = changes
            driver.notifyListeners(key)
            assertEquals("table key $key", before + 1, changes)
        }
    }

    @Test
    fun oneCommittedBatchAcrossRelevantTablesReportsOnce() {
        driver.notifyListeners("transactions", "transaction_splits", "ledger_entries")
        assertEquals(1, changes)
    }

    @Test
    fun budgetCreateAndUpdateReportDataChanges() {
        val budgets = SQLDelightBudgetRepository(database)
        val budget =
            Budget(
                id = "groceries",
                name = "Groceries",
                amount = Money(10_000L, inr),
                categoryId = "food",
                periodType = BudgetPeriodType.MONTHLY,
                createdAt = 1L,
                updatedAt = 1L,
            )

        assertTrue(budgets.create(budget).isSuccess())
        val afterCreate = changes
        assertTrue(afterCreate > 0)

        assertTrue(budgets.update(budget.copy(amount = Money(8_000L, inr), updatedAt = 2L)).isSuccess())
        assertTrue(changes > afterCreate)
    }

    @Test
    fun recordedExpenseReportsDataChange() {
        val recordExpense =
            RecordExpenseUseCase(
                SQLDelightAccountRepository(database),
                SQLDelightCategoryRepository(database),
                SQLDelightTransactionRepository(database),
                PostingEngine(SequentialIdGenerator()),
            )

        val result =
            recordExpense(
                RecordExpenseCommand(
                    timestamp = 2_000L,
                    description = "Lunch",
                    amount = Money(8_500L, inr),
                    accountId = "wallet",
                    categoryId = "food",
                    merchantId = null,
                    createdAt = 2_000L,
                ),
            )

        assertTrue(result is ApplicationResult.Success)
        assertTrue(changes > 0)
    }

    @Test
    fun loanCreationReportsDataChange() {
        assertTrue(SQLDelightLoanRepository(database).create(loanDetails()).isSuccess())
        assertTrue(changes > 0)
    }

    private fun insertSystemAccount(id: String) {
        database.accountsQueries.insertAccount(
            id = id,
            name = id,
            type = AccountType.WALLET.name,
            currency_code = "INR",
            is_archived = 0L,
            display_order = 0L,
            created_at = 0L,
            updated_at = 0L,
            entity_version = 1L,
            sync_version = 0L,
            device_id = null,
            updated_by = null,
            deleted_at = null,
        )
    }

    private fun account(
        id: String,
        type: AccountType,
    ): Account = Account(id = id, name = id, type = type, currencyCode = "INR", createdAt = 1L, updatedAt = 1L)

    private fun loanDetails(): LoanDetails {
        val loan =
            Loan(
                id = "home",
                name = "Home Loan",
                principal = Money(100_000L, inr),
                annualInterestRateBasisPoints = 1_000,
                interestType = LoanInterestType.REDUCING,
                emiCalculationMethod = LoanEmiCalculationMethod.REDUCING_BALANCE,
                compoundingFrequency = LoanCompoundingFrequency.MONTHLY,
                paymentFrequency = LoanPaymentFrequency.MONTHLY,
                tenureMonths = 1,
                startDate = 1_000L,
                accountId = "loan-account",
                disbursedAccountId = "wallet",
                processingFee = Money.zero(inr),
                insuranceAmount = Money.zero(inr),
                status = LoanStatus.ACTIVE,
                createdAt = 1L,
                updatedAt = 1L,
            )
        val installment =
            LoanInstallment(
                id = "home-1",
                loanId = loan.id,
                installmentNumber = 1,
                dueDate = 2_000L,
                openingBalance = Money(100_000L, inr),
                payment = Money(101_000L, inr),
                principalComponent = Money(100_000L, inr),
                interestComponent = Money(1_000L, inr),
                closingBalance = Money.zero(inr),
                status = LoanInstallmentStatus.PENDING,
                createdAt = 1L,
                updatedAt = 1L,
            )
        return LoanDetails(loan = loan, schedule = listOf(installment))
    }
}

private object InMemoryDriverFactory : DatabaseDriverFactory {
    override fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "PRAGMA foreign_keys = ON;", 0)
        TioLedgerDatabase.Schema.create(driver)
        return driver
    }
}

private class SequentialIdGenerator : IdGenerator {
    private var next = 0

    override fun nextId(): String {
        next += 1
        return "id-$next"
    }
}
