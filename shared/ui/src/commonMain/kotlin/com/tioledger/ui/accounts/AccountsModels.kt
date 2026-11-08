package com.tioledger.ui.accounts

import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.DEFAULT_ACCOUNT_GROUPS

data class AccountsUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val summary: AccountsSummaryUiModel = AccountsSummaryUiModel.Empty,
    val groups: List<AccountGroupUiModel> = emptyList(),
    val errorMessage: String? = null,
    val isCreateDialogVisible: Boolean = false,
    val draftName: String = "",
    val availableGroups: List<AccountGroupEntry> = DEFAULT_ACCOUNT_GROUPS,
    val draftGroupId: String = DEFAULT_ACCOUNT_GROUPS.first().id,
    val draftCurrencyCode: String = SUPPORTED_ACCOUNT_CURRENCY_CODES.first(),
    val isSaving: Boolean = false,
    val createErrorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && groups.all { it.accounts.isEmpty() }
}

// Fallback only when Main Currency has not been configured — see account-creation-v2.md.
val SUPPORTED_ACCOUNT_CURRENCY_CODES: List<String> = listOf("USD", "INR", "EUR", "GBP")

data class AccountsSummaryUiModel(
    val assets: String,
    val liabilities: String,
    val total: String,
    val currencyLabel: String,
) {
    companion object {
        val Empty = AccountsSummaryUiModel("0", "0", "0", "")
    }
}

data class AccountGroupUiModel(
    val type: AccountType,
    val id: String = type.name,
    val title: String,
    val totals: List<String>,
    val accounts: List<AccountRowUiModel>,
)

data class AccountRowUiModel(
    val id: String,
    val name: String,
    val typeLabel: String,
    val balance: String,
    val currencyCode: String,
    val isLiability: Boolean,
)

sealed interface AccountsAction {
    data object Load : AccountsAction

    data class SearchChanged(val query: String) : AccountsAction

    data object Retry : AccountsAction

    data object AddClicked : AccountsAction

    data object CreateDismissed : AccountsAction

    data class NameChanged(val name: String) : AccountsAction

    data class GroupChanged(val groupId: String) : AccountsAction

    data object SaveClicked : AccountsAction
}

sealed interface AccountsEvent

sealed interface AccountsEffect

internal fun Money.toDisplayAmount(): String {
    val absolute = kotlin.math.abs(amount)
    val major = absolute / MINOR_UNITS
    val minor = absolute % MINOR_UNITS
    val sign = if (amount < 0L) "-" else ""
    return "$sign$currency $major.${minor.toString().padStart(2, '0')}"
}

private const val MINOR_UNITS = 100L
