package com.tioledger.bootstrap

import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.transaction.RecordIncomeUseCase
import com.tioledger.bootstrap.database.DatabaseInitializer
import com.tioledger.bootstrap.di.tioApplicationModules
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.TransactionRepository
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TioApplicationBootstrapTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun koinGraphResolvesDatabaseRepositoriesEnginesAndUseCases() {
        val app =
            startKoin {
                modules(
                    module {
                        single { DatabaseInitializer(TestDatabaseDriverFactory()) }
                    },
                    *tioApplicationModules().toTypedArray(),
                )
            }

        assertNotNull(app.koin.get<TioLedgerDatabase>())
        assertNotNull(app.koin.get<AccountRepository>())
        assertNotNull(app.koin.get<TransactionRepository>())
        assertNotNull(app.koin.get<CreateAccountUseCase>())
        assertNotNull(app.koin.get<ListAccountSummariesUseCase>())
        assertNotNull(app.koin.get<RecordIncomeUseCase>())

        val diagnostics = app.koin.get<StartupDiagnostics>()
        assertTrue(diagnostics.koinStarted)
        assertTrue(diagnostics.databaseInitialized)
        assertTrue(diagnostics.repositoriesRegistered)
        assertTrue(diagnostics.useCasesRegistered)
    }

    @Test
    fun applicationBootstrapStartsAndReportsDiagnostics() {
        val bootstrap = TioApplicationBootstrap(TestDatabaseDriverFactory())
        val app = bootstrap.start()

        val diagnostics = bootstrap.diagnostics(app)

        assertTrue(diagnostics.koinStarted)
        assertTrue(diagnostics.databaseInitialized)
        assertTrue(diagnostics.repositoriesRegistered)
        assertTrue(diagnostics.useCasesRegistered)
    }
}
