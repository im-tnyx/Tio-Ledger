package com.tioledger.ui.more

import com.tioledger.core.model.LedgerResult
import com.tioledger.core.util.IdGenerator
import com.tioledger.core.util.UuidGenerator
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.repository.AccountRepository
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
    DEBIT_CARD("Card", "Debit Card", AccountType.BANK),
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

data class AccountBillingDetails(
    val billingDay: Int = 1,
    val paymentDueDay: Int = 1,
    val paymentAccountId: String? = null,
    val description: String = "",
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
    private val accountRepository: AccountRepository? = null,
) {
    private val _groups = MutableStateFlow(persistence.load() ?: initialGroups)
    val groups: StateFlow<List<AccountGroupEntry>> = _groups.asStateFlow()
    private val accountGroupIds = persistence.loadAccountGroupIds().toMutableMap()
    private val accountBillingDetails = persistence.loadAccountBillingDetails().toMutableMap()

    fun groupForId(id: String): AccountGroupEntry? = _groups.value.firstOrNull { it.id == id }

    fun addGroup(
        title: String,
        policy: AccountGroupPolicy,
    ): AccountGroupEntry? {
        val subtitle =
            if (policy == AccountGroupPolicy.BANK || policy == AccountGroupPolicy.CASH) {
                null
            } else {
                policy.subtitle
            }
        val entry =
            AccountGroupEntry(
                id = "group_${idGenerator.nextId()}",
                title = title,
                subtitle = subtitle,
                policy = policy,
                isPermanent = false,
            )
        val updated = _groups.value + entry
        if (!persistence.save(updated)) {
            return null
        }
        _groups.value = updated
        return entry
    }

    fun updateGroup(
        id: String,
        newTitle: String,
        newPolicy: AccountGroupPolicy? = null,
    ): Boolean {
        val current = _groups.value
        val index = current.indexOfFirst { it.id == id }
        if (index == -1) return false
        val existing = current[index]
        if (newPolicy != null && newPolicy != existing.policy && accountGroupIds.values.any { it == id }) {
            return false
        }
        val policy = if (existing.isPermanent) existing.policy else (newPolicy ?: existing.policy)
        val subtitle =
            if (policy == AccountGroupPolicy.BANK || policy == AccountGroupPolicy.CASH) {
                null
            } else {
                policy.subtitle
            }
        val updated = current.toMutableList()
        updated[index] =
            existing.copy(
                title = newTitle,
                policy = policy,
                subtitle = subtitle,
            )
        if (!persistence.save(updated)) {
            return false
        }
        _groups.value = updated
        return true
    }

    fun removeGroup(id: String): AccountGroupRemovalResult {
        val current = _groups.value
        val target = current.find { it.id == id } ?: return AccountGroupRemovalResult.GroupNotFound
        if (target.isPermanent) return AccountGroupRemovalResult.PermanentGroup

        val assignedAccountIds = accountGroupIds.filter { it.value == id }.keys
        if (assignedAccountIds.isNotEmpty()) {
            if (accountRepository != null) {
                when (val result = accountRepository.findAll(includeArchived = true)) {
                    is LedgerResult.Success -> {
                        val existingIds = result.value.map { it.id }.toSet()
                        val validAssigned = assignedAccountIds.filter { it in existingIds }
                        if (validAssigned.isNotEmpty()) {
                            return AccountGroupRemovalResult.GroupContainsAccounts
                        }
                        // Interrupted account creation recovery: clean up orphaned IDs that do not exist in the database.
                        val orphanedIds = assignedAccountIds.filter { it !in existingIds }
                        val updatedAssignments =
                            accountGroupIds.toMutableMap().apply {
                                orphanedIds.forEach { remove(it) }
                            }
                        if (!persistence.saveAccountGroupIds(updatedAssignments)) {
                            return AccountGroupRemovalResult.PersistenceFailure
                        }
                        accountGroupIds.clear()
                        accountGroupIds.putAll(updatedAssignments)
                    }
                    is LedgerResult.Failure -> {
                        // Repository read failed; do not assume accounts do not exist.
                        return AccountGroupRemovalResult.GroupContainsAccounts
                    }
                }
            } else {
                return AccountGroupRemovalResult.GroupContainsAccounts
            }
        }

        val updated = current.filter { it.id != id }
        if (!persistence.save(updated)) {
            return AccountGroupRemovalResult.PersistenceFailure
        }
        _groups.value = updated
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
            if (persistence.save(current)) {
                _groups.value = current
            }
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

    fun billingDetailsForAccount(accountId: String): AccountBillingDetails? = accountBillingDetails[accountId]

    fun saveBillingDetails(
        accountId: String,
        details: AccountBillingDetails,
    ): Boolean {
        if (details.billingDay !in 1..31 || details.paymentDueDay !in 1..31) return false
        val updated = accountBillingDetails.toMutableMap().apply { put(accountId, details) }
        if (!persistence.saveAccountBillingDetails(updated)) return false
        accountBillingDetails.clear()
        accountBillingDetails.putAll(updated)
        return true
    }

    fun removeBillingDetails(accountId: String): Boolean {
        if (accountId !in accountBillingDetails) return true
        val updated = accountBillingDetails.toMutableMap().apply { remove(accountId) }
        if (!persistence.saveAccountBillingDetails(updated)) return false
        accountBillingDetails.clear()
        accountBillingDetails.putAll(updated)
        return true
    }
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

    data object PersistenceFailure : AccountGroupRemovalResult
}

interface AccountGroupPersistence {
    fun load(): List<AccountGroupEntry>?

    fun save(groups: List<AccountGroupEntry>): Boolean

    fun loadAccountGroupIds(): Map<String, String> = emptyMap()

    fun saveAccountGroupIds(accountGroupIds: Map<String, String>): Boolean = true

    fun loadAccountBillingDetails(): Map<String, AccountBillingDetails> = emptyMap()

    fun saveAccountBillingDetails(details: Map<String, AccountBillingDetails>): Boolean = true
}

class InMemoryAccountGroupPersistence(
    initialGroups: List<AccountGroupEntry>? = null,
    var saveShouldSucceed: Boolean = true,
) : AccountGroupPersistence {
    private var groups: List<AccountGroupEntry>? = initialGroups

    override fun load(): List<AccountGroupEntry>? = groups

    override fun save(groups: List<AccountGroupEntry>): Boolean {
        if (!saveShouldSucceed) return false
        this.groups = groups.toList()
        return true
    }

    private var accountGroupIds: Map<String, String> = emptyMap()
    private var accountBillingDetails: Map<String, AccountBillingDetails> = emptyMap()

    override fun loadAccountGroupIds(): Map<String, String> = accountGroupIds

    override fun saveAccountGroupIds(accountGroupIds: Map<String, String>): Boolean {
        if (!saveShouldSucceed) return false
        this.accountGroupIds = accountGroupIds.toMap()
        return true
    }

    override fun loadAccountBillingDetails(): Map<String, AccountBillingDetails> = accountBillingDetails

    override fun saveAccountBillingDetails(details: Map<String, AccountBillingDetails>): Boolean {
        if (!saveShouldSucceed) return false
        accountBillingDetails = details.toMap()
        return true
    }
}
