package com.tioledger.ui.di

import com.tioledger.ui.accounts.AccountsViewModel
import com.tioledger.ui.budgets.BudgetsViewModel
import com.tioledger.ui.categories.CategoriesViewModel
import com.tioledger.ui.loans.LoanDetailsViewModel
import com.tioledger.ui.loans.LoansViewModel
import com.tioledger.ui.more.AccountGroupPersistence
import com.tioledger.ui.more.AccountGroupStore
import com.tioledger.ui.more.CurrencyPreferenceStore
import com.tioledger.ui.more.CurrencySettingsViewModel
import com.tioledger.ui.more.InMemoryAccountGroupPersistence
import com.tioledger.ui.reports.ReportsViewModel
import com.tioledger.ui.sms.SmsTransactionReviewViewModel
import com.tioledger.ui.transactions.TransactionEntryViewModel
import com.tioledger.ui.transactions.TransactionsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun tioUiModule(): Module =
    module {
        single<AccountGroupPersistence> { InMemoryAccountGroupPersistence() }
        single { AccountGroupStore(persistence = get(), accountRepository = getOrNull()) }
        single { CurrencyPreferenceStore(get(), get()) }
        factory { CurrencySettingsViewModel(get()) }
        factory { AccountsViewModel(get(), get(), get(), get(), accountGroupStore = get(), currencyPreferenceStore = get()) }
        factory { BudgetsViewModel(get(), get(), get(), get(), get()) }
        factory { CategoriesViewModel(get(), get(), get()) }
        factory { LoansViewModel(get(), get(), get(), get()) }
        factory { LoanDetailsViewModel(get(), get()) }
        factory { ReportsViewModel(get()) }
        factory { TransactionEntryViewModel(get(), get(), get(), get(), get()) }
        factory {
            SmsTransactionReviewViewModel(
                prepareUseCase = get(),
                confirmUseCase = get(),
            )
        }
        factory { TransactionsViewModel(get(), get()) }
    }
