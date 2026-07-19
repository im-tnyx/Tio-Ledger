package com.tioledger.ui.di

import com.tioledger.ui.accounts.AccountsViewModel
import com.tioledger.ui.budgets.BudgetsViewModel
import com.tioledger.ui.categories.CategoriesViewModel
import com.tioledger.ui.transactions.TransactionEntryViewModel
import com.tioledger.ui.transactions.TransactionsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun tioUiModule(): Module =
    module {
        factory { AccountsViewModel(get()) }
        factory { BudgetsViewModel(get(), get(), get(), get(), get()) }
        factory { CategoriesViewModel(get(), get(), get()) }
        factory { TransactionEntryViewModel(get(), get(), get(), get(), get()) }
        factory { TransactionsViewModel(get()) }
    }
