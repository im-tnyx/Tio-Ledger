package com.tioledger.domain.model

import com.tioledger.core.model.CurrencyCode
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyModelsTest {
    @Test
    fun defaultConfigHasInrFrontAndTwoDecimals() {
        val config = MainCurrencyConfig()
        assertEquals(CurrencyCode("INR"), config.currency.code)
        assertEquals("India", config.currency.countryName)
        assertEquals("₹", config.currency.symbol)
        assertEquals(CurrencyUnitPosition.FRONT, config.unitPosition)
        assertEquals(CurrencyDecimalDisplay.TWO, config.decimalDisplay)
        assertEquals("₹ 1.00", config.formatSampleAmount())
        assertEquals("INR(₹)", config.subtitleLabel)
    }

    @Test
    fun eachSupportedCurrencyFormatsCorrectly() {
        val inr = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "INR" }
        val usd = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "USD" }
        val eur = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "EUR" }
        val gbp = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "GBP" }

        assertEquals("₹ 1.00", MainCurrencyConfig(currency = inr).formatSampleAmount())
        assertEquals("$ 1.00", MainCurrencyConfig(currency = usd).formatSampleAmount())
        assertEquals("€ 1.00", MainCurrencyConfig(currency = eur).formatSampleAmount())
        assertEquals("£ 1.00", MainCurrencyConfig(currency = gbp).formatSampleAmount())

        assertEquals("INR(₹)", MainCurrencyConfig(currency = inr).subtitleLabel)
        assertEquals("USD($)", MainCurrencyConfig(currency = usd).subtitleLabel)
        assertEquals("EUR(€)", MainCurrencyConfig(currency = eur).subtitleLabel)
        assertEquals("GBP(£)", MainCurrencyConfig(currency = gbp).subtitleLabel)
    }

    @Test
    fun everyDecimalChoiceFormatsCorrectly() {
        val base = MainCurrencyConfig()
        assertEquals("₹ 1", base.copy(decimalDisplay = CurrencyDecimalDisplay.NONE).formatSampleAmount())
        assertEquals("₹ 1.0", base.copy(decimalDisplay = CurrencyDecimalDisplay.ONE).formatSampleAmount())
        assertEquals("₹ 1.00", base.copy(decimalDisplay = CurrencyDecimalDisplay.TWO).formatSampleAmount())
        assertEquals("₹ 1.000", base.copy(decimalDisplay = CurrencyDecimalDisplay.THREE).formatSampleAmount())
        assertEquals("₹ 1.0000", base.copy(decimalDisplay = CurrencyDecimalDisplay.FOUR).formatSampleAmount())
        assertEquals("₹ 1.00000", base.copy(decimalDisplay = CurrencyDecimalDisplay.FIVE).formatSampleAmount())
        assertEquals("₹ 1.00000000", base.copy(decimalDisplay = CurrencyDecimalDisplay.EIGHT).formatSampleAmount())
    }

    @Test
    fun frontAndEndUnitPositionsFormatCorrectly() {
        val front = MainCurrencyConfig(unitPosition = CurrencyUnitPosition.FRONT)
        val end = MainCurrencyConfig(unitPosition = CurrencyUnitPosition.END)

        assertEquals("₹ 1.00", front.formatSampleAmount())
        assertEquals("1.00 ₹", end.formatSampleAmount())
    }
}
