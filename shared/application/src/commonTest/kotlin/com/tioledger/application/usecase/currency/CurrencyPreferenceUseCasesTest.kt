package com.tioledger.application.usecase.currency

import com.tioledger.application.model.ApplicationResult
import com.tioledger.domain.model.CurrencyDecimalDisplay
import com.tioledger.domain.model.CurrencyUnitPosition
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.model.SYSTEM_SUPPORTED_CURRENCIES
import com.tioledger.domain.repository.CurrencyPreferenceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class CurrencyPreferenceUseCasesTest {
    private class FakeCurrencyPreferenceRepository(
        var failOnSave: Boolean = false,
        initial: MainCurrencyConfig = MainCurrencyConfig(),
    ) : CurrencyPreferenceRepository {
        private var current: MainCurrencyConfig = initial

        override fun getConfig(): MainCurrencyConfig = current

        override fun saveConfig(config: MainCurrencyConfig): Boolean {
            if (failOnSave) return false
            current = config
            return true
        }
    }

    @Test
    fun getMainCurrencyConfigReturnsCurrentConfig() {
        val repo = FakeCurrencyPreferenceRepository()
        val useCase = GetMainCurrencyConfigUseCase(repo)

        val config = useCase()
        assertEquals("INR", config.currency.code.normalized)
    }

    @Test
    fun saveMainCurrencyConfigSuccessReturnsSuccessResult() {
        val repo = FakeCurrencyPreferenceRepository()
        val saveUseCase = SaveMainCurrencyConfigUseCase(repo)
        val usd = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "USD" }
        val updated =
            MainCurrencyConfig(
                currency = usd,
                unitPosition = CurrencyUnitPosition.END,
                decimalDisplay = CurrencyDecimalDisplay.NONE,
            )

        val result = saveUseCase(updated)
        assertTrue(result.isSuccess())
        assertEquals(updated, repo.getConfig())
    }

    @Test
    fun saveMainCurrencyConfigFailureReturnsValidationFailure() {
        val repo = FakeCurrencyPreferenceRepository(failOnSave = true)
        val saveUseCase = SaveMainCurrencyConfigUseCase(repo)

        val result = saveUseCase(MainCurrencyConfig())
        assertTrue(result.isFailure())
        val failure = assertIs<ApplicationResult.Failure>(result)
        assertEquals("currencyConfig", (failure.error as com.tioledger.application.model.ApplicationError.Validation).field)
    }
}
