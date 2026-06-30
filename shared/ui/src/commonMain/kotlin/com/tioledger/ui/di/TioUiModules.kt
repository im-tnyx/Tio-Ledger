package com.tioledger.ui.di

import com.tioledger.ui.accounts.AccountsViewModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun tioUiModule(): Module =
    module {
        factory { AccountsViewModel(get()) }
    }
