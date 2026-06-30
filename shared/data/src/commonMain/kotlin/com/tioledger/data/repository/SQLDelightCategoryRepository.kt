package com.tioledger.data.repository

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.data.mapper.toDomain
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Category
import com.tioledger.domain.repository.CategoryRepository

class SQLDelightCategoryRepository(
    private val database: TioLedgerDatabase,
) : CategoryRepository {
    override fun findById(categoryId: String): LedgerResult<Category> {
        val result =
            runDatabaseCatching {
                database.categoriesQueries
                    .selectCategoryById(categoryId)
                    .executeAsOneOrNull()
            }
        return when (result) {
            is com.tioledger.data.result.DataResult.Success -> {
                val category = result.value
                if (category != null) {
                    LedgerResult.Success(category.toDomain())
                } else {
                    LedgerResult.Failure(LedgerError.CategoryNotFound(categoryId))
                }
            }
            is com.tioledger.data.result.DataResult.Failure -> {
                result.toLedgerResult()
            }
        }
    }

    override fun create(category: Category): LedgerResult<Category> {
        val result =
            runDatabaseCatching {
                database.categoriesQueries.insertCategory(
                    id = category.id,
                    name = category.name,
                    type = category.type.name,
                    parent_id = category.parentId,
                    is_default = if (category.isDefault) 1L else 0L,
                    created_at = category.createdAt,
                    updated_at = category.updatedAt,
                    entity_version = category.entityVersion.toLong(),
                    sync_version = category.syncVersion.toLong(),
                    device_id = category.deviceId,
                    deleted_at = category.deletedAt,
                )
                category
            }
        return result.toLedgerResult()
    }

    override fun update(category: Category): LedgerResult<Category> {
        val existingResult =
            runDatabaseCatching {
                database.categoriesQueries
                    .selectCategoryById(category.id)
                    .executeAsOneOrNull()
            }
        if (existingResult is com.tioledger.data.result.DataResult.Success && existingResult.value == null) {
            return LedgerResult.Failure(LedgerError.CategoryNotFound(category.id))
        }

        val result =
            runDatabaseCatching {
                database.categoriesQueries.updateCategory(
                    name = category.name,
                    type = category.type.name,
                    parent_id = category.parentId,
                    is_default = if (category.isDefault) 1L else 0L,
                    updated_at = category.updatedAt,
                    entity_version = category.entityVersion.toLong(),
                    sync_version = category.syncVersion.toLong(),
                    device_id = category.deviceId,
                    deleted_at = category.deletedAt,
                    id = category.id,
                )
                category
            }
        return result.toLedgerResult { LedgerError.CategoryNotFound(category.id) }
    }
}
