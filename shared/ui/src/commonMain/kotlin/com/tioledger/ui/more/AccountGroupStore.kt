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
) {
    private val _groups = MutableStateFlow(initialGroups)
    val groups: StateFlow<List<AccountGroupEntry>> = _groups.asStateFlow()

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
        return true
    }

    fun removeGroup(id: String): Boolean {
        val current = _groups.value
        val target = current.find { it.id == id } ?: return false
        if (target.isPermanent) return false
        _groups.value = current.filter { it.id != id }
        return true
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
        }
    }
}
