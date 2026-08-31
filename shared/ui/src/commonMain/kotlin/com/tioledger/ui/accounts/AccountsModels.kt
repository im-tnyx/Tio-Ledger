package com.tioledger.ui.accounts

import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType

data class AccountsUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val summary: AccountsSummaryUiModel = AccountsSummaryUiModel.Empty,
    val groups: List<AccountGroupUiModel> = emptyList(),
    val errorMessage: String? = null,
    val isCreateDialogVisible: Boolean = false,
    val draftName: String = "",
    val draftType: AccountType = AccountType.CASH,
    val draftCurrencyCode: String = SUPPORTED_ACCOUNT_CURRENCY_CODES.first(),
    val isSaving: Boolean = false,
    val createErrorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && groups.all { it.accounts.isEmpty() }
}

// Narrow, deterministic currency/type choices for Account Creation v2 — see account-creation-v2.md.
val SUPPORTED_ACCOUNT_CURRENCY_CODES: List<String> = listOf("USD", "INR", "EUR", "GBP")
val CREATABLE_ACCOUNT_TYPES: List<AccountType> = listOf(AccountType.CASH, AccountType.BANK, AccountType.LOAN_LINKED)

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
    val title: String,
    val total: String,
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

    data class TypeChanged(val type: AccountType) : AccountsAction

    data class CurrencyChanged(val currencyCode: String) : AccountsAction

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
