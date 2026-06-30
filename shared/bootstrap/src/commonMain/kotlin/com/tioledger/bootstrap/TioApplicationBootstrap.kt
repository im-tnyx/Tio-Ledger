package com.tioledger.bootstrap

import com.tioledger.bootstrap.database.DatabaseDriverFactory
import com.tioledger.bootstrap.database.DatabaseInitializer
import com.tioledger.bootstrap.di.tioApplicationModules
import com.tioledger.bootstrap.diagnostics.StartupDiagnostics
import com.tioledger.bootstrap.logging.StartupLogger
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

class TioApplicationBootstrap(
    private val databaseDriverFactory: DatabaseDriverFactory,
) {
    fun start(extraModules: List<Module> = emptyList()): KoinApplication {
        return startKoin {
            modules(
                module {
                    single { DatabaseInitializer(databaseDriverFactory) }
                },
                *tioApplicationModules().toTypedArray(),
                *extraModules.toTypedArray(),
            )
            koin.get<StartupLogger>().log("Tio Ledger startup initialized")
        }
    }

    fun diagnostics(koinApplication: KoinApplication): StartupDiagnostics {
        return koinApplication.koin.get()
    }
}
