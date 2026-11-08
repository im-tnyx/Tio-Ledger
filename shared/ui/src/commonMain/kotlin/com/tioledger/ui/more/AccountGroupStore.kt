package com.tioledger.ui.more

import com.tioledger.core.util.IdGenerator
import com.tioledger.core.util.UuidGenerator
import com.tioledger.domain.model.AccountType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AccountGroupPolicy(
    val title: String,
    val subtitle: String,
    val accountType: AccountType,
) {
    CASH("Cash", "Cash", AccountType.CASH),
    BANK("Accounts", "Bank", AccountType.BANK),
    CREDIT_CARD("Card", "Card", AccountType.CREDIT_CARD),
    LOAN("Loan", "Loan", AccountType.LOAN_LINKED),
    INVESTMENT("Investments", "Investments", AccountType.INVESTMENT),
    OTHER("Others", "Others", AccountType.WALLET),
}

data class AccountGroupEntry(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val policy: AccountGroupPolicy = AccountGroupPolicy.BANK,
    val isPermanent: Boolean = false,
)

val DEFAULT_ACCOUNT_GROUPS: List<AccountGroupEntry> =
    listOf(
        AccountGroupEntry(
            id = "cash",
            title = "Cash",
            subtitle = null,
            policy = AccountGroupPolicy.CASH,
            isPermanent = true,
        ),
        AccountGroupEntry(
            id = "accounts",
            title = "Accounts",
            subtitle = null,
            policy = AccountGroupPolicy.BANK,
            isPermanent = true,
        ),
        AccountGroupEntry(
            id = "credit_card",
            title = "Credit Card",
            subtitle = "Card",
            policy = AccountGroupPolicy.CREDIT_CARD,
            isPermanent = true,
        ),
    )

class AccountGroupStore(
    private val idGenerator: IdGenerator = UuidGenerator(),
    initialGroups: List<AccountGroupEntry> = DEFAULT_ACCOUNT_GROUPS,
    private val persistence: AccountGroupPersistence = InMemoryAccountGroupPersistence(initialGroups),
) {
    private val _groups = MutableStateFlow(persistence.load() ?: initialGroups)
    val groups: StateFlow<List<AccountGroupEntry>> = _groups.asStateFlow()
    private val accountGroupIds = persistence.loadAccountGroupIds().toMutableMap()

    fun groupForId(id: String): AccountGroupEntry? = _groups.value.firstOrNull { it.id == id }

    fun addGroup(
        title: String,
        policy: AccountGroupPolicy,
    ): AccountGroupEntry {
        val entry =
            AccountGroupEntry(
                id = "group_${idGenerator.nextId()}",
                title = title,
                subtitle = policy.subtitle,
                policy = policy,
                isPermanent = false,
            )
        _groups.value = _groups.value + entry
        persistence.save(_groups.value)
        return entry
    }

    fun updateGroup(
        id: String,
        newTitle: String,
    ): Boolean {
        val current = _groups.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return false
        val updated = current.toMutableList()
        updated[index] = updated[index].copy(title = newTitle)
        _groups.value = updated
        persistence.save(_groups.value)
        return true
    }

    fun removeGroup(id: String): AccountGroupRemovalResult {
        val current = _groups.value
        val target = current.find { it.id == id } ?: return AccountGroupRemovalResult.GroupNotFound
        if (target.isPermanent) return AccountGroupRemovalResult.PermanentGroup
        if (accountGroupIds.any { it.value == id }) return AccountGroupRemovalResult.GroupContainsAccounts
        _groups.value = current.filter { it.id != id }
        persistence.save(_groups.value)
        return AccountGroupRemovalResult.Removed
    }

    fun reorder(
        fromIndex: Int,
        toIndex: Int,
    ) {
        val current = _groups.value.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices && fromIndex != toIndex) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _groups.value = current
            persistence.save(_groups.value)
        }
    }

    fun prepareAccountAssignment(
        accountId: String,
        groupId: String,
        accountType: AccountType,
    ): AccountGroupAssignmentResult {
        val group = groupForId(groupId) ?: return AccountGroupAssignmentResult.GroupNotFound
        if (group.policy.accountType != accountType) return AccountGroupAssignmentResult.PolicyMismatch
        val updatedAssignments = accountGroupIds.toMutableMap().apply { put(accountId, groupId) }
        if (!persistence.saveAccountGroupIds(updatedAssignments)) {
            return AccountGroupAssignmentResult.PersistenceFailure
        }
        accountGroupIds.clear()
        accountGroupIds.putAll(updatedAssignments)
        return AccountGroupAssignmentResult.Prepared(group)
    }

    /** Test/support helper for assigning an already persisted legacy account. */
    fun assignAccount(
        accountId: String,
        groupId: String,
    ): Boolean {
        val group = groupForId(groupId) ?: return false
        return prepareAccountAssignment(accountId, groupId, group.policy.accountType) is AccountGroupAssignmentResult.Prepared
    }

    fun removePreparedAccountAssignment(
        accountId: String,
        groupId: String,
    ): Boolean {
        if (accountGroupIds[accountId] != groupId) return true
        val updatedAssignments = accountGroupIds.toMutableMap().apply { remove(accountId) }
        if (!persistence.saveAccountGroupIds(updatedAssignments)) return false
        accountGroupIds.clear()
        accountGroupIds.putAll(updatedAssignments)
        return true
    }

    fun groupIdForAccount(accountId: String): String? = accountGroupIds[accountId]
}

sealed interface AccountGroupAssignmentResult {
    data class Prepared(val group: AccountGroupEntry) : AccountGroupAssignmentResult

    data object GroupNotFound : AccountGroupAssignmentResult

    data object PolicyMismatch : AccountGroupAssignmentResult

    data object PersistenceFailure : AccountGroupAssignmentResult
}

sealed interface AccountGroupRemovalResult {
    data object Removed : AccountGroupRemovalResult

    data object GroupNotFound : AccountGroupRemovalResult

    data object PermanentGroup : AccountGroupRemovalResult

    data object GroupContainsAccounts : AccountGroupRemovalResult
}

interface AccountGroupPersistence {
    fun load(): List<AccountGroupEntry>?

    fun save(groups: List<AccountGroupEntry>)

    fun loadAccountGroupIds(): Map<String, String> = emptyMap()

    fun saveAccountGroupIds(accountGroupIds: Map<String, String>): Boolean = true
}

class InMemoryAccountGroupPersistence(
    initialGroups: List<AccountGroupEntry>? = null,
) : AccountGroupPersistence {
    private var groups: List<AccountGroupEntry>? = initialGroups

    override fun load(): List<AccountGroupEntry>? = groups

    override fun save(groups: List<AccountGroupEntry>) {
        this.groups = groups.toList()
    }

    private var accountGroupIds: Map<String, String> = emptyMap()

    override fun loadAccountGroupIds(): Map<String, String> = accountGroupIds

    override fun saveAccountGroupIds(accountGroupIds: Map<String, String>): Boolean {
        this.accountGroupIds = accountGroupIds.toMap()
        return true
    }
}
