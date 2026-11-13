package com.tioledger.apps.ios

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.window.ComposeUIViewController
import com.tioledger.bootstrap.TioApplicationBootstrap
import com.tioledger.bootstrap.database.IosDatabaseDriverFactory
import com.tioledger.ui.di.tioUiModule
import com.tioledger.ui.shell.TioAppShell
import platform.UIKit.UIViewController

@Suppress("FunctionName")
fun MainViewController(): UIViewController {
    return ComposeUIViewController {
        TioAppShell(
            diagnostics = IosAppBootstrap.diagnostics,
            darkTheme = isSystemInDarkTheme(),
        )
    }
}

private object IosAppBootstrap {
    private val bootstrap = TioApplicationBootstrap(IosDatabaseDriverFactory())
    private val application = bootstrap.start(extraModules = listOf(tioUiModule()))
    val diagnostics = bootstrap.diagnostics(application)
}
