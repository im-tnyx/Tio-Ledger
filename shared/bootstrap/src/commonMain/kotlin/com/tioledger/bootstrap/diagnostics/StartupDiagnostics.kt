package com.tioledger.bootstrap.diagnostics

data class StartupDiagnostics(
    val koinStarted: Boolean,
    val databaseInitialized: Boolean,
    val repositoriesRegistered: Boolean,
    val useCasesRegistered: Boolean,
)
