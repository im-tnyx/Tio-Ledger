package com.tioledger.apps.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.ui.shell.TioAppShell

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val diagnostics =
            (application as TioWearApplication)
                .koinApplication
                .koin
                .get<StartupDiagnostics>()

        setContent {
            TioAppShell(
                diagnostics = diagnostics,
                darkTheme = false,
            )
        }
    }
}
