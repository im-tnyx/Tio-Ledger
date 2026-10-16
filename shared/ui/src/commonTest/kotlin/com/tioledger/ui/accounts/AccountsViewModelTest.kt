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
    fun searchUsesLoadedSnapshotAndExplicitLoadRefreshesLedgerBalances() {
        val cash = account("cash", "Travel Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("initial", cash, 100L))
        val viewModel = testViewModel(accounts, ledger)
        ledger.entriesByAccount[cash.id] = listOf(entry("updated", cash, 200L))

        listOf("T", "Tr", "missing", "", "   ").forEach { query ->
            viewModel.onAction(AccountsAction.SearchChanged(query))
        }
        assertEquals(1, accounts.readCount)
        assertEquals(1, ledger.readCount)
        assertEquals(listOf("USD 1.00"), viewModel.uiState.value.groups.single().totals)

        viewModel.onAction(AccountsAction.Load)
        assertEquals(2, accounts.readCount)
        assertEquals(2, ledger.readCount)
        assertEquals(listOf("USD 2.00"), viewModel.uiState.value.groups.single().totals)
    }

    @Test
    fun sameTypeMixedCurrencyAccountsLoadWithoutCrashing() {
        val usdCash = account("usd-cash", "US Cash", AccountType.CASH)
        val inrCash = account("inr-cash", "India Cash", AccountType.CASH).copy(currencyCode = "INR")
        val accounts = FakeAccountRepository(listOf(usdCash, inrCash))

        val state = testViewModel(accounts, FakeLedgerRepository()).uiState.value

        assertFalse(state.isLoading)
        assertEquals(AccountType.CASH, state.groups.single().type)
        assertEquals(listOf("INR 0.00", "USD 0.00"), state.groups.single().totals)
        assertEquals(listOf("usd-cash", "inr-cash"), state.groups.single().accounts.map { it.id })
        assertTrue(accounts.created.isEmpty())
    }

    @Test
    fun mixedCurrencyGroupFormatsSeparateSignedSubtotals() {
        val usdCash = account("usd", "US Cash", AccountType.CASH)
        val inrCash = account("inr", "India Cash", AccountType.CASH).copy(currencyCode = "INR")
        val extraUsdCash = account("extra-usd", "Travel Cash", AccountType.CASH)
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[usdCash.id] = listOf(entry("usd-entry", usdCash, 1_000L))
        ledger.entriesByAccount[inrCash.id] = listOf(entry("inr-entry", inrCash, 500L, LedgerEntryType.CREDIT))
        ledger.entriesByAccount[extraUsdCash.id] = listOf(entry("extra-entry", extraUsdCash, 250L))
        val before = ledger.entriesByAccount.toMap()
        val accounts = FakeAccountRepository(listOf(usdCash, inrCash, extraUsdCash))

        val group = testViewModel(accounts, ledger).uiState.value.groups.single()

        assertEquals("Cash", group.title)
        assertEquals(listOf("-INR 5.00", "USD 12.50"), group.totals)
        assertEquals(listOf("USD 10.00", "-INR 5.00", "USD 2.50"), group.accounts.map { it.balance })
        assertEquals(before, ledger.entriesByAccount)
        assertTrue(accounts.created.isEmpty())
    }

    @Test
    fun liabilityGroupsPreserveBalancesWithoutNetAssetSignConversion() {
        val usdCard = account("usd-card", "US Card", AccountType.CREDIT_CARD)
        val inrCard = account("inr-card", "India Card", AccountType.CREDIT_CARD).copy(currencyCode = "INR")
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[usdCard.id] = listOf(entry("usd-entry", usdCard, 1_000L, LedgerEntryType.CREDIT))
        ledger.entriesByAccount[inrCard.id] = listOf(entry("inr-entry", inrCard, 250L))

        val state = testViewModel(FakeAccountRepository(listOf(usdCard, inrCard)), ledger).uiState.value

        assertEquals(AccountType.CREDIT_CARD, state.groups.single().type)
        assertEquals(listOf("-INR 2.50", "USD 10.00"), state.groups.single().totals)
    }

    @Test
    fun searchChangesVisibleCurrencySubtotalsWithoutChangingGlobalSummary() {
        val usdCash = account("usd", "Travel Dollar", AccountType.CASH)
        val inrCash = account("inr", "India Cash", AccountType.CASH).copy(currencyCode = "INR")
        val extraInrCash = account("extra-inr", "India Reserve", AccountType.CASH).copy(currencyCode = "INR")
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[usdCash.id] = listOf(entry("usd-entry", usdCash, 1_000L))
        ledger.entriesByAccount[inrCash.id] = listOf(entry("inr-entry", inrCash, 250L))
        ledger.entriesByAccount[extraInrCash.id] = listOf(entry("extra-entry", extraInrCash, 500L))
        val viewModel = testViewModel(FakeAccountRepository(listOf(usdCash, inrCash, extraInrCash)), ledger)
        val initial = viewModel.uiState.value

        viewModel.onAction(AccountsAction.SearchChanged("iNdIa"))
        assertEquals(listOf("INR 7.50"), viewModel.uiState.value.groups.single().totals)
        viewModel.onAction(AccountsAction.SearchChanged("Reserve"))
        assertEquals(listOf("INR 5.00"), viewModel.uiState.value.groups.single().totals)
        viewModel.onAction(AccountsAction.SearchChanged("missing"))
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(initial.summary, viewModel.uiState.value.summary)
        viewModel.onAction(AccountsAction.SearchChanged(""))
        assertEquals(initial.groups, viewModel.uiState.value.groups)
        viewModel.onAction(AccountsAction.SearchChanged("   "))
        assertEquals(initial.groups, viewModel.uiState.value.groups)
    }

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

    @Test
    fun cashAccountCreationDelegatesThroughUseCase() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("cash-id"))

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Pocket Cash"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val created = accounts.created.single()
        assertEquals("cash-id", created.id)
        assertEquals(AccountType.CASH, created.type)
    }

    @Test
    fun loanLinkedAccountCreationDelegatesThroughUseCase() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("loan-linked-id"))

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Home Loan Account"))
        viewModel.onAction(AccountsAction.TypeChanged(AccountType.LOAN_LINKED))
        viewModel.onAction(AccountsAction.SaveClicked)

        val created = accounts.created.single()
        assertEquals("loan-linked-id", created.id)
        assertEquals(AccountType.LOAN_LINKED, created.type)
        assertFalse(created.isArchived)
    }

    @Test
    fun selectedNonUsdCurrencyReachesPersistenceWithoutHiddenOverwrite() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("inr-id"))

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Mumbai Cash"))
        viewModel.onAction(AccountsAction.TypeChanged(AccountType.CASH))
        viewModel.onAction(AccountsAction.CurrencyChanged("INR"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val created = accounts.created.single()
        assertEquals("INR", created.currencyCode)
        assertEquals(listOf("INR 0.00", "USD 10.00"), viewModel.uiState.value.groups.single().totals)
    }

    @Test
    fun cancelPersistsNothing() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger)
        val stateBeforeOpen = viewModel.uiState.value

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Abandoned Draft"))
        viewModel.onAction(AccountsAction.TypeChanged(AccountType.LOAN_LINKED))
        viewModel.onAction(AccountsAction.CurrencyChanged("EUR"))
        viewModel.onAction(AccountsAction.CreateDismissed)

        val state = viewModel.uiState.value
        assertTrue(accounts.created.isEmpty())
        assertEquals(stateBeforeOpen.groups, state.groups)
        assertEquals(stateBeforeOpen.summary, state.summary)
    }

    @Test
    fun fullDraftResetsAfterCancel() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Abandoned Draft"))
        viewModel.onAction(AccountsAction.TypeChanged(AccountType.LOAN_LINKED))
        viewModel.onAction(AccountsAction.CurrencyChanged("EUR"))
        viewModel.onAction(AccountsAction.CreateDismissed)

        val state = viewModel.uiState.value
        assertFalse(state.isCreateDialogVisible)
        assertEquals("", state.draftName)
        assertEquals(AccountType.CASH, state.draftType)
        assertEquals(SUPPORTED_ACCOUNT_CURRENCY_CODES.first(), state.draftCurrencyCode)
        assertEquals(null, state.createErrorMessage)
    }

    @Test
    fun fullDraftResetsAfterSuccessfulCreation() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("reset-id"))

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Home Loan Account"))
        viewModel.onAction(AccountsAction.TypeChanged(AccountType.LOAN_LINKED))
        viewModel.onAction(AccountsAction.CurrencyChanged("GBP"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertFalse(state.isCreateDialogVisible)
        assertEquals("", state.draftName)
        assertEquals(AccountType.CASH, state.draftType)
        assertEquals(SUPPORTED_ACCOUNT_CURRENCY_CODES.first(), state.draftCurrencyCode)
    }

    @Test
    fun validationFailurePreservesEditableDraft() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged(""))
        viewModel.onAction(AccountsAction.TypeChanged(AccountType.LOAN_LINKED))
        viewModel.onAction(AccountsAction.CurrencyChanged("EUR"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertTrue(state.isCreateDialogVisible)
        assertEquals(AccountType.LOAN_LINKED, state.draftType)
        assertEquals("EUR", state.draftCurrencyCode)
        assertEquals("name: must not be blank", state.createErrorMessage)
    }

    @Test
    fun repositoryFailureDoesNotFabricateAnAccount() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        accounts.createResult = { LedgerResult.Failure(LedgerError.StorageUnavailable) }
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger)
        val stateBeforeSave = viewModel.uiState.value

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Should Not Persist"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertTrue(accounts.created.isEmpty())
        assertTrue(state.isCreateDialogVisible)
        assertFalse(state.isSaving)
        assertEquals("Should Not Persist", state.draftName)
        assertEquals(stateBeforeSave.groups, state.groups)
    }

    @Test
    fun repositoryFailureSurfacesCreateSpecificFeedback() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        accounts.createResult = { LedgerResult.Failure(LedgerError.StorageUnavailable) }
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val viewModel = testViewModel(accounts, ledger)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Should Not Persist"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertEquals("Unable to save this account. Try again.", state.createErrorMessage)
        assertTrue(state.createErrorMessage != "Unable to load accounts.")
    }

    @Test
    fun repeatedSaveProtectionRemainsDeterministic() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        lateinit var viewModel: AccountsViewModel
        viewModel = testViewModel(accounts, ledger, FixedIdGenerator("single-id"))
        accounts.beforeCreate = {
            // Simulates a second Save tap arriving while the first submission is still in flight.
            viewModel.onAction(AccountsAction.SaveClicked)
        }

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Only Once"))
        viewModel.onAction(AccountsAction.SaveClicked)

        assertEquals(1, accounts.created.size)
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
    entryType: LedgerEntryType = LedgerEntryType.DEBIT,
): LedgerEntry =
    LedgerEntry(
        id = id,
        transactionId = "txn-$id",
        target = PostingTarget.Account(account.id, account.type.ledgerClass),
        amount = Money(amount, CurrencyCode(account.currencyCode)),
        entryType = entryType,
        sourceType = LedgerSourceType.TRANSACTION,
        createdAt = 1L,
    )

private class FixedIdGenerator(private val id: String) : IdGenerator {
    override fun nextId(): String = id
}

private class FakeAccountRepository(initialAccounts: List<Account>) : AccountRepository {
    var readCount = 0
    private val accounts = initialAccounts.toMutableList()
    private val createdAccounts = mutableListOf<Account>()
    val created: List<Account> get() = createdAccounts
    var beforeCreate: (() -> Unit)? = null
    var createResult: ((Account) -> LedgerResult<Account>)? = null

    override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> {
        readCount++
        return LedgerResult.Success(accounts.filter { includeArchived || !it.isArchived })
    }

    override fun findById(accountId: String): LedgerResult<Account> =
        accounts.firstOrNull { it.id == accountId }?.let { LedgerResult.Success(it) }
            ?: LedgerResult.Failure(LedgerError.AccountNotFound(accountId))

    override fun create(account: Account): LedgerResult<Account> {
        beforeCreate?.invoke()
        val result = createResult?.invoke(account) ?: LedgerResult.Success(account)
        if (result is LedgerResult.Success) {
            accounts += account
            createdAccounts += account
        }
        return result
    }

    override fun update(account: Account): LedgerResult<Account> = LedgerResult.Success(account)
}

private class FakeLedgerRepository : LedgerRepository {
    var readCount = 0
    val entriesByAccount: MutableMap<String, List<LedgerEntry>> = mutableMapOf()

    override fun findEntriesByAccount(accountId: String): LedgerResult<List<LedgerEntry>> {
        readCount++
        return LedgerResult.Success(entriesByAccount[accountId].orEmpty())
    }

    override fun findEntriesByTransaction(transactionId: String): LedgerResult<List<LedgerEntry>> =
        LedgerResult.Success(
            entriesByAccount.values.flatten().filter { it.transactionId == transactionId },
        )
}
