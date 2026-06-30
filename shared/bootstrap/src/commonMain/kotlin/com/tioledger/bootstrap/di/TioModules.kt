package com.tioledger.bootstrap.di

import com.tioledger.application.usecase.account.ArchiveAccountUseCase
import com.tioledger.application.usecase.account.CreateAccountUseCase
import com.tioledger.application.usecase.account.ListAccountSummariesUseCase
import com.tioledger.application.usecase.account.UpdateAccountUseCase
import com.tioledger.application.usecase.category.ArchiveCategoryUseCase
import com.tioledger.application.usecase.category.CreateCategoryUseCase
import com.tioledger.application.usecase.category.UpdateCategoryUseCase
import com.tioledger.application.usecase.transaction.RecordAdjustmentUseCase
import com.tioledger.application.usecase.transaction.RecordExpenseUseCase
import com.tioledger.application.usecase.transaction.RecordIncomeUseCase
import com.tioledger.application.usecase.transaction.RecordOpeningBalanceUseCase
import com.tioledger.application.usecase.transaction.RecordTransferUseCase
import com.tioledger.bootstrap.database.DatabaseInitializer
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.bootstrap.logging.StartupLogger
import com.tioledger.core.util.IdGenerator
import com.tioledger.core.util.UuidGenerator
import com.tioledger.data.repository.SQLDelightAccountRepository
import com.tioledger.data.repository.SQLDelightCategoryRepository
import com.tioledger.data.repository.SQLDelightLedgerRepository
import com.tioledger.data.repository.SQLDelightTransactionRepository
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.CategoryRepository
import com.tioledger.domain.repository.LedgerRepository
import com.tioledger.domain.repository.TransactionRepository
import com.tioledger.finance.engine.BalanceCalculator
import com.tioledger.finance.engine.PostingEngine
import com.tioledger.finance.engine.PostingStrategyRegistry
import com.tioledger.finance.engine.PostingValidator
import org.koin.core.module.Module
import org.koin.dsl.module

fun coreModule(): Module =
    module {
        single<IdGenerator> { UuidGenerator() }
        single<StartupLogger> { StartupLogger { message -> println(message) } }
    }

fun databaseModule(): Module =
    module {
        single<TioLedgerDatabase> { get<DatabaseInitializer>().initialize() }
    }

fun dataModule(): Module =
    module {
        single<AccountRepository> { SQLDelightAccountRepository(get()) }
        single<CategoryRepository> { SQLDelightCategoryRepository(get()) }
        single<LedgerRepository> { SQLDelightLedgerRepository(get()) }
        single<TransactionRepository> { SQLDelightTransactionRepository(get()) }
    }

fun applicationModule(): Module =
    module {
        factory { CreateAccountUseCase(get()) }
        factory { UpdateAccountUseCase(get()) }
        factory { ArchiveAccountUseCase(get()) }
        factory { ListAccountSummariesUseCase(get(), get(), get()) }
        factory { CreateCategoryUseCase(get()) }
        factory { UpdateCategoryUseCase(get()) }
        factory { ArchiveCategoryUseCase(get()) }
        factory { RecordIncomeUseCase(get(), get(), get(), get()) }
        factory { RecordExpenseUseCase(get(), get(), get(), get()) }
        factory { RecordTransferUseCase(get(), get(), get()) }
        factory { RecordAdjustmentUseCase(get(), get(), get()) }
        factory { RecordOpeningBalanceUseCase(get(), get(), get()) }
    }

fun financeEngineModule(): Module =
    module {
        single { PostingValidator() }
        single { PostingStrategyRegistry() }
        single { BalanceCalculator() }
        single { PostingEngine(idGenerator = get(), validator = get(), strategyRegistry = get()) }
    }

fun diagnosticsModule(): Module =
    module {
        factory {
            StartupDiagnostics(
                koinStarted = true,
                databaseInitialized = getOrNull<TioLedgerDatabase>() != null,
                repositoriesRegistered =
                    getOrNull<AccountRepository>() != null &&
                        getOrNull<CategoryRepository>() != null &&
                        getOrNull<LedgerRepository>() != null &&
                        getOrNull<TransactionRepository>() != null,
                useCasesRegistered =
                    getOrNull<CreateAccountUseCase>() != null &&
                        getOrNull<ListAccountSummariesUseCase>() != null &&
                        getOrNull<CreateCategoryUseCase>() != null &&
                        getOrNull<RecordIncomeUseCase>() != null,
            )
        }
    }

fun tioApplicationModules(): List<Module> =
    listOf(
        coreModule(),
        databaseModule(),
        dataModule(),
        applicationModule(),
        financeEngineModule(),
        diagnosticsModule(),
    )
