package com.tioledger.apps.android

import android.app.Application
import com.tioledger.apps.android.reminders.ReminderNotificationChannels
import com.tioledger.apps.android.reminders.ReminderReconciliationEnqueuer
import com.tioledger.apps.android.reminders.ReminderReconciliationReason
import com.tioledger.apps.android.reminders.androidReminderModule
import com.tioledger.bootstrap.TioApplicationBootstrap
import com.tioledger.bootstrap.database.AndroidDatabaseDriverFactory
import com.tioledger.ui.di.tioUiModule
import org.koin.core.KoinApplication

class TioAndroidApplication : Application() {
    lateinit var koinApplication: KoinApplication
        private set

    override fun onCreate() {
        super.onCreate()
        koinApplication =
            TioApplicationBootstrap(AndroidDatabaseDriverFactory(this))
                .start(
                    extraModules =
                        listOf(
                            tioUiModule(),
                            androidReminderModule(this),
                        ),
                )
        ReminderNotificationChannels.create(this)
        ReminderReconciliationEnqueuer.enqueue(
            context = this,
            reason = ReminderReconciliationReason.APP_START,
        )
    }
}
