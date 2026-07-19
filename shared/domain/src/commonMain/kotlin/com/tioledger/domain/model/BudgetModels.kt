package com.tioledger.domain.model

import com.tioledger.core.model.Money
import kotlinx.serialization.Serializable

@Serializable
enum class BudgetPeriodType {
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM,
}

@Serializable
data class Budget(
    val id: String,
    val name: String,
    val amount: Money,
    val categoryId: String? = null,
    val periodType: BudgetPeriodType,
    val createdAt: Long,
    val updatedAt: Long,
    val entityVersion: Int = 1,
    val syncVersion: Int = 0,
    val deviceId: String? = null,
    val deletedAt: Long? = null,
)
