package com.tioledger.domain.model

import com.tioledger.core.model.Money
import kotlinx.serialization.Serializable

@Serializable
enum class LoanInterestType {
    FIXED,
    FLOATING,
    REDUCING,
    FLAT,
}

@Serializable
enum class LoanEmiCalculationMethod {
    REDUCING_BALANCE,
}

@Serializable
enum class LoanCompoundingFrequency {
    MONTHLY,
    QUARTERLY,
    ANNUALLY,
    NONE,
}

@Serializable
enum class LoanPaymentFrequency {
    MONTHLY,
}

@Serializable
enum class LoanStatus {
    ACTIVE,
    CLOSED,
    DRAFT,
}

@Serializable
enum class LoanInstallmentStatus {
    PENDING,
    PAID,
    OVERDUE,
    WAIVED,
    ADJUSTED,
}

@Serializable
data class Loan(
    val id: String,
    val name: String,
    val principal: Money,
    val annualInterestRateBasisPoints: Int,
    val interestType: LoanInterestType,
    val emiCalculationMethod: LoanEmiCalculationMethod,
    val compoundingFrequency: LoanCompoundingFrequency,
    val paymentFrequency: LoanPaymentFrequency,
    val tenureMonths: Int,
    val startDate: Long,
    val accountId: String,
    val disbursedAccountId: String,
    val processingFee: Money,
    val insuranceAmount: Money,
    val lateFeePolicy: String? = null,
    val gracePeriodDays: Int = 0,
    val moratoriumMonths: Int = 0,
    val status: LoanStatus,
    val createdAt: Long,
    val updatedAt: Long,
    val entityVersion: Int = 1,
    val syncVersion: Int = 0,
    val deviceId: String? = null,
    val deletedAt: Long? = null,
)

@Serializable
data class LoanInstallment(
    val id: String,
    val loanId: String,
    val installmentNumber: Int,
    val dueDate: Long,
    val openingBalance: Money,
    val payment: Money,
    val principalComponent: Money,
    val interestComponent: Money,
    val closingBalance: Money,
    val status: LoanInstallmentStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class LoanDetails(
    val loan: Loan,
    val schedule: List<LoanInstallment>,
)
