package com.tioledger.apps.android

import android.app.Application
import com.tioledger.apps.android.accountgroups.SharedPreferencesAccountGroupPersistence
import com.tioledger.apps.android.currency.SharedPreferencesCurrencyPreferenceRepository
import com.tioledger.apps.android.reminders.ReminderDataChangeDriverFactory
import com.tioledger.apps.android.reminders.ReminderNotificationChannels
import com.tioledger.apps.android.reminders.ReminderReconciliationEnqueuer
import com.tioledger.apps.android.reminders.ReminderReconciliationReason
import com.tioledger.apps.android.reminders.androidReminderModule
import com.tioledger.bootstrap.TioApplicationBootstrap
import com.tioledger.bootstrap.database.AndroidDatabaseDriverFactory
import com.tioledger.domain.repository.CurrencyPreferenceRepository
import com.tioledger.ui.di.tioUiModule
import com.tioledger.ui.more.AccountGroupPersistence
import org.koin.core.KoinApplication
import org.koin.dsl.module

class TioAndroidApplication : Application() {
    lateinit var koinApplication: KoinApplication
        private set

    override fun onCreate() {
        super.onCreate()
        val driverFactory =
            ReminderDataChangeDriverFactory(AndroidDatabaseDriverFactory(this)) {
                ReminderReconciliationEnqueuer.enqueue(
                    context = this,
                    reason = ReminderReconciliationReason.RELEVANT_DATA_CHANGED,
                )
            }
        koinApplication =
            TioApplicationBootstrap(driverFactory)
                .start(
                    extraModules =
                        listOf(
                            tioUiModule(),
                            androidReminderModule(this),
                            module {
                                single<AccountGroupPersistence> {
                                    SharedPreferencesAccountGroupPersistence(this@TioAndroidApplication)
                                }
                                single<CurrencyPreferenceRepository> {
                                    SharedPreferencesCurrencyPreferenceRepository(this@TioAndroidApplication)
                                }
                            },
                        ),
                )
        ReminderNotificationChannels.create(this)
        ReminderReconciliationEnqueuer.enqueue(
            context = this,
            reason = ReminderReconciliationReason.APP_START,
        )
    }
}
