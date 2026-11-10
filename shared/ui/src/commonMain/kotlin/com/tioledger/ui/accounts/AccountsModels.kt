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
    val editingAccountId: String? = null,
    val draftName: String = "",
    val draftCurrentBalance: String = "",
    val draftPaymentAccountId: String? = null,
    val draftDescription: String = "",
    val draftSettlementDate: String = "1",
    val draftPaymentDate: String = "1",
    val availableGroups: List<AccountGroupEntry> = DEFAULT_ACCOUNT_GROUPS,
    val draftGroupId: String = DEFAULT_ACCOUNT_GROUPS.first().id,
    val draftCurrencyCode: String = SUPPORTED_ACCOUNT_CURRENCY_CODES.first(),
    val currencySymbol: String = "₹",
    val isSaving: Boolean = false,
    val saveCompleted: Boolean = false,
    val createErrorMessage: String? = null,
    val archiveAccountId: String? = null,
    val archiveAccountName: String? = null,
    val archiveAccountGroupId: String? = null,
    val isArchiving: Boolean = false,
    val archiveCompleted: Boolean = false,
    val archiveErrorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && errorMessage == null && groups.all { it.accounts.isEmpty() }
}

// Fallback only when Main Currency has not been configured — see account-creation-v2.md.
val SUPPORTED_ACCOUNT_CURRENCY_CODES: List<String> = listOf("INR", "USD", "EUR", "GBP")

data class AccountsSummaryUiModel(
    val assets: String,
    val liabilities: String,
    val total: String,
    val currencyLabel: String,
) {
    companion object {
        val Empty = AccountsSummaryUiModel("0.00", "0.00", "0.00", "")
    }
}

data class AccountGroupUiModel(
    val type: AccountType,
    val id: String = type.name,
    val title: String,
    val totals: List<String>,
    val accounts: List<AccountRowUiModel>,
    val categoryLabel: String = title,
    val accountCount: Int = accounts.size,
)

data class AccountRowUiModel(
    val id: String,
    val name: String,
    val typeLabel: String,
    val balance: String,
    val currencyCode: String,
    val isLiability: Boolean,
    val isLoan: Boolean = false,
    val loanId: String? = null,
    val billingDay: Int? = null,
    val paymentDueDay: Int? = null,
    val accountType: AccountType = AccountType.BANK,
)

sealed interface AccountsAction {
    data object Load : AccountsAction

    data class SearchChanged(val query: String) : AccountsAction

    data object Retry : AccountsAction

    data object AddClicked : AccountsAction

    data class EditAccountRequested(val accountId: String) : AccountsAction

    data object CreateDismissed : AccountsAction

    data class NameChanged(val name: String) : AccountsAction

    data class CurrentBalanceChanged(val amount: String) : AccountsAction

    data class PaymentAccountChanged(val accountId: String?) : AccountsAction

    data class DescriptionChanged(val description: String) : AccountsAction

    data class SettlementDateChanged(val day: String) : AccountsAction

    data class PaymentDateChanged(val day: String) : AccountsAction

    data class GroupChanged(val groupId: String) : AccountsAction

    data object SaveClicked : AccountsAction

    data class ArchiveRequested(
        val accountId: String,
        val accountName: String,
    ) : AccountsAction

    data object ArchiveDismissed : AccountsAction

    data object ArchiveConfirmed : AccountsAction
}

sealed interface AccountsEvent

sealed interface AccountsEffect

internal fun currencySymbolFor(currencyCode: String): String =
    when (currencyCode.uppercase()) {
        "INR" -> "₹"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> currencyCode
    }

internal fun Money.toDecimalAmount(): String {
    val absolute = kotlin.math.abs(amount)
    val major = absolute / MINOR_UNITS
    val minor = absolute % MINOR_UNITS
    val sign = if (amount < 0L) "-" else ""
    return "$sign$major.${minor.toString().padStart(2, '0')}"
}

internal fun Money.toDisplayAmount(overrideSymbol: String? = null): String {
    val absolute = kotlin.math.abs(amount)
    val major = absolute / MINOR_UNITS
    val minor = absolute % MINOR_UNITS
    val sign = if (amount < 0L) "-" else ""
    val sym = overrideSymbol ?: currency.normalized
    return "$sign$sym $major.${minor.toString().padStart(2, '0')}"
}

private const val MINOR_UNITS = 100L
