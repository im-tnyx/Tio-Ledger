package com.tioledger.ui.more

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.repository.AccountRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AccountGroupStoreTest {
    private class TestIdGenerator(var nextVal: String = "test-id-1") : IdGenerator {
        override fun nextId(): String = nextVal
    }

    @Test
    fun storeInitializesWithThreePermanentDefaultGroups() {
        val store = AccountGroupStore(TestIdGenerator())
        val groups = store.groups.value

        assertEquals(3, groups.size)
        assertEquals(listOf("Cash", "Accounts", "Credit Card"), groups.map { it.title })
        assertEquals(AccountGroupPolicy.CASH, groups[0].policy)
        assertEquals(AccountGroupPolicy.BANK, groups[1].policy)
        assertEquals(AccountGroupPolicy.CREDIT_CARD, groups[2].policy)
        assertTrue(groups.all { it.isPermanent })
    }

    @Test
    fun addGroupWithLoanPolicyCreatesNonPermanentEntry() {
        val idGen = TestIdGenerator("loan-123")
        val store = AccountGroupStore(idGen)

        val created = store.addGroup("Home Loan", AccountGroupPolicy.LOAN)
        assertNotNull(created)

        assertEquals("group_loan-123", created.id)
        assertEquals("Home Loan", created.title)
        assertEquals("Loan", created.subtitle)
        assertEquals(AccountGroupPolicy.LOAN, created.policy)
        assertFalse(created.isPermanent)

        val groups = store.groups.value
        assertEquals(4, groups.size)
        assertEquals(created, groups.last())
    }

    @Test
    fun permanentGroupsCannotBeRemoved() {
        val store = AccountGroupStore()
        val cashGroup = store.groups.value.first { it.id == "cash" }

        val removed = store.removeGroup(cashGroup.id)
        assertEquals(AccountGroupRemovalResult.PermanentGroup, removed)
        assertEquals(3, store.groups.value.size)
    }

    @Test
    fun customGroupsCanBeRemoved() {
        val store = AccountGroupStore()
        val custom = store.addGroup("My Stocks", AccountGroupPolicy.INVESTMENT)
        assertNotNull(custom)
        assertEquals(4, store.groups.value.size)

        val removed = store.removeGroup(custom.id)
        assertEquals(AccountGroupRemovalResult.Removed, removed)
        assertEquals(3, store.groups.value.size)
        assertFalse(store.groups.value.any { it.id == custom.id })
    }

    @Test
    fun groupContainingAccountsCannotBeRemovedOrUnassignAccounts() {
        val store = AccountGroupStore()
        val custom = store.addGroup("Emergency Savings", AccountGroupPolicy.BANK)
        assertNotNull(custom)
        store.assignAccount("account-1", custom.id)

        assertEquals(AccountGroupRemovalResult.GroupContainsAccounts, store.removeGroup(custom.id))
        assertEquals(custom.id, store.groupIdForAccount("account-1"))
    }

    @Test
    fun updateGroupChangesTitleOnly() {
        val store = AccountGroupStore()
        val custom = store.addGroup("Initial Name", AccountGroupPolicy.BANK)!!

        val updated = store.updateGroup(custom.id, "Renamed Account")
        assertTrue(updated)

        val entry = store.groups.value.first { it.id == custom.id }
        assertEquals("Renamed Account", entry.title)
        assertEquals(AccountGroupPolicy.BANK, entry.policy)
    }

    @Test
    fun reorderMovesItemsCorrectly() {
        val store = AccountGroupStore()
        store.reorder(0, 2)

        val titles = store.groups.value.map { it.title }
        assertEquals(listOf("Accounts", "Credit Card", "Cash"), titles)
    }

    @Test
    fun allPoliciesMapToCorrectAccountType() {
        assertEquals(AccountType.CASH, AccountGroupPolicy.CASH.accountType)
        assertEquals(AccountType.BANK, AccountGroupPolicy.BANK.accountType)
        assertEquals(AccountType.CREDIT_CARD, AccountGroupPolicy.CREDIT_CARD.accountType)
        assertEquals(AccountType.LOAN_LINKED, AccountGroupPolicy.LOAN.accountType)
        assertEquals(AccountType.INVESTMENT, AccountGroupPolicy.INVESTMENT.accountType)
        assertEquals(AccountType.WALLET, AccountGroupPolicy.OTHER.accountType)
    }

    @Test
    fun groupsAndAccountAssignmentsSurviveStoreRecreation() {
        val persistence = InMemoryAccountGroupPersistence()
        val firstStore = AccountGroupStore(TestIdGenerator("persisted"), persistence = persistence)
        val custom = firstStore.addGroup("Investments", AccountGroupPolicy.INVESTMENT)!!
        firstStore.assignAccount("account-1", custom.id)

        val secondStore = AccountGroupStore(TestIdGenerator(), persistence = persistence)

        assertEquals(custom, secondStore.groups.value.last())
        assertEquals(custom.id, secondStore.groupIdForAccount("account-1"))
    }

    @Test
    fun assignmentRejectsMissingGroupAndMismatchedPolicy() {
        val store = AccountGroupStore()

        assertEquals(
            AccountGroupAssignmentResult.GroupNotFound,
            store.prepareAccountAssignment("missing-group-account", "missing", AccountType.CASH),
        )
        assertEquals(
            AccountGroupAssignmentResult.PolicyMismatch,
            store.prepareAccountAssignment("mismatch-account", "cash", AccountType.BANK),
        )
        assertEquals(null, store.groupIdForAccount("missing-group-account"))
        assertEquals(null, store.groupIdForAccount("mismatch-account"))
    }

    @Test
    fun assignmentDoesNotMutateStoreWhenPersistenceFails() {
        val persistence =
            object : AccountGroupPersistence {
                override fun load(): List<AccountGroupEntry>? = DEFAULT_ACCOUNT_GROUPS

                override fun save(groups: List<AccountGroupEntry>): Boolean = true

                override fun saveAccountGroupIds(accountGroupIds: Map<String, String>): Boolean = false
            }
        val store = AccountGroupStore(persistence = persistence)

        assertEquals(
            AccountGroupAssignmentResult.PersistenceFailure,
            store.prepareAccountAssignment("account-1", "cash", AccountType.CASH),
        )
        assertEquals(null, store.groupIdForAccount("account-1"))
    }

    @Test
    fun addGroupFailsWhenPersistenceFailsAndDoesNotMutateStore() {
        val persistence = InMemoryAccountGroupPersistence(saveShouldSucceed = false)
        val store = AccountGroupStore(persistence = persistence)

        val result = store.addGroup("Failed Group", AccountGroupPolicy.BANK)
        assertNull(result)
        assertEquals(3, store.groups.value.size)
        assertFalse(store.groups.value.any { it.title == "Failed Group" })
    }

    @Test
    fun updateGroupFailsWhenPersistenceFailsAndDoesNotMutateStore() {
        val persistence = InMemoryAccountGroupPersistence(saveShouldSucceed = true)
        val store = AccountGroupStore(persistence = persistence)
        val custom = store.addGroup("Initial", AccountGroupPolicy.BANK)
        assertNotNull(custom)

        persistence.saveShouldSucceed = false
        val updated = store.updateGroup(custom.id, "Renamed")
        assertFalse(updated)
        assertEquals("Initial", store.groups.value.first { it.id == custom.id }.title)
    }

    @Test
    fun removeGroupFailsWhenPersistenceFailsAndDoesNotMutateStore() {
        val persistence = InMemoryAccountGroupPersistence(saveShouldSucceed = true)
        val store = AccountGroupStore(persistence = persistence)
        val custom = store.addGroup("Temporary", AccountGroupPolicy.BANK)
        assertNotNull(custom)
        assertEquals(4, store.groups.value.size)

        persistence.saveShouldSucceed = false
        val result = store.removeGroup(custom.id)
        assertEquals(AccountGroupRemovalResult.PersistenceFailure, result)
        assertEquals(4, store.groups.value.size)
    }

    @Test
    fun removeGroupCleansUpOrphanedAssignmentsFromInterruptedCreation() {
        val repository =
            object : AccountRepository {
                override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> =
                    LedgerResult.Success(emptyList()) // account-orphan does NOT exist in database!

                override fun findById(accountId: String): LedgerResult<Account> =
                    LedgerResult.Failure(LedgerError.AccountNotFound(accountId))

                override fun create(account: Account): LedgerResult<Account> = LedgerResult.Success(account)

                override fun update(account: Account): LedgerResult<Account> = LedgerResult.Success(account)
            }
        val store = AccountGroupStore(accountRepository = repository)
        val custom = store.addGroup("Custom Group", AccountGroupPolicy.BANK)
        assertNotNull(custom)
        store.assignAccount("account-orphan", custom.id)

        // Since account-orphan does not exist in repository, it is treated as an orphaned assignment and recovered
        val result = store.removeGroup(custom.id)
        assertEquals(AccountGroupRemovalResult.Removed, result)
        assertNull(store.groupIdForAccount("account-orphan"))
    }

    @Test
    fun removeGroupPreservesAssignmentsBelongingToArchivedAccounts() {
        val archivedAccount =
            Account(
                id = "archived-1",
                name = "Old Account",
                type = AccountType.BANK,
                currencyCode = "USD",
                isArchived = true,
                createdAt = 1000L,
                updatedAt = 2000L,
            )
        val repository =
            object : AccountRepository {
                override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> = LedgerResult.Success(listOf(archivedAccount))

                override fun findById(accountId: String): LedgerResult<Account> = LedgerResult.Success(archivedAccount)

                override fun create(account: Account): LedgerResult<Account> = LedgerResult.Success(account)

                override fun update(account: Account): LedgerResult<Account> = LedgerResult.Success(account)
            }
        val store = AccountGroupStore(accountRepository = repository)
        val custom = store.addGroup("Archived Holding Group", AccountGroupPolicy.BANK)
        assertNotNull(custom)
        store.assignAccount("archived-1", custom.id)

        // Valid assignment belonging to an archived account must NEVER be deleted and prevents group removal
        val result = store.removeGroup(custom.id)
        assertEquals(AccountGroupRemovalResult.GroupContainsAccounts, result)
        assertEquals(custom.id, store.groupIdForAccount("archived-1"))
    }

    @Test
    fun removeGroupDoesNotDeleteGroupWhenRepositoryReadFails() {
        val repository =
            object : AccountRepository {
                override fun findAll(includeArchived: Boolean): LedgerResult<List<Account>> =
                    LedgerResult.Failure(LedgerError.Unknown("disk read error"))

                override fun findById(accountId: String): LedgerResult<Account> =
                    LedgerResult.Failure(LedgerError.Unknown("disk read error"))

                override fun create(account: Account): LedgerResult<Account> = LedgerResult.Success(account)

                override fun update(account: Account): LedgerResult<Account> = LedgerResult.Success(account)
            }
        val store = AccountGroupStore(accountRepository = repository)
        val custom = store.addGroup("Suspicious Group", AccountGroupPolicy.BANK)
        assertNotNull(custom)
        store.assignAccount("account-unknown", custom.id)

        // Repository read failed; conservative preservation: do not delete group or assignment
        val result = store.removeGroup(custom.id)
        assertEquals(AccountGroupRemovalResult.GroupContainsAccounts, result)
        assertEquals(custom.id, store.groupIdForAccount("account-unknown"))
    }
}
