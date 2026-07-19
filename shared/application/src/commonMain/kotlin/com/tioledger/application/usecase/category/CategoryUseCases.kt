package com.tioledger.application.usecase.category

import com.tioledger.application.internal.mapRepositoryResult
import com.tioledger.application.internal.normalizedId
import com.tioledger.application.internal.validateId
import com.tioledger.application.internal.validateName
import com.tioledger.application.internal.validateOptionalId
import com.tioledger.application.internal.validateTimestamp
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.event.DomainEvent
import com.tioledger.domain.model.Category
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.repository.CategoryRepository

class ListCategoriesUseCase(private val categoryRepository: CategoryRepository) {
    operator fun invoke(type: CategoryType? = null): ApplicationResult<List<Category>> {
        return when (val result = categoryRepository.findAll()) {
            is LedgerResult.Success -> {
                val categories =
                    result.value
                        .asSequence()
                        .filter { it.deletedAt == null }
                        .filter { category -> type == null || category.type == type }
                        .sortedWith(
                            compareByDescending<Category> { it.isDefault }
                                .thenBy { it.name.lowercase() },
                        )
                        .toList()
                ApplicationResult.Success(UseCaseOutcome(value = categories))
            }
            is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(result.error))
        }
    }
}

data class CreateCategoryCommand(
    val id: String,
    val name: String,
    val type: CategoryType,
    val parentId: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long,
    val deviceId: String? = null,
)

class CreateCategoryUseCase(private val categoryRepository: CategoryRepository) {
    operator fun invoke(command: CreateCategoryCommand): ApplicationResult<Category> {
        validateId(command.id, "id")?.let { return ApplicationResult.Failure(it) }
        validateName(command.name)?.let { return ApplicationResult.Failure(it) }
        validateOptionalId(command.parentId, "parentId")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.createdAt, "createdAt")?.let { return ApplicationResult.Failure(it) }

        val normalizedName = command.name.trim()
        when (val result = categoryRepository.findAll()) {
            is LedgerResult.Success -> {
                val duplicateExists =
                    result.value.any { category ->
                        category.deletedAt == null &&
                            category.type == command.type &&
                            category.name.trim().equals(normalizedName, ignoreCase = true)
                    }
                if (duplicateExists) {
                    return ApplicationResult.Failure(
                        ApplicationError.Validation("name", "category already exists for this type"),
                    )
                }
            }
            is LedgerResult.Failure -> {
                return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }
        }

        val category =
            Category(
                id = normalizedId(command.id),
                name = normalizedName,
                type = command.type,
                parentId = command.parentId?.let(::normalizedId),
                isDefault = command.isDefault,
                createdAt = command.createdAt,
                updatedAt = command.createdAt,
                deviceId = command.deviceId,
            )

        return categoryRepository.create(category).mapRepositoryResult(
            events = { created ->
                listOf(
                    DomainEvent.CategoryCreated(
                        categoryId = created.id,
                        categoryType = created.type,
                        occurredAt = command.createdAt,
                    ),
                )
            },
            transform = { it },
        )
    }
}

data class UpdateCategoryCommand(
    val categoryId: String,
    val name: String,
    val parentId: String? = null,
    val updatedAt: Long,
)

class UpdateCategoryUseCase(private val categoryRepository: CategoryRepository) {
    operator fun invoke(command: UpdateCategoryCommand): ApplicationResult<Category> {
        validateId(command.categoryId, "categoryId")?.let { return ApplicationResult.Failure(it) }
        validateName(command.name)?.let { return ApplicationResult.Failure(it) }
        validateOptionalId(command.parentId, "parentId")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.updatedAt, "updatedAt")?.let { return ApplicationResult.Failure(it) }

        val categoryId = normalizedId(command.categoryId)
        val existing =
            when (val result = categoryRepository.findById(categoryId)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }

        if (existing.deletedAt != null) {
            return ApplicationResult.Failure(ApplicationError.Validation("categoryId", "archived category cannot be updated"))
        }

        val updated =
            existing.copy(
                name = command.name.trim(),
                parentId = command.parentId?.let(::normalizedId),
                updatedAt = command.updatedAt,
            )

        return categoryRepository.update(updated).mapRepositoryResult(
            events = { category -> listOf(DomainEvent.CategoryUpdated(category.id, command.updatedAt)) },
            transform = { it },
        )
    }
}

data class ArchiveCategoryCommand(
    val categoryId: String,
    val archivedAt: Long,
)

class ArchiveCategoryUseCase(private val categoryRepository: CategoryRepository) {
    operator fun invoke(command: ArchiveCategoryCommand): ApplicationResult<Category> {
        validateId(command.categoryId, "categoryId")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.archivedAt, "archivedAt")?.let { return ApplicationResult.Failure(it) }

        val categoryId = normalizedId(command.categoryId)
        val existing =
            when (val result = categoryRepository.findById(categoryId)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }

        if (existing.deletedAt != null) {
            return ApplicationResult.Failure(ApplicationError.Validation("categoryId", "category is already archived"))
        }

        val archived = existing.copy(updatedAt = command.archivedAt, deletedAt = command.archivedAt)

        return categoryRepository.update(archived).mapRepositoryResult(
            events = { category -> listOf(DomainEvent.CategoryArchived(category.id, command.archivedAt)) },
            transform = { it },
        )
    }
}
