package com.tioledger.apps.android.currency

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.tioledger.domain.model.CurrencyDecimalDisplay
import com.tioledger.domain.model.CurrencyUnitPosition
import com.tioledger.domain.model.MainCurrencyConfig
import com.tioledger.domain.model.SYSTEM_SUPPORTED_CURRENCIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesCurrencyPreferenceRepositoryTest {
    private class FakeEditor(private val data: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val staged = mutableMapOf<String, Any?>()

        override fun putString(
            key: String,
            value: String?,
        ): SharedPreferences.Editor {
            staged[key] = value
            return this
        }

        override fun putStringSet(
            key: String,
            values: Set<String>?,
        ): SharedPreferences.Editor = this

        override fun putInt(
            key: String,
            value: Int,
        ): SharedPreferences.Editor = this

        override fun putLong(
            key: String,
            value: Long,
        ): SharedPreferences.Editor = this

        override fun putFloat(
            key: String,
            value: Float,
        ): SharedPreferences.Editor = this

        override fun putBoolean(
            key: String,
            value: Boolean,
        ): SharedPreferences.Editor = this

        override fun remove(key: String): SharedPreferences.Editor {
            staged.remove(key)
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            staged.clear()
            return this
        }

        override fun commit(): Boolean {
            data.putAll(staged)
            return true
        }

        override fun apply() {
            commit()
        }
    }

    private class FakeSharedPreferences : SharedPreferences {
        val storage = mutableMapOf<String, Any?>()

        override fun getAll(): Map<String, *> = storage

        override fun getString(
            key: String,
            defValue: String?,
        ): String? = storage[key] as? String ?: defValue

        override fun getStringSet(
            key: String,
            defValues: Set<String>?,
        ): Set<String>? = defValues

        override fun getInt(
            key: String,
            defValue: Int,
        ): Int = defValue

        override fun getLong(
            key: String,
            defValue: Long,
        ): Long = defValue

        override fun getFloat(
            key: String,
            defValue: Float,
        ): Float = defValue

        override fun getBoolean(
            key: String,
            defValue: Boolean,
        ): Boolean = defValue

        override fun contains(key: String): Boolean = storage.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(storage)

        override fun registerOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(listener: OnSharedPreferenceChangeListener?) = Unit
    }

    @Test
    fun defaultInitializationReadsDefaultsWhenEmpty() {
        val sharedPrefs = FakeSharedPreferences()
        val repo = SharedPreferencesCurrencyPreferenceRepository(sharedPrefs)

        val config = repo.getConfig()
        assertEquals("INR", config.currency.code.normalized)
        assertEquals(CurrencyUnitPosition.FRONT, config.unitPosition)
        assertEquals(CurrencyDecimalDisplay.TWO, config.decimalDisplay)
    }

    @Test
    fun saveConfigPersistsValuesAcrossReinitialization() {
        val sharedPrefs = FakeSharedPreferences()
        val repo1 = SharedPreferencesCurrencyPreferenceRepository(sharedPrefs)

        val usd = SYSTEM_SUPPORTED_CURRENCIES.first { it.code.normalized == "USD" }
        val updatedConfig =
            MainCurrencyConfig(
                currency = usd,
                unitPosition = CurrencyUnitPosition.END,
                decimalDisplay = CurrencyDecimalDisplay.FOUR,
            )

        val saved = repo1.saveConfig(updatedConfig)
        assertTrue(saved)
        assertEquals(updatedConfig, repo1.getConfig())

        // Simulate app restart by creating a new repository with the same SharedPreferences
        val repo2 = SharedPreferencesCurrencyPreferenceRepository(sharedPrefs)
        val restoredConfig = repo2.getConfig()

        assertEquals("USD", restoredConfig.currency.code.normalized)
        assertEquals(CurrencyUnitPosition.END, restoredConfig.unitPosition)
        assertEquals(CurrencyDecimalDisplay.FOUR, restoredConfig.decimalDisplay)
    }

    @Test
    fun fallbackGracefullyOnCorruptedData() {
        val sharedPrefs = FakeSharedPreferences()
        sharedPrefs.storage[SharedPreferencesCurrencyPreferenceRepository.KEY_CURRENCY_CODE] = "INVALID"
        sharedPrefs.storage[SharedPreferencesCurrencyPreferenceRepository.KEY_UNIT_POSITION] = "UNKNOWN"
        sharedPrefs.storage[SharedPreferencesCurrencyPreferenceRepository.KEY_DECIMAL_DISPLAY] = "CORRUPT"

        val repo = SharedPreferencesCurrencyPreferenceRepository(sharedPrefs)
        val config = repo.getConfig()

        assertEquals("INR", config.currency.code.normalized)
        assertEquals(CurrencyUnitPosition.FRONT, config.unitPosition)
        assertEquals(CurrencyDecimalDisplay.TWO, config.decimalDisplay)
    }
}
