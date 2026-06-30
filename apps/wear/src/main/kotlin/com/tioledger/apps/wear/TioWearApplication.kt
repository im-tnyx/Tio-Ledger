package com.tioledger.apps.wear

import android.app.Application
import com.tioledger.bootstrap.TioApplicationBootstrap
import com.tioledger.bootstrap.database.AndroidDatabaseDriverFactory
import com.tioledger.ui.di.tioUiModule
import org.koin.core.KoinApplication

class TioWearApplication : Application() {
    lateinit var koinApplication: KoinApplication
        private set

    override fun onCreate() {
        super.onCreate()
        koinApplication =
            TioApplicationBootstrap(AndroidDatabaseDriverFactory(this))
                .start(extraModules = listOf(tioUiModule()))
    }
}
