package com.tioledger.application.usecase.currency

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.repository.CurrencyPreferenceRepository

class GetMainCurrencyConfigUseCase(
    private val repository: CurrencyPreferenceRepository,
) {
    operator fun invoke(): MainCurrencyConfig = repository.getConfig()
}

class SaveMainCurrencyConfigUseCase(
    private val repository: CurrencyPreferenceRepository,
) {
    operator fun invoke(config: MainCurrencyConfig): ApplicationResult<Unit> {
        val success = repository.saveConfig(config)
        return if (success) {
            ApplicationResult.Success(UseCaseOutcome(Unit))
        } else {
            ApplicationResult.Failure(
                ApplicationError.Validation("currencyConfig", "Failed to persist currency configuration"),
            )
        }
    }
}
