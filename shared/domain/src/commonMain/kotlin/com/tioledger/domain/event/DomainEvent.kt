package com.tioledger.domain.event

import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.TransactionType
import kotlinx.serialization.Serializable

@Serializable
sealed interface DomainEvent {
    val occurredAt: Long

    @Serializable
    data class AccountCreated(
        val accountId: String,
        val accountType: AccountType,
        override val occurredAt: Long,
    ) : DomainEvent

    @Serializable
    data class AccountUpdated(
        val accountId: String,
        override val occurredAt: Long,
    ) : DomainEvent

    @Serializable
    data class AccountArchived(
        val accountId: String,
        override val occurredAt: Long,
    ) : DomainEvent

    @Serializable
    data class CategoryCreated(
        val categoryId: String,
        val categoryType: CategoryType,
        override val occurredAt: Long,
    ) : DomainEvent

    @Serializable
    data class CategoryUpdated(
        val categoryId: String,
        override val occurredAt: Long,
    ) : DomainEvent

    @Serializable
    data class CategoryArchived(
        val categoryId: String,
        override val occurredAt: Long,
    ) : DomainEvent

    @Serializable
    data class TransactionRecorded(
        val transactionId: String,
        val transactionType: TransactionType,
        override val occurredAt: Long,
    ) : DomainEvent
}
