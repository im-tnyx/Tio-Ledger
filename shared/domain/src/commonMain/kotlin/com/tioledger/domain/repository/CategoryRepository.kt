package com.tioledger.domain.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.Category

interface CategoryRepository {
    fun findById(categoryId: String): LedgerResult<Category>

    fun create(category: Category): LedgerResult<Category>

    fun update(category: Category): LedgerResult<Category>
}
