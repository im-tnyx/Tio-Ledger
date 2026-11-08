package com.tioledger.domain.model

import com.tioledger.core.model.CurrencyCode
import kotlinx.serialization.Serializable

@Serializable
enum class CurrencyUnitPosition {
    FRONT,
    END,
    ;

    val displayName: String
        get() =
            when (this) {
                FRONT -> "Front"
                END -> "End"
            }
}

@Serializable
enum class CurrencyDecimalDisplay(val places: Int, val label: String) {
    NONE(0, "None"),
    ONE(1, "1.0"),
    TWO(2, "1.00"),
    THREE(3, "1.000"),
    FOUR(4, "1.0000"),
    FIVE(5, "1.00000"),
    EIGHT(8, "1.00000000"),
}

@Serializable
data class SupportedCurrency(
    val code: CurrencyCode,
    val countryName: String,
    val symbol: String,
) {
    val displayName: String
        get() = "${code.normalized} - $countryName ($symbol)"
}

val SYSTEM_SUPPORTED_CURRENCIES: List<SupportedCurrency> =
    listOf(
        SupportedCurrency(CurrencyCode("INR"), "India", "₹"),
        SupportedCurrency(CurrencyCode("USD"), "United States", "$"),
        SupportedCurrency(CurrencyCode("EUR"), "European Union", "€"),
        SupportedCurrency(CurrencyCode("GBP"), "United Kingdom", "£"),
    )

@Serializable
data class MainCurrencyConfig(
    val currency: SupportedCurrency = SYSTEM_SUPPORTED_CURRENCIES.first(),
    val unitPosition: CurrencyUnitPosition = CurrencyUnitPosition.FRONT,
    val decimalDisplay: CurrencyDecimalDisplay = CurrencyDecimalDisplay.TWO,
) {
    fun formatSampleAmount(): String {
        val numberPart =
            when (decimalDisplay) {
                CurrencyDecimalDisplay.NONE -> "1"
                CurrencyDecimalDisplay.ONE -> "1.0"
                CurrencyDecimalDisplay.TWO -> "1.00"
                CurrencyDecimalDisplay.THREE -> "1.000"
                CurrencyDecimalDisplay.FOUR -> "1.0000"
                CurrencyDecimalDisplay.FIVE -> "1.00000"
                CurrencyDecimalDisplay.EIGHT -> "1.00000000"
            }
        return when (unitPosition) {
            CurrencyUnitPosition.FRONT -> "${currency.symbol} $numberPart"
            CurrencyUnitPosition.END -> "$numberPart ${currency.symbol}"
        }
    }

    val subtitleLabel: String
        get() = "${currency.code.normalized}(${currency.symbol})"
}
