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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tioledger.ui.accounts.AccountsPreviewData
import com.tioledger.ui.accounts.AccountsScreen
import com.tioledger.ui.design.TioLedgerTheme

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
                else -> AccountsPreviewData.mixedCurrency
            }
        val darkTheme = intent.getBooleanExtra("darkTheme", false)
        val width = if (intent.getIntExtra("widthDp", 360) == 320) 320.dp else 360.dp
        setContent {
            TioLedgerTheme(darkTheme = darkTheme) {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    Box(modifier = Modifier.width(width).fillMaxHeight().align(Alignment.TopCenter)) {
                        // Focus/navigation are inspectable; every activation callback is deliberately inert.
                        AccountsScreen(state = state, onAction = {}, onNavigate = {})
                    }
                }
            }
        }
    }
}
