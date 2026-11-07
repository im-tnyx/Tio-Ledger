package com.tioledger.domain.repository

import com.tioledger.domain.model.MainCurrencyConfig

interface CurrencyPreferenceRepository {
    fun getConfig(): MainCurrencyConfig

    fun saveConfig(config: MainCurrencyConfig): Boolean
}
