package com.tioledger.data.repository

import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.data.mapper.toDomain
import com.tioledger.data.result.DataError
import com.tioledger.data.result.DataResult
import com.tioledger.database.TioLedgerDatabase
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.repository.LoanRepository

class SQLDelightLoanRepository(
    private val database: TioLedgerDatabase,
) : LoanRepository {
    override fun findAll(): LedgerResult<List<Loan>> {
        val result =
            runDatabaseCatching {
                database.loansQueries
                    .selectAllLoans()
                    .executeAsList()
                    .map { it.toDomain() }
            }
        return result.toLedgerResult()
    }

    override fun findDetails(loanId: String): LedgerResult<LoanDetails> {
        val result =
            runDatabaseCatching {
                val loan =
                    database.loansQueries
                        .selectLoanById(loanId)
                        .executeAsOneOrNull()
                        ?: throw IllegalStateException("$LOAN_NOT_FOUND_PREFIX$loanId")
                val schedule =
                    database.loansQueries
                        .selectLoanInstallments(loanId)
                        .executeAsList()
                        .map { it.toDomain() }
                LoanDetails(
                    loan = loan.toDomain(),
                    schedule = schedule,
                )
            }
        return when (result) {
            is DataResult.Success -> LedgerResult.Success(result.value)
            is DataResult.Failure -> {
                if (result.error.message.contains(LOAN_NOT_FOUND_PREFIX)) {
                    LedgerResult.Failure(LedgerError.LoanNotFound(loanId))
                } else {
                    result.toLedgerResult()
                }
            }
        }
    }

    override fun create(details: LoanDetails): LedgerResult<LoanDetails> {
        val loan = details.loan
        val result =
            runDatabaseCatching {
                validateScheduleOwnership(details)
                var duplicate = false
                database.transaction {
                    val existing =
                        database.loansQueries
                            .selectLoanById(loan.id)
                            .executeAsOneOrNull()
                    if (existing != null) {
                        duplicate = true
                        rollback()
                    }

                    insertLoan(loan)
                    details.schedule.forEach(::insertInstallment)
                }
                if (duplicate) {
                    throw IllegalStateException("$DUPLICATE_LOAN_PREFIX${loan.id}")
                }
                details
            }

        return when (result) {
            is DataResult.Success -> LedgerResult.Success(result.value)
            is DataResult.Failure -> {
                if (result.isDuplicateLoanFailure()) {
                    LedgerResult.Failure(LedgerError.DuplicateLoanId(loan.id))
                } else {
                    result.toLedgerResult()
                }
            }
        }
    }

    private fun insertLoan(loan: Loan) {
        database.loansQueries.insertLoan(
            id = loan.id,
            name = loan.name,
            principal = loan.principal.amount,
            interest_rate_basis_points = loan.annualInterestRateBasisPoints.toLong(),
            interest_type = loan.interestType.name,
            emi_calculation_method = loan.emiCalculationMethod.name,
            compounding_frequency = loan.compoundingFrequency.name,
            payment_frequency = loan.paymentFrequency.name,
            tenure_months = loan.tenureMonths.toLong(),
            start_date = loan.startDate,
            account_id = loan.accountId,
            disbursed_account_id = loan.disbursedAccountId,
            processing_fee = loan.processingFee.amount,
            insurance_amount = loan.insuranceAmount.amount,
            late_fee_policy = loan.lateFeePolicy,
            grace_period_days = loan.gracePeriodDays.toLong(),
            moratorium_months = loan.moratoriumMonths.toLong(),
            status = loan.status.name,
            created_at = loan.createdAt,
            updated_at = loan.updatedAt,
            entity_version = loan.entityVersion.toLong(),
            sync_version = loan.syncVersion.toLong(),
            device_id = loan.deviceId,
            deleted_at = loan.deletedAt,
        )
    }

    private fun insertInstallment(installment: LoanInstallment) {
        database.loansQueries.insertLoanInstallment(
            id = installment.id,
            loan_id = installment.loanId,
            installment_number = installment.installmentNumber.toLong(),
            due_date = installment.dueDate,
            opening_balance = installment.openingBalance.amount,
            emi_amount = installment.payment.amount,
            principal_component = installment.principalComponent.amount,
            interest_component = installment.interestComponent.amount,
            closing_balance = installment.closingBalance.amount,
            status = installment.status.name,
            created_at = installment.createdAt,
            updated_at = installment.updatedAt,
        )
    }

    private fun validateScheduleOwnership(details: LoanDetails) {
        if (details.schedule.any { it.loanId != details.loan.id }) {
            throw IllegalArgumentException("CONSTRAINT: loan schedule must reference its owning loan")
        }
    }

    private fun DataResult.Failure.isDuplicateLoanFailure(): Boolean {
        val message = error.message
        return message.contains(DUPLICATE_LOAN_PREFIX) ||
            (error is DataError.ConstraintViolation &&
                message.contains("loans.id", ignoreCase = true))
    }

    private companion object {
        const val LOAN_NOT_FOUND_PREFIX = "LOAN_NOT_FOUND:"
        const val DUPLICATE_LOAN_PREFIX = "DUPLICATE_LOAN:"
    }
}
