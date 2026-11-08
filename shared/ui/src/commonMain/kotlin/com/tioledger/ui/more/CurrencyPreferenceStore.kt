package com.tioledger.ui.more

import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.currency.GetMainCurrencyConfigUseCase
import com.tioledger.application.usecase.currency.SaveMainCurrencyConfigUseCase
import com.tioledger.domain.model.MainCurrencyConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrencyPreferenceStore(
    private val getMainCurrencyConfigUseCase: GetMainCurrencyConfigUseCase,
    private val saveMainCurrencyConfigUseCase: SaveMainCurrencyConfigUseCase,
) {
    private val _config = MutableStateFlow(getMainCurrencyConfigUseCase())
    val config: StateFlow<MainCurrencyConfig> = _config.asStateFlow()

    fun getConfig(): MainCurrencyConfig = _config.value

    fun saveConfig(newConfig: MainCurrencyConfig): Boolean {
        val result = saveMainCurrencyConfigUseCase(newConfig)
        return if (result is ApplicationResult.Success) {
            _config.value = newConfig
            true
        } else {
            false
        }
    }

    fun reload() {
        _config.value = getMainCurrencyConfigUseCase()
    }
}
