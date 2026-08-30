package com.tioledger.ui.accounts

import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
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

        val viewModel = testViewModel(accounts, ledger)
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
        val viewModel = testViewModel(accounts, ledger)

        viewModel.onAction(AccountsAction.SearchChanged("sbi"))

        val state = viewModel.uiState.value
        assertEquals("sbi", state.searchQuery)
        assertEquals(1, state.groups.single().accounts.size)
        assertEquals("SBI", state.groups.single().accounts.single().name)
    }

    @Test
    fun createAccountDelegatesThroughUseCaseWithDeterministicGeneratedId() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("generated-account-id"))

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Travel Wallet"))
        viewModel.onAction(AccountsAction.TypeChanged(AccountType.BANK))
        viewModel.onAction(AccountsAction.SaveClicked)

        val created = accounts.created.single()
        assertEquals("generated-account-id", created.id)
        assertEquals("Travel Wallet", created.name)
        assertEquals(AccountType.BANK, created.type)
        assertEquals("USD", created.currencyCode)
    }

    @Test
    fun successfulCreationRefreshesAccountList() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("generated-account-id"))

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Travel Wallet"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertFalse(state.isCreateDialogVisible)
        assertFalse(state.isSaving)
        assertEquals("", state.draftName)
        assertTrue(state.groups.flatMap { it.accounts }.any { it.name == "Travel Wallet" })
    }

    @Test
    fun validationFailureSurfacesThroughExistingErrorMapping() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged(""))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertEquals("name: must not be blank", state.createErrorMessage)
    }

    @Test
    fun failedCreationDoesNotFabricateOrPersistState() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger)
        val stateBeforeSave = viewModel.uiState.value

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged(""))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertTrue(accounts.created.isEmpty())
        assertTrue(state.isCreateDialogVisible)
        assertFalse(state.isSaving)
        assertEquals(stateBeforeSave.groups, state.groups)
        assertEquals(stateBeforeSave.summary, state.summary)
    }
}

private fun testViewModel(
    accounts: FakeAccountRepository,
    ledger: FakeLedgerRepository,
    idGenerator: IdGenerator = FixedIdGenerator("unused-id"),
): AccountsViewModel =
    AccountsViewModel(
        listAccountSummariesUseCase = ListAccountSummariesUseCase(accounts, ledger, BalanceCalculator()),
        createAccountUseCase = CreateAccountUseCase(accounts),
        idGenerator = idGenerator,
        nowProvider = { 1_700_000_000_000L },
    )

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

private class FixedIdGenerator(private val id: String) : IdGenerator {
    override fun nextId(): String = id
}

private class FakeAccountRepository(initialAccounts: List<Account>) : AccountRepository {
    private val accounts = initialAccounts.toMutableList()
    private val createdAccounts = mutableListOf<Account>()
    val created: List<Account> get() = createdAccounts

    override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> =
        LedgerResult.Success(accounts.filter { includeArchived || !it.isArchived })

    override fun findById(accountId: String): LedgerResult<Account> =
        accounts.firstOrNull { it.id == accountId }?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.AccountNotFound(accountId))

    override fun create(account: Account): LedgerResult<Account> {
        accounts += account
        createdAccounts += account
        return LedgerResult.Success(account)
    }

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
