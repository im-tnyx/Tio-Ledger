package com.tioledger.ui.design

import kotlin.test.Test
import kotlin.test.assertTrue

class TioDesignSystemTest {
    @Test
    fun designTokensExposeStableDimensions() {
        assertTrue(TioSpacing.lg > TioSpacing.sm)
        assertTrue(TioElevation.high > TioElevation.low)
        assertTrue(TioCornerRadius.md > TioCornerRadius.xs)
        assertTrue(TioDimensions.minTouchTarget > TioSpacing.xxl)
        assertTrue(TioMotionDurations.MEDIUM_MILLIS > TioMotionDurations.FAST_MILLIS)
    }

    @Test
    fun iconTokensAreAvailableForCoreProductAreas() {
        assertTrue(TioIconToken.entries.contains(TioIconToken.Account))
        assertTrue(TioIconToken.entries.contains(TioIconToken.Transaction))
        assertTrue(TioIconToken.entries.contains(TioIconToken.Loan))
        assertTrue(TioIconToken.entries.contains(TioIconToken.Calendar))
        assertTrue(TioIconToken.entries.contains(TioIconToken.Analytics))
    }
}
