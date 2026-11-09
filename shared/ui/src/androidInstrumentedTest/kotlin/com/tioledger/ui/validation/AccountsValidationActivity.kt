package com.tioledger.ui.validation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.accounts.AccountGroupUiModel
import com.tioledger.ui.accounts.AccountRowUiModel
import com.tioledger.ui.accounts.AccountsPreviewData
import com.tioledger.ui.accounts.AccountsScreen
import com.tioledger.ui.accounts.AccountsSummaryUiModel
import com.tioledger.ui.accounts.AccountsUiState
import com.tioledger.ui.design.TioLedgerTheme
import com.tioledger.ui.navigation.MainRoute

/** Test APK only: display state is never routed through Application/DB/reminder code. */
class AccountsValidationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showScenario(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        showScenario(intent)
    }

    private fun showScenario(intent: Intent) {
        val state =
            when (intent.getStringExtra("scenario")) {
                "zero" -> AccountsPreviewData.mixedCurrencyZero
                "large-signed" -> AccountsPreviewData.mixedCurrencyLargeSigned
                "loan-shortcut-fresh" -> AccountsPreviewData.empty
                "loan-shortcut-all-linked" -> loanShortcutWithLinkedAccounts
                else -> AccountsPreviewData.mixedCurrency
            }
        val darkTheme = intent.getBooleanExtra("darkTheme", false)
        val width = if (intent.getIntExtra("widthDp", 360) == 320) 320.dp else 360.dp
        setContent {
            TioLedgerTheme(darkTheme = darkTheme) {
                var navigationTarget by remember { mutableStateOf<MainRoute?>(null) }
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(modifier = Modifier.width(width).fillMaxHeight().align(Alignment.TopCenter)) {
                        AccountsScreen(state = state, onAction = {}, onNavigate = { navigationTarget = it })
                        navigationTarget?.let { target ->
                            Text("Selected route: ${target.title}", modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }
}

private val loanShortcutWithLinkedAccounts =
    AccountsUiState(
        isLoading = false,
        summary = AccountsSummaryUiModel("INR 0.00", "INR 0.00", "INR 0.00", "INR"),
        groups =
            listOf(
                AccountGroupUiModel(
                    type = AccountType.LOAN_LINKED,
                    title = "Loans",
                    totals = listOf("INR 0.00"),
                    accounts =
                        listOf(
                            AccountRowUiModel(
                                id = "loan-account",
                                name = "Loan account",
                                typeLabel = "Loan",
                                balance = "INR 0.00",
                                currencyCode = "INR",
                                isLiability = true,
                                isLoan = true,
                                loanId = "existing-loan",
                            ),
                        ),
                ),
            ),
    )
