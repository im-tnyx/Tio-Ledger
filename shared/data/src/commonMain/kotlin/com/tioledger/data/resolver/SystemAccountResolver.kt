package com.tioledger.data.resolver

import com.tioledger.domain.model.LedgerClass
import com.tioledger.domain.model.LedgerSourceType
import com.tioledger.domain.model.PostingTarget
import com.tioledger.domain.model.SYSTEM_ADJUSTMENT_ID
import com.tioledger.domain.model.SYSTEM_EXPENSE_ID
import com.tioledger.domain.model.SYSTEM_INCOME_ID
import com.tioledger.domain.model.SYSTEM_OPENING_BALANCE_ID

object SystemAccountResolver {
    fun isSystemAccountId(accountId: String): Boolean {
        return accountId == SYSTEM_INCOME_ID ||
            accountId == SYSTEM_EXPENSE_ID ||
            accountId == SYSTEM_OPENING_BALANCE_ID ||
            accountId == SYSTEM_ADJUSTMENT_ID
    }

    fun getMappedAccountId(
        target: PostingTarget.Virtual,
        sourceType: LedgerSourceType,
    ): String {
        return when (target.ledgerClass) {
            LedgerClass.INCOME -> SYSTEM_INCOME_ID
            LedgerClass.EXPENSE -> SYSTEM_EXPENSE_ID
            else -> {
                if (sourceType == LedgerSourceType.OPENING_BALANCE) {
                    SYSTEM_OPENING_BALANCE_ID
                } else {
                    SYSTEM_ADJUSTMENT_ID
                }
            }
        }
    }

    fun toVirtualTarget(
        accountId: String,
        categoryId: String?,
    ): PostingTarget.Virtual {
        val ledgerClass =
            when (accountId) {
                SYSTEM_INCOME_ID -> LedgerClass.INCOME
                SYSTEM_EXPENSE_ID -> LedgerClass.EXPENSE
                else -> LedgerClass.EQUITY
            }
        return PostingTarget.Virtual(categoryId, ledgerClass)
    }
}
