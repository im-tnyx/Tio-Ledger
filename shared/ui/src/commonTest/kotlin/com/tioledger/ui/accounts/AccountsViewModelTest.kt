package com.tioledger.ui.accounts

import com.tioledger.application.usecase.account.ArchiveAccountUseCase
import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.account.UpdateAccountUseCase
import com.tioledger.application.usecase.currency.GetMainCurrencyConfigUseCase
import com.tioledger.application.usecase.currency.SaveMainCurrencyConfigUseCase
import com.tioledger.application.usecase.loan.ListLoansUseCase
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
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.SYSTEM_SUPPORTED_CURRENCIES
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.CurrencyPreferenceRepository
import com.tioledger.domain.repository.LedgerRepository
import com.tioledger.finance.engine.BalanceCalculator
import com.tioledger.ui.more.AccountBillingDetails
import com.tioledger.ui.more.AccountGroupPolicy
import com.tioledger.ui.more.AccountGroupRemovalResult
import com.tioledger.ui.more.AccountGroupStore
import com.tioledger.ui.more.CurrencyPreferenceStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
        viewModel.onAction(AccountsAction.GroupChanged("accounts"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val created = accounts.created.single()
        assertEquals("generated-account-id", created.id)
        assertEquals("Travel Wallet", created.name)
        assertEquals(AccountType.BANK, created.type)
        assertEquals("INR", created.currencyCode)
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
    fun openingBalanceAndCardBillingDraftActionsUpdateState() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        val viewModel = testViewModel(accounts, ledger)

        viewModel.onAction(AccountsAction.OpeningBalanceChanged("1500.50"))
        viewModel.onAction(AccountsAction.PaymentAccountChanged("bank-1"))
        viewModel.onAction(AccountsAction.DescriptionChanged("Primary credit card"))
        viewModel.onAction(AccountsAction.SettlementDateChanged("15"))
        viewModel.onAction(AccountsAction.PaymentDateChanged("25"))

        val state = viewModel.uiState.value
        assertEquals("1500.50", state.draftOpeningBalance)
        assertEquals("bank-1", state.draftPaymentAccountId)
        assertEquals("Primary credit card", state.draftDescription)
        assertEquals("15", state.draftSettlementDate)
        assertEquals("25", state.draftPaymentDate)
    }

    @Test
    fun openingBalanceParserRejectsPrecisionLossAndOverflow() {
        assertEquals(199L, parseAmountToMinor("1.99"))
        assertEquals(100L, parseAmountToMinor("1"))
        assertNull(parseAmountToMinor("1.999"))
        assertNull(parseAmountToMinor("-1.00"))
        assertNull(parseAmountToMinor("999999999999999999999.00"))
    }

    @Test
    fun accountGroupsShowDistinctCardCategoryAndBillingDate() {
        val card = account("card-1", "Travel Card", AccountType.CREDIT_CARD)
        val accounts = FakeAccountRepository(listOf(card))
        val ledger = FakeLedgerRepository()
        val groupStore = AccountGroupStore()
        assertTrue(groupStore.saveBillingDetails(card.id, com.tioledger.ui.more.AccountBillingDetails(12, 25)))

        val viewModel = testViewModel(accounts, ledger, accountGroupStore = groupStore)
        val group = viewModel.uiState.value.groups.single()

        assertEquals("Credit card accounts", group.categoryLabel)
        assertEquals(1, group.accountCount)
        assertEquals(12, group.accounts.single().billingDay)
        assertEquals(25, group.accounts.single().paymentDueDay)
    }

    @Test
    fun editingCreditCardLoadsSavedPaymentAccountDescriptionAndDates() {
        val bank = account("bank-1", "Main bank", AccountType.BANK)
        val card = account("card-1", "Travel Card", AccountType.CREDIT_CARD)
        val groupStore = AccountGroupStore()
        assertTrue(groupStore.assignAccount(bank.id, "accounts"))
        assertTrue(groupStore.assignAccount(card.id, "credit_card"))
        assertTrue(
            groupStore.saveBillingDetails(
                card.id,
                AccountBillingDetails(
                    billingDay = 12,
                    paymentDueDay = 25,
                    paymentAccountId = bank.id,
                    description = "Primary card",
                ),
            ),
        )
        val viewModel = testViewModel(FakeAccountRepository(listOf(bank, card)), FakeLedgerRepository(), accountGroupStore = groupStore)

        viewModel.onAction(AccountsAction.EditAccountRequested(card.id))

        val state = viewModel.uiState.value
        assertEquals(bank.id, state.draftPaymentAccountId)
        assertEquals("Primary card", state.draftDescription)
        assertEquals("12", state.draftSettlementDate)
        assertEquals("25", state.draftPaymentDate)
    }

    @Test
    fun archivedAccountsViewListsOnlyArchivedRecords() {
        val active = account("active", "Daily Account", AccountType.BANK)
        val archived = account("archived", "Old Card", AccountType.CREDIT_CARD).copy(isArchived = true)
        val accounts = FakeAccountRepository(listOf(active, archived))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[archived.id] = listOf(entry("archived-entry", archived, 250L))
        val groupStore = AccountGroupStore()
        val viewModel =
            ArchivedAccountsViewModel(
                ListAccountSummariesUseCase(accounts, ledger, BalanceCalculator()),
                groupStore,
            )

        assertEquals(listOf("archived"), viewModel.uiState.value.accounts.map { it.id })
        assertEquals("Old Card", viewModel.uiState.value.accounts.single().name)
        assertEquals("-USD 2.50", viewModel.uiState.value.accounts.single().balance)
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
        val groupStore = AccountGroupStore()
        val loanGroup = groupStore.addGroup("Loans", AccountGroupPolicy.LOAN)!!
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("loan-linked-id"), groupStore)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Home Loan Account"))
        viewModel.onAction(AccountsAction.GroupChanged(loanGroup.id))
        viewModel.onAction(AccountsAction.SaveClicked)

        val created = accounts.created.single()
        assertEquals("loan-linked-id", created.id)
        assertEquals(AccountType.LOAN_LINKED, created.type)
        assertFalse(created.isArchived)
    }

    @Test
    fun selectedGroupControlsCreatedAccountTypeWithoutIndependentSelector() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val groupStore = AccountGroupStore()
        val investmentGroup = groupStore.addGroup("My investments", AccountGroupPolicy.INVESTMENT)!!
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("investment-id"), groupStore)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Index fund"))
        viewModel.onAction(AccountsAction.GroupChanged(investmentGroup.id))
        viewModel.onAction(AccountsAction.SaveClicked)

        val created = accounts.created.single()
        assertEquals(AccountType.INVESTMENT, created.type)
        assertEquals(investmentGroup.id, groupStore.groupIdForAccount(created.id))
    }

    @Test
    fun persistedMainCurrencyReachesAccountPersistenceUnchanged() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val viewModel =
            testViewModel(
                accounts = accounts,
                ledger = FakeLedgerRepository(),
                idGenerator = FixedIdGenerator("inr-id"),
                currencyPreferenceStore = currencyPreferenceStore("INR"),
            )

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Mumbai Cash"))
        viewModel.onAction(AccountsAction.SaveClicked)

        assertEquals("INR", accounts.created.single().currencyCode)
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
        viewModel.onAction(AccountsAction.CreateDismissed)

        val state = viewModel.uiState.value
        assertFalse(state.isCreateDialogVisible)
        assertEquals("", state.draftName)
        assertEquals("cash", state.draftGroupId)
        assertEquals(SUPPORTED_ACCOUNT_CURRENCY_CODES.first(), state.draftCurrencyCode)
        assertEquals(null, state.createErrorMessage)
    }

    @Test
    fun fullDraftResetsAfterSuccessfulCreation() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val groupStore = AccountGroupStore()
        val loanGroup = groupStore.addGroup("Loans", AccountGroupPolicy.LOAN)!!
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("reset-id"), groupStore)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Home Loan Account"))
        viewModel.onAction(AccountsAction.GroupChanged(loanGroup.id))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertFalse(state.isCreateDialogVisible)
        assertEquals("", state.draftName)
        assertEquals("cash", state.draftGroupId)
        assertEquals(SUPPORTED_ACCOUNT_CURRENCY_CODES.first(), state.draftCurrencyCode)
    }

    @Test
    fun editAccountRequestedAndSavedUpdatesAccount() {
        val usdAccount = account("acc-1", "Old Name", AccountType.BANK).copy(currencyCode = "INR")
        val accounts = FakeAccountRepository(listOf(usdAccount))
        val ledger = FakeLedgerRepository()
        val viewModel = testViewModel(accounts, ledger)

        viewModel.onAction(AccountsAction.EditAccountRequested("acc-1"))
        val draftState = viewModel.uiState.value
        assertEquals("acc-1", draftState.editingAccountId)
        assertEquals("Old Name", draftState.draftName)

        viewModel.onAction(AccountsAction.NameChanged("New Name"))
        viewModel.onAction(AccountsAction.SaveClicked)

        val updatedResult = accounts.findById("acc-1")
        assertTrue(updatedResult is LedgerResult.Success)
        val updated = updatedResult.value
        assertEquals("New Name", updated.name)
        assertEquals("INR", updated.currencyCode)
        kotlin.test.assertNull(viewModel.uiState.value.editingAccountId)
    }

    @Test
    fun loadAccountsPreservesExistingAccountCurrencyWhenMainCurrencyDiffers() {
        val usdAccount = account("acc-1", "My Bank", AccountType.BANK).copy(currencyCode = "USD")
        val accounts = FakeAccountRepository(listOf(usdAccount))
        val ledger = FakeLedgerRepository()
        val inrStore = currencyPreferenceStore("INR")
        val viewModel = testViewModel(accounts, ledger, currencyPreferenceStore = inrStore)

        val updatedResult = accounts.findById("acc-1")
        assertTrue(updatedResult is LedgerResult.Success)
        assertEquals("USD", updatedResult.value.currencyCode)
        assertEquals("USD 0.00", viewModel.uiState.value.groups.single().accounts.single().balance)
    }

    @Test
    fun validationFailurePreservesEditableDraft() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("cash-entry", cash, 1_000L))
        val groupStore = AccountGroupStore()
        val loanGroup = groupStore.addGroup("Loans", AccountGroupPolicy.LOAN)!!
        val viewModel = testViewModel(accounts, ledger, accountGroupStore = groupStore)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged(""))
        viewModel.onAction(AccountsAction.GroupChanged(loanGroup.id))
        viewModel.onAction(AccountsAction.SaveClicked)

        val state = viewModel.uiState.value
        assertTrue(state.isCreateDialogVisible)
        assertEquals(loanGroup.id, state.draftGroupId)
        assertEquals("INR", state.draftCurrencyCode)
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

    @Test
    fun invalidOrDeletedGroupRejectsBeforeAccountPersistence() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        val groupStore = AccountGroupStore()
        val custom = groupStore.addGroup("Temporary", AccountGroupPolicy.BANK)!!
        val viewModel = testViewModel(accounts, FakeLedgerRepository(), FixedIdGenerator("blocked-id"), groupStore)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Blocked account"))
        viewModel.onAction(AccountsAction.GroupChanged(custom.id))
        assertEquals(AccountGroupRemovalResult.Removed, groupStore.removeGroup(custom.id))
        viewModel.onAction(AccountsAction.SaveClicked)

        assertTrue(accounts.created.isEmpty())
        assertEquals("A valid Account Group is required.", viewModel.uiState.value.createErrorMessage)
        assertNull(groupStore.groupIdForAccount("blocked-id"))
    }

    @Test
    fun failedAccountSaveCleansPreparedGroupAssignment() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        accounts.createResult = { LedgerResult.Failure(LedgerError.StorageUnavailable) }
        val groupStore = AccountGroupStore()
        val viewModel = testViewModel(accounts, FakeLedgerRepository(), FixedIdGenerator("failed-id"), groupStore)

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("Not saved"))
        viewModel.onAction(AccountsAction.SaveClicked)

        assertTrue(accounts.created.isEmpty())
        assertNull(groupStore.groupIdForAccount("failed-id"))
        assertEquals("Unable to save this account. Try again.", viewModel.uiState.value.createErrorMessage)
    }

    @Test
    fun archivePreservesHistoricalLedgerEntriesAndGroupAssignment() {
        val bank = account("bank", "Primary Bank", AccountType.BANK)
        val accounts = FakeAccountRepository(listOf(bank))
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[bank.id] = listOf(entry("bank-entry", bank, 2_500L))
        val ledgerBefore = ledger.entriesByAccount.toMap()
        val groupStore = AccountGroupStore()
        assertTrue(groupStore.assignAccount(bank.id, "accounts"))
        val viewModel = testViewModel(accounts, ledger, accountGroupStore = groupStore)

        viewModel.onAction(AccountsAction.ArchiveRequested(bank.id, bank.name))
        viewModel.onAction(AccountsAction.ArchiveConfirmed)

        val archived = (accounts.findById(bank.id) as LedgerResult.Success).value
        assertTrue(archived.isArchived)
        assertTrue(viewModel.uiState.value.isEmpty)
        assertEquals(ledgerBefore, ledger.entriesByAccount)
        assertEquals("accounts", groupStore.groupIdForAccount(bank.id))
    }

    @Test
    fun archiveFailureKeepsAccountActiveAndConfirmationVisible() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        accounts.updateResult = { LedgerResult.Failure(LedgerError.StorageUnavailable) }
        val viewModel = testViewModel(accounts, FakeLedgerRepository())

        viewModel.onAction(AccountsAction.ArchiveRequested(cash.id, cash.name))
        viewModel.onAction(AccountsAction.ArchiveConfirmed)

        val stored = (accounts.findById(cash.id) as LedgerResult.Success).value
        assertFalse(stored.isArchived)
        assertEquals(cash.id, viewModel.uiState.value.archiveAccountId)
        assertEquals("Unable to archive this account. Try again.", viewModel.uiState.value.archiveErrorMessage)
    }

    @Test
    fun repeatedArchiveConfirmationOnlyUpdatesOnce() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val accounts = FakeAccountRepository(listOf(cash))
        lateinit var viewModel: AccountsViewModel
        viewModel = testViewModel(accounts, FakeLedgerRepository())
        accounts.beforeUpdate = { viewModel.onAction(AccountsAction.ArchiveConfirmed) }

        viewModel.onAction(AccountsAction.ArchiveRequested(cash.id, cash.name))
        viewModel.onAction(AccountsAction.ArchiveConfirmed)

        assertEquals(1, accounts.updateCount)
        assertTrue((accounts.findById(cash.id) as LedgerResult.Success).value.isArchived)
    }

    @Test
    fun legacyUnassignedAccountRemainsVisibleWithItsExistingType() {
        val legacyBank = account("legacy-bank", "Legacy bank", AccountType.BANK)
        val groupStore = AccountGroupStore()

        val state =
            testViewModel(
                accounts = FakeAccountRepository(listOf(legacyBank)),
                ledger = FakeLedgerRepository(),
                accountGroupStore = groupStore,
            ).uiState.value

        assertEquals("Accounts", state.groups.single().title)
        assertEquals(AccountType.BANK, state.groups.single().type)
        assertNull(groupStore.groupIdForAccount(legacyBank.id))
    }

    @Test
    fun accountCreationDoesNotModifyExistingLedgerEntries() {
        val cash = account("cash", "Cash", AccountType.CASH)
        val ledger = FakeLedgerRepository()
        ledger.entriesByAccount[cash.id] = listOf(entry("existing-entry", cash, 1_000L))
        val before = ledger.entriesByAccount.toMap()
        val accounts = FakeAccountRepository(listOf(cash))
        val viewModel = testViewModel(accounts, ledger, FixedIdGenerator("new-account"))

        viewModel.onAction(AccountsAction.AddClicked)
        viewModel.onAction(AccountsAction.NameChanged("New cash"))
        viewModel.onAction(AccountsAction.SaveClicked)

        assertEquals(before, ledger.entriesByAccount)
        assertEquals(AccountType.CASH, accounts.created.single().type)
    }

    @Test
    fun assignedCustomGroupControlsAccountListingTitle() {
        val defaultBank = account("default-bank", "Primary Bank", AccountType.BANK)
        val bank = account("bank", "Emergency Reserve", AccountType.BANK)
        val accounts = FakeAccountRepository(listOf(defaultBank, bank))
        val ledger = FakeLedgerRepository()
        val groupStore = AccountGroupStore()
        val custom = groupStore.addGroup("Emergency Savings", AccountGroupPolicy.BANK)!!
        groupStore.assignAccount(bank.id, custom.id)

        val state = testViewModel(accounts, ledger, accountGroupStore = groupStore).uiState.value

        val customGroup = state.groups.single { it.title == "Emergency Savings" }
        assertEquals(listOf(bank.id), customGroup.accounts.map { it.id })
        assertEquals(listOf("Accounts", "Emergency Savings"), state.groups.map { it.title })
        assertEquals(2, state.groups.map { it.id }.toSet().size)
    }
}

private fun testViewModel(
    accounts: FakeAccountRepository,
    ledger: FakeLedgerRepository,
    idGenerator: IdGenerator = FixedIdGenerator("unused-id"),
    accountGroupStore: AccountGroupStore = AccountGroupStore(),
    currencyPreferenceStore: CurrencyPreferenceStore? = null,
    listLoansUseCase: ListLoansUseCase? = null,
    updateAccountUseCase: UpdateAccountUseCase? = UpdateAccountUseCase(accounts),
): AccountsViewModel =
    AccountsViewModel(
        listAccountSummariesUseCase = ListAccountSummariesUseCase(accounts, ledger, BalanceCalculator()),
        createAccountUseCase = CreateAccountUseCase(accounts),
        archiveAccountUseCase = ArchiveAccountUseCase(accounts),
        idGenerator = idGenerator,
        nowProvider = { 1_700_000_000_000L },
        accountGroupStore = accountGroupStore,
        currencyPreferenceStore = currencyPreferenceStore,
        listLoansUseCase = listLoansUseCase,
        updateAccountUseCase = updateAccountUseCase,
    )

private fun currencyPreferenceStore(currencyCode: String): CurrencyPreferenceStore {
    val config =
        MainCurrencyConfig(
            currency = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == currencyCode },
        )
    val repository =
        object : CurrencyPreferenceRepository {
            private var current = config

            override fun getConfig(): MainCurrencyConfig = current

            override fun saveConfig(config: MainCurrencyConfig): Boolean {
                current = config
                return true
            }
        }
    return CurrencyPreferenceStore(
        GetMainCurrencyConfigUseCase(repository),
        SaveMainCurrencyConfigUseCase(repository),
    )
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
    var beforeUpdate: (() -> Unit)? = null
    var updateResult: ((Account) -> LedgerResult<Account>)? = null
    var updateCount = 0

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

    override fun update(account: Account): LedgerResult<Account> {
        beforeUpdate?.invoke()
        updateCount++
        val result = updateResult?.invoke(account) ?: LedgerResult.Success(account)
        if (result is LedgerResult.Success) {
            val index = accounts.indexOfFirst { it.id == account.id }
            if (index >= 0) accounts[index] = account
        }
        return result
    }
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
