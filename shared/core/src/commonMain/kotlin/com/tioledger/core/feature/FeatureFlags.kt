package com.tioledger.core.feature

/**
 * Typed experimental capabilities that must default to conservative production behavior.
 */
enum class FeatureFlag {
    SMS_ASSISTED_TRANSACTION_REVIEW,
}

fun interface FeatureFlagProvider {
    fun isEnabled(flag: FeatureFlag): Boolean
}

/**
 * Immutable provider used by production bootstrap and focused tests.
 * An empty set keeps every experimental capability disabled.
 */
class StaticFeatureFlagProvider(
    enabledFlags: Set<FeatureFlag> = emptySet(),
) : FeatureFlagProvider {
    private val enabledFlags = enabledFlags.toSet()

    override fun isEnabled(flag: FeatureFlag): Boolean = flag in enabledFlags
}
