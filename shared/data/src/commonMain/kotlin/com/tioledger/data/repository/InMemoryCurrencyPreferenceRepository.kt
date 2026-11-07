package com.tioledger.data.repository

import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.repository.CurrencyPreferenceRepository

class InMemoryCurrencyPreferenceRepository(
    initialConfig: MainCurrencyConfig = MainCurrencyConfig(),
) : CurrencyPreferenceRepository {
    private var currentConfig: MainCurrencyConfig = initialConfig

    override fun getConfig(): MainCurrencyConfig = currentConfig

    override fun saveConfig(config: MainCurrencyConfig): Boolean {
        currentConfig = config
        return true
    }
}
