package com.tioledger.domain.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.Account

interface AccountRepository {
    fun findAll(includeArchived: Boolean = false): LedgerResult<List<Account>>

    fun findById(accountId: String): LedgerResult<Account>

    fun create(account: Account): LedgerResult<Account>

    fun update(account: Account): LedgerResult<Account>
}
