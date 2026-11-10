@file:Suppress("FunctionName")

package com.tioledger.ui.accounts

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBackIcon
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.more.AccountGroupStore
import com.tioledger.ui.navigation.MainRoute
import com.tioledger.ui.navigation.TioNavigationGraphs
import com.tioledger.ui.navigation.bottomNavigationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.compose.koinInject

data class ArchivedAccountRowUiModel(
    val id: String,
    val name: String,
    val groupName: String,
    val balance: String,
)

data class ArchivedAccountsUiState(
    val isLoading: Boolean = true,
    val accounts: List<ArchivedAccountRowUiModel> = emptyList(),
    val errorMessage: String? = null,
)

class ArchivedAccountsViewModel(
    private val listAccountSummariesUseCase: ListAccountSummariesUseCase,
    private val accountGroupStore: AccountGroupStore,
) {
    private val mutableState = MutableStateFlow(ArchivedAccountsUiState())
    val uiState: StateFlow<ArchivedAccountsUiState> = mutableState.asStateFlow()

    init {
        load()
    }

    fun load() {
        mutableState.value = mutableState.value.copy(isLoading = true, errorMessage = null)
        when (val result = listAccountSummariesUseCase(includeArchived = true)) {
            is ApplicationResult.Success -> {
                val groupOrder = accountGroupStore.groups.value.mapIndexed { index, group -> group.id to index }.toMap()
                val rows =
                    result.outcome.value.accounts
                        .filter { it.account.isArchived }
                        .map { summary ->
                            val group =
                                accountGroupStore.groupForId(accountGroupStore.groupIdForAccount(summary.account.id).orEmpty())
                            ArchivedAccountRowUiModel(
                                id = summary.account.id,
                                name = summary.account.name,
                                groupName = group?.title ?: summary.account.type.archivedGroupTitle(),
                                balance = summary.balance.toDisplayAmount(),
                            )
                        }
                        .sortedWith(
                            compareBy<ArchivedAccountRowUiModel> {
                                accountGroupStore.groupIdForAccount(it.id)?.let(groupOrder::get) ?: Int.MAX_VALUE
                            }
                                .thenBy { it.name.lowercase() },
                        )
                mutableState.value = ArchivedAccountsUiState(isLoading = false, accounts = rows)
            }
            is ApplicationResult.Failure -> {
                mutableState.value = ArchivedAccountsUiState(isLoading = false, errorMessage = result.error.toArchivedMessage())
            }
        }
    }
}

@Composable
fun ArchivedAccountsRoute(
    viewModel: ArchivedAccountsViewModel = koinInject(),
    onNavigate: (MainRoute) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    ArchivedAccountsScreen(state = state, onRetry = viewModel::load, onNavigate = onNavigate, onNavigateBack = onNavigateBack)
}

@Composable
fun ArchivedAccountsScreen(
    state: ArchivedAccountsUiState,
    onRetry: () -> Unit,
    onNavigate: (MainRoute) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bottomNavigation = TioNavigationGraphs.main.bottomNavigationModel(MainRoute.AccountSettings)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Deleted accounts",
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.semantics { contentDescription = "Navigate back" }) {
                        TioBackIcon()
                    }
                },
            )
        },
        bottomBar = {
            TioBottomNavigation(items = bottomNavigation.items, onItemSelected = { selected ->
                bottomNavigation.navigate(selected, onNavigate)
            })
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> AccountsLoading()
                state.errorMessage != null -> AccountsError(state.errorMessage, onRetry)
                state.accounts.isEmpty() ->
                    TioEmptyState(
                        title = "No deleted accounts",
                        message = "Archived accounts will appear here. Their transaction and ledger history stays preserved.",
                    )
                else ->
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.accounts, key = ArchivedAccountRowUiModel::id) { account ->
                            TioListItem(
                                title = account.name,
                                subtitle = "${account.groupName} · Archived",
                                trailing = {
                                    androidx.compose.material3.Text(
                                        text = account.balance,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                            )
                        }
                    }
            }
        }
    }
}

private fun AccountType.archivedGroupTitle(): String =
    when (this) {
        AccountType.CASH -> "Cash"
        AccountType.BANK -> "Accounts"
        AccountType.CREDIT_CARD -> "Credit Card"
        AccountType.WALLET -> "Others"
        AccountType.LOAN_LINKED -> "Loan"
        AccountType.INVESTMENT -> "Investments"
    }

private fun ApplicationError.toArchivedMessage(): String =
    when (this) {
        is ApplicationError.Validation -> "$field: $reason"
        is ApplicationError.Repository -> "Unable to load deleted accounts."
        is ApplicationError.Ledger -> "Unable to calculate deleted account balances."
    }
