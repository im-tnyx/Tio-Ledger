package com.tioledger.apps.android.reminders

import android.content.Context
import androidx.work.WorkManager
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidReminderModule(context: Context): Module {
    val applicationContext = context.applicationContext
    return module {
        single<ReminderPlatformStateStore> {
            SharedPreferencesReminderPlatformStateStore(applicationContext)
        }
        single<BudgetReminderReceiptStore> {
            SharedPreferencesBudgetReminderReceiptStore(applicationContext)
        }
        single<ScheduledReminderStore> {
            SharedPreferencesScheduledReminderStore(applicationContext)
        }
        single<AndroidReminderClock> { AndroidReminderClock { System.currentTimeMillis() } }
        single { AndroidNotificationPermissionController(applicationContext, get()) }
        single { ReminderReconciliationPlanner() }
        single { WorkManager.getInstance(applicationContext) }
        single<ReminderWorkOperationApplier> {
            AndroidReminderWorkScheduler(
                workManager = get(),
                scheduledStore = get(),
                clock = get(),
            )
        }
        single { AndroidReminderNotificationPublisher(applicationContext) }
        single {
            AndroidReminderReconciler(
                planRemindersUseCase = get(),
                stateStore = get(),
                receiptStore = get(),
                scheduledStore = get(),
                permissionController = get(),
                reconciliationPlanner = get(),
                workOperationApplier = get(),
                clock = get(),
            )
        }
        single {
            AndroidReminderSettingsService(
                context = applicationContext,
                stateStore = get(),
                permissionController = get(),
            )
        }
    }
}
