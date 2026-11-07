package com.tioledger.ui.more

import com.tioledger.application.usecase.currency.GetMainCurrencyConfigUseCase
import com.tioledger.application.usecase.currency.SaveMainCurrencyConfigUseCase
import com.tioledger.domain.model.CurrencyDecimalDisplay
import com.tioledger.domain.model.CurrencyUnitPosition
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.model.SYSTEM_SUPPORTED_CURRENCIES
import com.tioledger.domain.repository.CurrencyPreferenceRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CurrencySettingsViewModelTest {
    private class FakeCurrencyPreferenceRepository(
        var failOnSave: Boolean = false,
        initial: MainCurrencyConfig = MainCurrencyConfig(),
    ) : CurrencyPreferenceRepository {
        val saveCalls = mutableListOf<MainCurrencyConfig>()
        private var current: MainCurrencyConfig = initial

        override fun getConfig(): MainCurrencyConfig = current

        override fun saveConfig(config: MainCurrencyConfig): Boolean {
            saveCalls.add(config)
            if (failOnSave) return false
            current = config
            return true
        }
    }

    private fun createViewModel(repo: FakeCurrencyPreferenceRepository): CurrencySettingsViewModel {
        val store =
            CurrencyPreferenceStore(
                getMainCurrencyConfigUseCase = GetMainCurrencyConfigUseCase(repo),
                saveMainCurrencyConfigUseCase = SaveMainCurrencyConfigUseCase(repo),
            )
        return CurrencySettingsViewModel(store)
    }

    @Test
    fun storeUpdatesStateFlowWhenSaved() {
        val repo = FakeCurrencyPreferenceRepository()
        val store =
            CurrencyPreferenceStore(
                getMainCurrencyConfigUseCase = GetMainCurrencyConfigUseCase(repo),
                saveMainCurrencyConfigUseCase = SaveMainCurrencyConfigUseCase(repo),
            )

        assertEquals("INR", store.config.value.currency.code.normalized)

        val usd = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "USD" }
        val newConfig = MainCurrencyConfig(currency = usd)
        val success = store.saveConfig(newConfig)

        assertTrue(success)
        assertEquals("USD", store.config.value.currency.code.normalized)
        assertEquals("USD", repo.getConfig().currency.code.normalized)
    }

    @Test
    fun initialStateLoadsPersistedConfiguration() {
        val repo = FakeCurrencyPreferenceRepository()
        val viewModel = createViewModel(repo)

        val state = viewModel.uiState.value
        assertEquals("INR", state.selectedCurrency.code.normalized)
        assertEquals(CurrencyUnitPosition.FRONT, state.unitPosition)
        assertEquals(CurrencyDecimalDisplay.TWO, state.decimalDisplay)
        assertEquals("INR - India (₹)", state.headerTitle)
        assertEquals("₹ 1.00", state.formattedPreview)
        assertFalse(state.isSaving)
        assertNull(state.errorMessage)
    }

    @Test
    fun selectingDifferentCurrencyUpdatesDraftAndPreviewWithoutPersisting() {
        val repo = FakeCurrencyPreferenceRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAction(CurrencySettingsAction.OpenChangeCurrency)
        assertTrue(viewModel.uiState.value.isChangeCurrencyDialogVisible)

        val usd = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "USD" }
        viewModel.onAction(CurrencySettingsAction.CurrencySelected(usd))

        val state = viewModel.uiState.value
        assertFalse(state.isChangeCurrencyDialogVisible)
        assertEquals("USD", state.selectedCurrency.code.normalized)
        assertEquals("USD - United States ($)", state.headerTitle)
        assertEquals("$ 1.00", state.formattedPreview)

        // Verifying repo was NOT yet touched (draft only)
        assertEquals(0, repo.saveCalls.size)
        assertEquals("INR", repo.getConfig().currency.code.normalized)
    }

    @Test
    fun selectingUnitPositionAndDecimalPointUpdatesDraftPreview() {
        val repo = FakeCurrencyPreferenceRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAction(CurrencySettingsAction.OpenUnitPositionSheet)
        assertTrue(viewModel.uiState.value.isUnitPositionSheetVisible)
        viewModel.onAction(CurrencySettingsAction.UnitPositionSelected(CurrencyUnitPosition.END))
        assertFalse(viewModel.uiState.value.isUnitPositionSheetVisible)

        viewModel.onAction(CurrencySettingsAction.OpenDecimalPointSheet)
        assertTrue(viewModel.uiState.value.isDecimalPointSheetVisible)
        viewModel.onAction(CurrencySettingsAction.DecimalPointSelected(CurrencyDecimalDisplay.FOUR))
        assertFalse(viewModel.uiState.value.isDecimalPointSheetVisible)

        val state = viewModel.uiState.value
        assertEquals(CurrencyUnitPosition.END, state.unitPosition)
        assertEquals(CurrencyDecimalDisplay.FOUR, state.decimalDisplay)
        assertEquals("1.0000 ₹", state.formattedPreview)
        assertEquals(0, repo.saveCalls.size)
    }

    @Test
    fun resetDraftRestoresPersistedValuesOnDiscard() {
        val repo = FakeCurrencyPreferenceRepository()
        val viewModel = createViewModel(repo)

        val eur = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "EUR" }
        viewModel.onAction(CurrencySettingsAction.CurrencySelected(eur))
        viewModel.onAction(CurrencySettingsAction.UnitPositionSelected(CurrencyUnitPosition.END))
        assertEquals("1.00 €", viewModel.uiState.value.formattedPreview)

        viewModel.onAction(CurrencySettingsAction.ResetDraft)
        val state = viewModel.uiState.value
        assertEquals("INR", state.selectedCurrency.code.normalized)
        assertEquals(CurrencyUnitPosition.FRONT, state.unitPosition)
        assertEquals("₹ 1.00", state.formattedPreview)
    }

    @Test
    fun saveSuccessPersistsConfigurationAtomicallyAndInvokesCallback() {
        val repo = FakeCurrencyPreferenceRepository()
        val viewModel = createViewModel(repo)

        val gbp = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "GBP" }
        viewModel.onAction(CurrencySettingsAction.CurrencySelected(gbp))
        viewModel.onAction(CurrencySettingsAction.UnitPositionSelected(CurrencyUnitPosition.END))
        viewModel.onAction(CurrencySettingsAction.DecimalPointSelected(CurrencyDecimalDisplay.NONE))

        var callbackInvoked = false
        viewModel.onAction(CurrencySettingsAction.Save, onSaveSuccess = { callbackInvoked = true })

        assertTrue(callbackInvoked)
        assertEquals(1, repo.saveCalls.size)
        val persisted = repo.getConfig()
        assertEquals("GBP", persisted.currency.code.normalized)
        assertEquals(CurrencyUnitPosition.END, persisted.unitPosition)
        assertEquals(CurrencyDecimalDisplay.NONE, persisted.decimalDisplay)
        assertEquals("1 £", persisted.formatSampleAmount())
        assertEquals("GBP(£)", persisted.subtitleLabel)
    }

    @Test
    fun saveFailureDisplaysErrorKeepsDraftEditableAndDoesNotReportSuccess() {
        val repo = FakeCurrencyPreferenceRepository(failOnSave = true)
        val viewModel = createViewModel(repo)

        var callbackInvoked = false
        viewModel.onAction(CurrencySettingsAction.Save, onSaveSuccess = { callbackInvoked = true })

        assertFalse(callbackInvoked)
        assertFalse(viewModel.uiState.value.isSaving)
        assertEquals("Failed to save currency configuration", viewModel.uiState.value.errorMessage)
    }
}
