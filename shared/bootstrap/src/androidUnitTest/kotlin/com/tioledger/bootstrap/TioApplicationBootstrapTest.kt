package com.tioledger.bootstrap

import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.loan.CreateLoanUseCase
import com.tioledger.application.usecase.loan.GetLoanDetailsUseCase
import com.tioledger.application.usecase.loan.ListLoansUseCase
import com.tioledger.application.usecase.sms.ConfirmSmsTransactionUseCase
import com.tioledger.application.usecase.sms.PrepareSmsTransactionReviewUseCase
import com.tioledger.application.usecase.transaction.RecordIncomeUseCase
import com.tioledger.bootstrap.database.DatabaseInitializer
import com.tioledger.bootstrap.di.tioApplicationModules
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.core.feature.FeatureFlag
import com.tioledger.core.feature.FeatureFlagProvider
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.SmsTransactionParser
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.LoanRepository
import com.tioledger.domain.repository.TransactionRepository
import com.tioledger.loan.engine.LoanCalculator
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
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
        assertNotNull(app.koin.get<LoanRepository>())
        assertNotNull(app.koin.get<TransactionRepository>())
        assertNotNull(app.koin.get<LoanCalculator>())
        assertNotNull(app.koin.get<SmsTransactionParser>())
        assertNotNull(app.koin.get<CreateAccountUseCase>())
        assertNotNull(app.koin.get<ListAccountSummariesUseCase>())
        assertNotNull(app.koin.get<CreateLoanUseCase>())
        assertNotNull(app.koin.get<ListLoansUseCase>())
        assertNotNull(app.koin.get<GetLoanDetailsUseCase>())
        assertNotNull(app.koin.get<RecordIncomeUseCase>())
        assertNotNull(app.koin.get<PrepareSmsTransactionReviewUseCase>())
        assertNotNull(app.koin.get<ConfirmSmsTransactionUseCase>())

        val featureFlags = app.koin.get<FeatureFlagProvider>()
        assertFalse(featureFlags.isEnabled(FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW))

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
