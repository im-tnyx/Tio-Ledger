package com.tioledger.ui.more

import com.tioledger.domain.model.CurrencyDecimalDisplay
import com.tioledger.domain.model.CurrencyUnitPosition
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.model.SYSTEM_SUPPORTED_CURRENCIES
import com.tioledger.domain.model.SupportedCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CurrencySettingsUiState(
    val selectedCurrency: SupportedCurrency = SYSTEM_SUPPORTED_CURRENCIES.first(),
    val unitPosition: CurrencyUnitPosition = CurrencyUnitPosition.FRONT,
    val decimalDisplay: CurrencyDecimalDisplay = CurrencyDecimalDisplay.TWO,
    val isChangeCurrencyDialogVisible: Boolean = false,
    val isUnitPositionSheetVisible: Boolean = false,
    val isDecimalPointSheetVisible: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val supportedCurrencies: List<SupportedCurrency> = SYSTEM_SUPPORTED_CURRENCIES,
) {
    val headerTitle: String
        get() = "${selectedCurrency.code.normalized} - ${selectedCurrency.countryName} (${selectedCurrency.symbol})"

    val formattedPreview: String
        get() =
            MainCurrencyConfig(
                currency = selectedCurrency,
                unitPosition = unitPosition,
                decimalDisplay = decimalDisplay,
            ).formatSampleAmount()
}

sealed interface CurrencySettingsAction {
    data object OpenChangeCurrency : CurrencySettingsAction

    data object DismissChangeCurrency : CurrencySettingsAction

    data class CurrencySelected(val currency: SupportedCurrency) : CurrencySettingsAction

    data object OpenUnitPositionSheet : CurrencySettingsAction

    data object DismissUnitPositionSheet : CurrencySettingsAction

    data class UnitPositionSelected(val position: CurrencyUnitPosition) : CurrencySettingsAction

    data object OpenDecimalPointSheet : CurrencySettingsAction

    data object DismissDecimalPointSheet : CurrencySettingsAction

    data class DecimalPointSelected(val decimal: CurrencyDecimalDisplay) : CurrencySettingsAction

    data object Save : CurrencySettingsAction

    data object ResetDraft : CurrencySettingsAction
}

class CurrencySettingsViewModel(
    private val currencyPreferenceStore: CurrencyPreferenceStore,
) {
    private val _uiState = MutableStateFlow(createInitialState())
    val uiState: StateFlow<CurrencySettingsUiState> = _uiState.asStateFlow()

    private fun createInitialState(): CurrencySettingsUiState {
        val config = currencyPreferenceStore.getConfig()
        return CurrencySettingsUiState(
            selectedCurrency = config.currency,
            unitPosition = config.unitPosition,
            decimalDisplay = config.decimalDisplay,
        )
    }

    fun onAction(
        action: CurrencySettingsAction,
        onSaveSuccess: () -> Unit = {},
    ) {
        when (action) {
            CurrencySettingsAction.OpenChangeCurrency ->
                _uiState.update { it.copy(isChangeCurrencyDialogVisible = true) }

            CurrencySettingsAction.DismissChangeCurrency ->
                _uiState.update { it.copy(isChangeCurrencyDialogVisible = false) }

            is CurrencySettingsAction.CurrencySelected ->
                _uiState.update {
                    it.copy(
                        selectedCurrency = action.currency,
                        isChangeCurrencyDialogVisible = false,
                    )
                }

            CurrencySettingsAction.OpenUnitPositionSheet ->
                _uiState.update { it.copy(isUnitPositionSheetVisible = true) }

            CurrencySettingsAction.DismissUnitPositionSheet ->
                _uiState.update { it.copy(isUnitPositionSheetVisible = false) }

            is CurrencySettingsAction.UnitPositionSelected ->
                _uiState.update {
                    it.copy(
                        unitPosition = action.position,
                        isUnitPositionSheetVisible = false,
                    )
                }

            CurrencySettingsAction.OpenDecimalPointSheet ->
                _uiState.update { it.copy(isDecimalPointSheetVisible = true) }

            CurrencySettingsAction.DismissDecimalPointSheet ->
                _uiState.update { it.copy(isDecimalPointSheetVisible = false) }

            is CurrencySettingsAction.DecimalPointSelected ->
                _uiState.update {
                    it.copy(
                        decimalDisplay = action.decimal,
                        isDecimalPointSheetVisible = false,
                    )
                }

            CurrencySettingsAction.ResetDraft ->
                _uiState.value = createInitialState()

            CurrencySettingsAction.Save -> {
                val current = _uiState.value
                if (current.isSaving) return

                _uiState.update { it.copy(isSaving = true, errorMessage = null) }
                val newConfig =
                    MainCurrencyConfig(
                        currency = current.selectedCurrency,
                        unitPosition = current.unitPosition,
                        decimalDisplay = current.decimalDisplay,
                    )
                val success = currencyPreferenceStore.saveConfig(newConfig)
                if (success) {
                    _uiState.update { it.copy(isSaving = false) }
                    onSaveSuccess()
                } else {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Failed to save currency configuration",
                        )
                    }
                }
            }
        }
    }
}
