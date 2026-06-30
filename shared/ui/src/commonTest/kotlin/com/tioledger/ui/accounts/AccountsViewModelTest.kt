package com.tioledger.ui.accounts

import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntry
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.LedgerRepository
import com.tioledger.finance.engine.BalanceCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountsViewModelTest {
    @Test
    fun loadAccountsBuildsGroupedUiState() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val bank = account("bank", "SBI", AccountType.BANK)
        val accounts = FakeAccountRepository(listOf(cash, bank))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        ledger.entriesByAccount[bank.id] = listOf(entry("bank-entry", bank, 2_500L))

        val viewModel = AccountsViewModel(ListAccountSummariesUseCase(accounts, ledger, BalanceCalculator()))
        val state = viewModel.uiState.value

        assertFalse(state.isLoading)
        assertEquals("USD 35.00", state.summary.assets)
        assertEquals(2, state.groups.size)
        assertTrue(state.groups.any { it.title == "Cash" })
        assertTrue(state.groups.any { it.title == "Accounts" })
    }

    @Test
    fun searchActionFiltersAccounts() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val bank = account("bank", "SBI", AccountType.BANK)
        val accounts = FakeAccountRepository(listOf(cash, bank))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        ledger.entriesByAccount[bank.id] = listOf(entry("bank-entry", bank, 2_500L))
        val viewModel = AccountsViewModel(ListAccountSummariesUseCase(accounts, ledger, BalanceCalculator()))

        viewModel.onAction(AccountsAction.SearchChanged("sbi"))

        val state = viewModel.uiState.value
        assertEquals("sbi", state.searchQuery)
        assertEquals(1, state.groups.single().accounts.size)
        assertEquals("SBI", state.groups.single().accounts.single().name)
    }
}

private fun account(
    id: String,
    name: String,
    type: AccountType,
): Account =
    Account(
        id = id,
        name = name,
        type = type,
        currencyCode = "USD",
        createdAt = 1L,
        updatedAt = 1L,
    )

private fun entry(
    id: String,
    account: Account,
    amount: Long,
): LedgerEntry =
    LedgerEntry(
        id = id,
        transactionId = "txn-$id",
        target = PostingTarget.Account(account.id, account.type.ledgerClass),
        amount = Money(amount, CurrencyCode("USD")),
        entryType = LedgerEntryType.DEBIT,
        sourceType = LedgerSourceType.TRANSACTION,
        createdAt = 1L,
    )

private class FakeAccountRepository(private val accountList: List<Account>) : AccountRepository {
    override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> =
        LedgerResult.Success(accountList.filter { includeArchived || !it.isArchived })

    override fun findById(accountId: String): LedgerResult<Account> =
        accountList.firstOrNull { it.id == accountId }?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.AccountNotFound(accountId))

    override fun create(account: Account): LedgerResult<Account> = LedgerResult.Success(account)

    override fun update(account: Account): LedgerResult<Account> = LedgerResult.Success(account)
}

private class FakeLedgerRepository : LedgerRepository {
    val entriesByAccount: MutableMap<String, List<LedgerEntry>> = mutableMapOf()

    override fun findEntriesByAccount(accountId: String): LedgerResult<List<LedgerEntry>> =
        LedgerResult.Success(entriesByAccount[accountId].orEmpty())

    override fun findEntriesByTransaction(transactionId: String): LedgerResult<List<LedgerEntry>> =
        LedgerResult.Success(
            entriesByAccount.values.flatten().filter { it.transactionId == transactionId },
        )
}
