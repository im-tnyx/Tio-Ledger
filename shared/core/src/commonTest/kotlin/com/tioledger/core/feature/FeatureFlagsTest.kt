package com.tioledger.core.feature

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FeatureFlagsTest {
    @Test
    fun emptyProviderDisablesExperimentalFeatures() {
        val provider = StaticFeatureFlagProvider()

        assertFalse(provider.isEnabled(FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW))
    }

    @Test
    fun explicitlyEnabledFeatureCanBeResolved() {
        val provider =
            StaticFeatureFlagProvider(
                enabledFlags = setOf(FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW),
            )

        assertTrue(provider.isEnabled(FeatureFlag.SMS_ASSISTED_TRANSACTION_REVIEW))
    }
}
