package com.tioledger.ui.di

import com.tioledger.ui.accounts.AccountsViewModel
import com.tioledger.ui.transactions.TransactionEntryViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun tioUiModule(): Module =
    module {
        factory { AccountsViewModel(get()) }
        factory { TransactionEntryViewModel() }
    }
