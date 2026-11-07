package com.tioledger.apps.android.currency

import android.content.Context
import android.content.SharedPreferences
import com.tioledger.domain.model.CurrencyDecimalDisplay
import com.tioledger.domain.model.CurrencyUnitPosition
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.model.SYSTEM_SUPPORTED_CURRENCIES
import com.tioledger.domain.repository.CurrencyPreferenceRepository

class SharedPreferencesCurrencyPreferenceRepository(
    private val prefs: SharedPreferences,
) : CurrencyPreferenceRepository {
    constructor(
        context: Context,
        prefsName: String = PREFERENCES_NAME,
    ) : this(
        context.getSharedPreferences(prefsName, Context.MODE_PRIVATE),
    )

    override fun getConfig(): MainCurrencyConfig = loadConfig()

    override fun saveConfig(config: MainCurrencyConfig): Boolean {
        return try {
            prefs.edit()
                .putString(KEY_CURRENCY_CODE, config.currency.code.normalized)
                .putString(KEY_UNIT_POSITION, config.unitPosition.name)
                .putString(KEY_DECIMAL_DISPLAY, config.decimalDisplay.name)
                .commit()
        } catch (_: Exception) {
            false
        }
    }

    private fun loadConfig(): MainCurrencyConfig {
        val savedCode = prefs.getString(KEY_CURRENCY_CODE, null)
        val savedUnitPosition = prefs.getString(KEY_UNIT_POSITION, null)
        val savedDecimalDisplay = prefs.getString(KEY_DECIMAL_DISPLAY, null)

        val currency =
            SYSTEM_SUPPORTED_CURRENCIES.find { it.code.normalized == savedCode?.uppercase() }
                ?: SYSTEM_SUPPORTED_CURRENCIES.first()

        val unitPosition =
            savedUnitPosition?.let { runCatching { CurrencyUnitPosition.valueOf(it) }.getOrNull() }
                ?: CurrencyUnitPosition.FRONT

        val decimalDisplay =
            savedDecimalDisplay?.let { runCatching { CurrencyDecimalDisplay.valueOf(it) }.getOrNull() }
                ?: CurrencyDecimalDisplay.TWO

        return MainCurrencyConfig(
            currency = currency,
            unitPosition = unitPosition,
            decimalDisplay = decimalDisplay,
        )
    }

    companion object {
        const val PREFERENCES_NAME = "tio_currency_preferences"
        const val KEY_CURRENCY_CODE = "main_currency_code"
        const val KEY_UNIT_POSITION = "currency_unit_position"
        const val KEY_DECIMAL_DISPLAY = "currency_decimal_display"
    }
}
