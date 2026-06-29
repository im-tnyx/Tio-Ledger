package com.tioledger.finance.engine

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.LedgerClass
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.NormalBalance
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.TransactionType
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

class TestIdGenerator : IdGenerator {
    private var counter = 0

    override fun nextId(): String {
        counter++
        return "id-$counter"
    }
}

class PostingEngineTest {
    private val idGenerator = TestIdGenerator()
    private val validator = PostingValidator()
    private val engine = PostingEngine(idGenerator, validator)
    private val calculator = BalanceCalculator()

    private val usd = CurrencyCode("USD")
    private val inr = CurrencyCode("INR")

    private val bankAccount =
        Account(
            id = "acc-bank",
            name = "Checking Bank Account",
            type = AccountType.BANK,
            currencyCode = "USD",
            createdAt = 1000L,
            updatedAt = 1000L,
        )

    private val creditCardAccount =
        Account(
            id = "acc-cc",
            name = "Visa Credit Card",
            type = AccountType.CREDIT_CARD,
            currencyCode = "USD",
            createdAt = 1000L,
            updatedAt = 1000L,
        )

    private val incomeCategory =
        Category(
            id = "cat-salary",
            name = "Salary Category",
            type = CategoryType.INCOME,
            createdAt = 1000L,
            updatedAt = 1000L,
        )

    private val expenseCategory =
        Category(
            id = "cat-groceries",
            name = "Groceries",
            type = CategoryType.EXPENSE,
            createdAt = 1000L,
            updatedAt = 1000L,
        )

    @Test
    fun testCurrencyCodeValidation() {
        assertEquals("USD", CurrencyCode("usd").toString())
        assertEquals("INR", CurrencyCode("INR").toString())

        assertFailsWith<IllegalArgumentException> {
            CurrencyCode("US")
        }
        assertFailsWith<IllegalArgumentException> {
            CurrencyCode("US1")
        }
        assertFailsWith<IllegalArgumentException> {
            CurrencyCode("U\$D")
        }
    }

    @Test
    fun testMoneyArithmetic() {
        val m1 = Money(100L, usd)
        val m2 = Money(50L, usd)

        assertEquals(Money(150L, usd), m1 + m2)
        assertEquals(Money(50L, usd), m1 - m2)
        assertEquals(Money(-100L, usd), -m1)
        assertEquals(Money(300L, usd), m1 * 3L)

        assertTrue(m1 > m2)
        assertTrue(m2 < m1)
        assertEquals(Money.zero(usd), Money(0L, usd))
        assertTrue(Money.zero(usd).isZero())
        assertTrue(m1.isPositive())
        assertTrue((-m1).isNegative())

        val inrMoney = Money(100L, inr)
        assertFailsWith<IllegalArgumentException> {
            m1 + inrMoney
        }
    }

    @Test
    fun testOpeningBalancePosting() {
        val result =
            engine.postOpeningBalance(
                timestamp = 2000L,
                description = "Initial balance",
                amount = Money(1000L, usd),
                account = bankAccount,
                createdAt = 2000L,
            )

        assertTrue(result.isSuccess())
        val posted = result.getOrNull()!!

        assertEquals(TransactionType.ADJUSTMENT, posted.transaction.type)
        assertEquals(1, posted.splits.size)
        assertEquals(2, posted.entries.size)

        val assetEntry = posted.entries.first { it.target is PostingTarget.Account }
        val equityEntry = posted.entries.first { it.target is PostingTarget.Virtual }

        assertEquals(LedgerEntryType.DEBIT, assetEntry.entryType)
        assertEquals(LedgerEntryType.CREDIT, equityEntry.entryType)
        assertEquals(Money(1000L, usd), assetEntry.amount)
        assertEquals(Money(1000L, usd), equityEntry.amount)
        assertEquals(LedgerSourceType.OPENING_BALANCE, assetEntry.sourceType)

        assertTrue(posted.summary.isBalanced)
        assertEquals(Money(1000L, usd), posted.summary.debitTotal)
    }

    @Test
    fun testIncomePosting() {
        val result =
            engine.postIncome(
                timestamp = 2000L,
                description = "Monthly Salary",
                amount = Money(5000L, usd),
                account = bankAccount,
                category = incomeCategory,
                merchantId = null,
                createdAt = 2000L,
            )

        assertTrue(result.isSuccess())
        val posted = result.getOrNull()!!

        assertEquals(TransactionType.INCOME, posted.transaction.type)
        assertEquals(1, posted.splits.size)
        assertEquals(2, posted.entries.size)

        val assetEntry = posted.entries.first { it.target is PostingTarget.Account }
        val incomeEntry = posted.entries.first { it.target is PostingTarget.Virtual }

        assertEquals(LedgerEntryType.DEBIT, assetEntry.entryType)
        assertEquals(LedgerEntryType.CREDIT, incomeEntry.entryType)
        assertEquals(LedgerClass.INCOME, (incomeEntry.target as PostingTarget.Virtual).ledgerClass)
        assertEquals(incomeCategory.id, (incomeEntry.target as PostingTarget.Virtual).categoryId)

        assertTrue(posted.summary.isBalanced)
    }

    @Test
    fun testExpensePosting() {
        val result =
            engine.postExpense(
                timestamp = 2000L,
                description = "Weekly Groceries",
                amount = Money(150L, usd),
                account = bankAccount,
                category = expenseCategory,
                merchantId = null,
                createdAt = 2000L,
            )

        assertTrue(result.isSuccess())
        val posted = result.getOrNull()!!

        assertEquals(TransactionType.EXPENSE, posted.transaction.type)
        assertEquals(1, posted.splits.size)
        assertEquals(2, posted.entries.size)

        val expenseEntry = posted.entries.first { it.target is PostingTarget.Virtual }
        val assetEntry = posted.entries.first { it.target is PostingTarget.Account }

        assertEquals(LedgerEntryType.DEBIT, expenseEntry.entryType)
        assertEquals(LedgerEntryType.CREDIT, assetEntry.entryType)
        assertEquals(LedgerClass.EXPENSE, (expenseEntry.target as PostingTarget.Virtual).ledgerClass)
        assertEquals(expenseCategory.id, (expenseEntry.target as PostingTarget.Virtual).categoryId)

        assertTrue(posted.summary.isBalanced)
    }

    @Test
    fun testTransferPosting() {
        val walletAccount =
            Account(
                id = "acc-wallet",
                name = "My Wallet",
                type = AccountType.WALLET,
                currencyCode = "USD",
                createdAt = 1000L,
                updatedAt = 1000L,
            )

        val result =
            engine.postTransfer(
                timestamp = 2000L,
                description = "Atm cash withdrawal",
                amount = Money(200L, usd),
                sourceAccount = bankAccount,
                targetAccount = walletAccount,
                createdAt = 2000L,
            )

        assertTrue(result.isSuccess())
        val posted = result.getOrNull()!!

        assertEquals(TransactionType.TRANSFER, posted.transaction.type)
        assertEquals(2, posted.splits.size)
        assertEquals(2, posted.entries.size)

        val srcEntry = posted.entries.first { (it.target as PostingTarget.Account).accountId == bankAccount.id }
        val tgtEntry = posted.entries.first { (it.target as PostingTarget.Account).accountId == walletAccount.id }

        assertEquals(LedgerEntryType.CREDIT, srcEntry.entryType)
        assertEquals(LedgerEntryType.DEBIT, tgtEntry.entryType)

        assertTrue(posted.summary.isBalanced)
    }

    @Test
    fun testAdjustmentPosting() {
        val resultDebit =
            engine.postAdjustment(
                timestamp = 2000L,
                description = "Adjusting cash up",
                amount = Money(20L, usd),
                account = bankAccount,
                isDebit = true,
                createdAt = 2000L,
            )

        assertTrue(resultDebit.isSuccess())
        val postedDebit = resultDebit.getOrNull()!!
        val debitAssetEntry = postedDebit.entries.first { it.target is PostingTarget.Account }
        val debitEquityEntry = postedDebit.entries.first { it.target is PostingTarget.Virtual }
        assertEquals(LedgerEntryType.DEBIT, debitAssetEntry.entryType)
        assertEquals(LedgerEntryType.CREDIT, debitEquityEntry.entryType)

        val resultCredit =
            engine.postAdjustment(
                timestamp = 2000L,
                description = "Adjusting cash down",
                amount = Money(15L, usd),
                account = bankAccount,
                isDebit = false,
                createdAt = 2000L,
            )

        assertTrue(resultCredit.isSuccess())
        val postedCredit = resultCredit.getOrNull()!!
        val creditAssetEntry = postedCredit.entries.first { it.target is PostingTarget.Account }
        val creditEquityEntry = postedCredit.entries.first { it.target is PostingTarget.Virtual }
        assertEquals(LedgerEntryType.CREDIT, creditAssetEntry.entryType)
        assertEquals(LedgerEntryType.DEBIT, creditEquityEntry.entryType)
    }

    @Test
    fun testInvalidPostingScenarios() {
        // Negative amount
        val res1 =
            engine.postIncome(
                timestamp = 2000L,
                description = "Negative income",
                amount = Money(-100L, usd),
                account = bankAccount,
                category = incomeCategory,
                merchantId = null,
                createdAt = 2000L,
            )
        assertTrue(res1.isFailure())
        assertTrue(res1.errorOrNull() is LedgerError.NegativeAmount)

        // Zero amount
        val res2 =
            engine.postIncome(
                timestamp = 2000L,
                description = "Zero income",
                amount = Money(0L, usd),
                account = bankAccount,
                category = incomeCategory,
                merchantId = null,
                createdAt = 2000L,
            )
        assertTrue(res2.isFailure())
        assertEquals(LedgerError.ZeroAmount, res2.errorOrNull())

        // Currency mismatch (Account = USD, Money = INR)
        val res3 =
            engine.postIncome(
                timestamp = 2000L,
                description = "Currency mismatch",
                amount = Money(100L, inr),
                account = bankAccount,
                category = incomeCategory,
                merchantId = null,
                createdAt = 2000L,
            )
        assertTrue(res3.isFailure())
        assertTrue(res3.errorOrNull() is LedgerError.CurrencyMismatch)

        // Archived account
        val archivedBank = bankAccount.copy(isArchived = true)
        val res4 =
            engine.postIncome(
                timestamp = 2000L,
                description = "Archived account posting",
                amount = Money(100L, usd),
                account = archivedBank,
                category = incomeCategory,
                merchantId = null,
                createdAt = 2000L,
            )
        assertTrue(res4.isFailure())
        assertTrue(res4.errorOrNull() is LedgerError.AccountArchived)

        // Category type mismatch (Income posting with Expense category)
        val res5 =
            engine.postIncome(
                timestamp = 2000L,
                description = "Category type mismatch",
                amount = Money(100L, usd),
                account = bankAccount,
                category = expenseCategory,
                merchantId = null,
                createdAt = 2000L,
            )
        assertTrue(res5.isFailure())
        assertTrue(res5.errorOrNull() is LedgerError.CategoryTypeMismatch)
    }

    @Test
    fun testDerivedBalanceCalculator() {
        val entries =
            listOf(
                LedgerEntry(
                    "1", "tx-1", null,
                    PostingTarget.Account(
                        "bank",
                        LedgerClass.ASSET,
                    ),
                    Money(5000L, usd), LedgerEntryType.DEBIT, LedgerSourceType.OPENING_BALANCE, null, 1000L,
                ),
                LedgerEntry(
                    "2", "tx-2", null,
                    PostingTarget.Account(
                        "bank",
                        LedgerClass.ASSET,
                    ),
                    Money(1500L, usd), LedgerEntryType.DEBIT, LedgerSourceType.TRANSACTION, null, 1100L,
                ),
                LedgerEntry(
                    "3", "tx-3", null,
                    PostingTarget.Account(
                        "bank",
                        LedgerClass.ASSET,
                    ),
                    Money(200L, usd), LedgerEntryType.CREDIT, LedgerSourceType.TRANSACTION, null, 1200L,
                ),
            )

        // Asset Normal Balance is DEBIT: 5000 + 1500 - 200 = 6300
        val balResult = calculator.calculateBalance(entries, NormalBalance.DEBIT, usd)
        assertTrue(balResult.isSuccess())
        assertEquals(Money(6300L, usd), balResult.getOrNull())

        val ccEntries =
            listOf(
                LedgerEntry(
                    "1", "tx-1", null,
                    PostingTarget.Account(
                        "cc",
                        LedgerClass.LIABILITY,
                    ),
                    Money(1000L, usd), LedgerEntryType.CREDIT, LedgerSourceType.TRANSACTION, null, 1000L,
                ),
                LedgerEntry(
                    "2", "tx-2", null,
                    PostingTarget.Account(
                        "cc",
                        LedgerClass.LIABILITY,
                    ),
                    Money(300L, usd), LedgerEntryType.DEBIT, LedgerSourceType.TRANSACTION, null, 1100L,
                ),
                LedgerEntry(
                    "3", "tx-3", null,
                    PostingTarget.Account(
                        "cc",
                        LedgerClass.LIABILITY,
                    ),
                    Money(450L, usd), LedgerEntryType.CREDIT, LedgerSourceType.TRANSACTION, null, 1200L,
                ),
            )

        // Liability Normal Balance is CREDIT: 1000 - 300 + 450 = 1150
        val ccResult = calculator.calculateBalance(ccEntries, NormalBalance.CREDIT, usd)
        assertTrue(ccResult.isSuccess())
        assertEquals(Money(1150L, usd), ccResult.getOrNull())
    }

    @Test
    fun testPropertyBasedTransferSimulation() {
        val rand = Random(42)
        val accounts =
            mutableListOf(
                Account("acc-1", "Bank A", AccountType.BANK, "USD", createdAt = 0L, updatedAt = 0L),
                Account("acc-2", "Bank B", AccountType.BANK, "USD", createdAt = 0L, updatedAt = 0L),
                Account("acc-3", "Cash", AccountType.CASH, "USD", createdAt = 0L, updatedAt = 0L),
                Account("acc-4", "Wallet", AccountType.WALLET, "USD", createdAt = 0L, updatedAt = 0L),
                Account("acc-5", "Credit Card", AccountType.CREDIT_CARD, "USD", createdAt = 0L, updatedAt = 0L),
            )

        val entryHistory = mutableMapOf<String, MutableList<LedgerEntry>>()
        accounts.forEach { entryHistory[it.id] = mutableListOf() }

        // Post opening balances
        accounts.forEach { acc ->
            val opening =
                engine.postOpeningBalance(
                    timestamp = 1000L,
                    description = "Opening",
                    // $100.00
                    amount = Money(10000L, usd),
                    account = acc,
                    createdAt = 1000L,
                ).getOrNull()!!

            opening.entries.forEach { entry ->
                if (entry.target is PostingTarget.Account) {
                    entryHistory[(entry.target as PostingTarget.Account).accountId]!!.add(entry)
                }
            }
        }

        // Helper to check net assets
        fun verifyNetAssets(): Long {
            var sum = 0L
            accounts.forEach { acc ->
                val entries = entryHistory[acc.id]!!
                val balance = calculator.calculateBalance(entries, acc.type.ledgerClass.normalBalance, usd).getOrNull()!!.amount
                sum +=
                    if (acc.type.ledgerClass.normalBalance == NormalBalance.DEBIT) {
                        balance
                    } else {
                        -balance // Liabilities subtract from net assets
                    }
            }
            return sum
        }

        val initialNetAssets = verifyNetAssets()
        assertEquals(30000L, initialNetAssets) // 4 assets (+40000), 1 liability (-10000)

        // Execute 1000 random transfers
        for (i in 0 until 1000) {
            val src = accounts[rand.nextInt(accounts.size)]
            var tgt = accounts[rand.nextInt(accounts.size)]
            while (src.id == tgt.id) {
                tgt = accounts[rand.nextInt(accounts.size)]
            }

            val amount = Money(rand.nextLong(1L, 100L), usd)
            val res =
                engine.postTransfer(
                    timestamp = 2000L + i,
                    description = "Transfer $i",
                    amount = amount,
                    sourceAccount = src,
                    targetAccount = tgt,
                    createdAt = 2000L + i,
                )

            assertTrue(res.isSuccess(), "Transfer failed at step $i: ${res.errorOrNull()}")
            val posted = res.getOrNull()!!

            posted.entries.forEach { entry ->
                if (entry.target is PostingTarget.Account) {
                    entryHistory[(entry.target as PostingTarget.Account).accountId]!!.add(entry)
                }
            }

            // Assert invariant: net assets must remain CONSTANT
            assertEquals(initialNetAssets, verifyNetAssets(), "Asset mismatch at transfer $i")
        }
    }

    @Test
    fun testPropertyBasedMixTransactionSimulation() {
        val rand = Random(1234)
        val accounts =
            listOf(
                Account("acc-bank", "Bank", AccountType.BANK, "USD", createdAt = 0L, updatedAt = 0L),
                Account("acc-cash", "Cash", AccountType.CASH, "USD", createdAt = 0L, updatedAt = 0L),
                Account("acc-cc", "Credit Card", AccountType.CREDIT_CARD, "USD", createdAt = 0L, updatedAt = 0L),
            )

        val globalVirtualIncomeHistory = mutableListOf<LedgerEntry>()
        val globalVirtualExpenseHistory = mutableListOf<LedgerEntry>()
        val globalVirtualEquityHistory = mutableListOf<LedgerEntry>()
        val accountHistory = accounts.associate { it.id to mutableListOf<LedgerEntry>() }

        // Invariant sums
        var globalDebitSum = 0L
        var globalCreditSum = 0L

        fun addEntry(entry: LedgerEntry) {
            when (val target = entry.target) {
                is PostingTarget.Account -> accountHistory[target.accountId]!!.add(entry)
                is PostingTarget.Virtual -> {
                    when (target.ledgerClass) {
                        LedgerClass.INCOME -> globalVirtualIncomeHistory.add(entry)
                        LedgerClass.EXPENSE -> globalVirtualExpenseHistory.add(entry)
                        LedgerClass.EQUITY -> globalVirtualEquityHistory.add(entry)
                        else -> fail("Unexpected virtual ledger class: ${target.ledgerClass}")
                    }
                }
            }

            when (entry.entryType) {
                LedgerEntryType.DEBIT -> globalDebitSum += entry.amount.amount
                LedgerEntryType.CREDIT -> globalCreditSum += entry.amount.amount
            }
        }

        // Post opening balances
        accounts.forEach { acc ->
            val posted =
                engine.postOpeningBalance(
                    timestamp = 0L,
                    description = "Opening",
                    amount = Money(50000L, usd),
                    account = acc,
                    createdAt = 0L,
                ).getOrNull()!!
            posted.entries.forEach { addEntry(it) }
        }

        // Execute 10,000 mixed random transactions
        for (i in 0 until 10000) {
            val choice = rand.nextInt(4)
            val acc = accounts[rand.nextInt(accounts.size)]
            val amt = Money(rand.nextLong(1L, 500L), usd)

            val result =
                when (choice) {
                    0 ->
                        engine.postIncome(
                            timestamp = i.toLong(),
                            description = "Income $i",
                            amount = amt,
                            account = acc,
                            category = incomeCategory,
                            merchantId = null,
                            createdAt = i.toLong(),
                        )
                    1 ->
                        engine.postExpense(
                            timestamp = i.toLong(),
                            description = "Expense $i",
                            amount = amt,
                            account = acc,
                            category = expenseCategory,
                            merchantId = null,
                            createdAt = i.toLong(),
                        )
                    2 -> {
                        var other = accounts[rand.nextInt(accounts.size)]
                        while (other.id == acc.id) {
                            other = accounts[rand.nextInt(accounts.size)]
                        }
                        engine.postTransfer(
                            timestamp = i.toLong(),
                            description = "Transfer $i",
                            amount = amt,
                            sourceAccount = acc,
                            targetAccount = other,
                            createdAt = i.toLong(),
                        )
                    }
                    else ->
                        engine.postAdjustment(
                            timestamp = i.toLong(),
                            description = "Adjustment $i",
                            amount = amt,
                            account = acc,
                            isDebit = rand.nextBoolean(),
                            createdAt = i.toLong(),
                        )
                }

            assertTrue(result.isSuccess(), "Failed transaction at step $i: ${result.errorOrNull()}")
            val posted = result.getOrNull()!!
            posted.entries.forEach { addEntry(it) }

            // Verify local transaction balanced invariant
            assertEquals(posted.summary.debitTotal.amount, posted.summary.creditTotal.amount)
            assertTrue(posted.summary.isBalanced)

            // Verify global balanced invariant
            assertEquals(globalDebitSum, globalCreditSum, "Global ledger became unbalanced at step $i")
        }
    }

    @Test
    fun testPropertyBasedAdjustmentSimulation() {
        val rand = Random(999)
        val entries = mutableListOf<LedgerEntry>()

        // 1. Start with a random initial sequence of transactions to build current balance
        val initialDebitCount = rand.nextInt(5, 20)
        val initialCreditCount = rand.nextInt(5, 20)

        for (i in 0 until initialDebitCount) {
            entries.add(
                LedgerEntry(
                    id = "deb-$i",
                    transactionId = "tx-deb-$i",
                    target = PostingTarget.Account(bankAccount.id, bankAccount.type.ledgerClass),
                    amount = Money(rand.nextLong(10L, 1000L), usd),
                    entryType = LedgerEntryType.DEBIT,
                    sourceType = LedgerSourceType.TRANSACTION,
                    createdAt = i.toLong(),
                ),
            )
        }

        for (i in 0 until initialCreditCount) {
            entries.add(
                LedgerEntry(
                    id = "cred-$i",
                    transactionId = "tx-cred-$i",
                    target = PostingTarget.Account(bankAccount.id, bankAccount.type.ledgerClass),
                    amount = Money(rand.nextLong(5L, 500L), usd),
                    entryType = LedgerEntryType.CREDIT,
                    sourceType = LedgerSourceType.TRANSACTION,
                    createdAt = i.toLong(),
                ),
            )
        }

        val currentBalance =
            calculator.calculateBalance(
                entries = entries,
                normalBalance = bankAccount.type.ledgerClass.normalBalance,
                currency = usd,
            ).getOrNull()!!

        // 2. Decide a random target balance
        val targetBalance = Money(rand.nextLong(2000L, 50000L), usd)

        // 3. Compute delta and post adjustment
        val delta = targetBalance.amount - currentBalance.amount
        val isDebit = delta > 0
        val absDelta = abs(delta)

        if (absDelta > 0) {
            val adjustmentResult =
                engine.postAdjustment(
                    timestamp = 9999L,
                    description = "Reconcile to target",
                    amount = Money(absDelta, usd),
                    account = bankAccount,
                    isDebit = isDebit,
                    createdAt = 9999L,
                )

            assertTrue(adjustmentResult.isSuccess())
            val posted = adjustmentResult.getOrNull()!!

            // Add the new adjustment ledger entry to our collection
            posted.entries.forEach { entry ->
                if (entry.target is PostingTarget.Account) {
                    entries.add(entry)
                }
            }

            // 4. Derived balance MUST now exactly match expected target balance
            val newDerivedBalance =
                calculator.calculateBalance(
                    entries = entries,
                    normalBalance = bankAccount.type.ledgerClass.normalBalance,
                    currency = usd,
                ).getOrNull()!!

            assertEquals(targetBalance, newDerivedBalance, "Adjustment failed to align derived balance to target")
        }
    }
}
