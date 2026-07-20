package com.tioledger.domain.repository

import com.tioledger.core.model.LedgerResult
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanDetails

interface LoanRepository {
    fun findAll(): LedgerResult<List<Loan>>

    fun findDetails(loanId: String): LedgerResult<LoanDetails>

    fun create(details: LoanDetails): LedgerResult<LoanDetails>
}
