package com.tioledger.data.repository

import com.tioledger.domain.model.CurrencyDecimalDisplay
import com.tioledger.domain.model.CurrencyUnitPosition
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.model.SYSTEM_SUPPORTED_CURRENCIES
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InMemoryCurrencyPreferenceRepositoryTest {
    @Test
    fun defaultInitializationMatchesContract() {
        val repo = InMemoryCurrencyPreferenceRepository()
        val config = repo.getConfig()

        assertEquals("INR", config.currency.code.normalized)
        assertEquals(CurrencyUnitPosition.FRONT, config.unitPosition)
        assertEquals(CurrencyDecimalDisplay.TWO, config.decimalDisplay)
    }

    @Test
    fun saveConfigUpdatesSnapshotAndStateFlow() {
        val repo = InMemoryCurrencyPreferenceRepository()
        val usd = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "USD" }
        val newConfig =
            MainCurrencyConfig(
                currency = usd,
                unitPosition = CurrencyUnitPosition.END,
                decimalDisplay = CurrencyDecimalDisplay.FOUR,
            )

        val success = repo.saveConfig(newConfig)
        assertTrue(success)
        assertEquals(newConfig, repo.getConfig())
    }
}
