package com.tioledger.data.mapper

import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.Money
import com.tioledger.database.query.SelectAllLoans
import com.tioledger.database.query.SelectLoanById
import com.tioledger.database.query.SelectLoanInstallments
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanCompoundingFrequency
import com.tioledger.domain.model.LoanEmiCalculationMethod
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanInterestType
import com.tioledger.domain.model.LoanPaymentFrequency
import com.tioledger.domain.model.LoanStatus

fun SelectAllLoans.toDomain(): Loan =
    Loan(
        id = id,
        name = name,
        principal = Money(principal, CurrencyCode(currency_code)),
        annualInterestRateBasisPoints = interest_rate_basis_points.toInt(),
        interestType = LoanInterestType.valueOf(interest_type),
        emiCalculationMethod = LoanEmiCalculationMethod.valueOf(emi_calculation_method),
        compoundingFrequency = LoanCompoundingFrequency.valueOf(compounding_frequency),
        paymentFrequency = LoanPaymentFrequency.valueOf(payment_frequency),
        tenureMonths = tenure_months.toInt(),
        startDate = start_date,
        accountId = account_id,
        disbursedAccountId = disbursed_account_id,
        processingFee = Money(processing_fee, CurrencyCode(currency_code)),
        insuranceAmount = Money(insurance_amount, CurrencyCode(currency_code)),
        lateFeePolicy = late_fee_policy,
        gracePeriodDays = grace_period_days.toInt(),
        moratoriumMonths = moratorium_months.toInt(),
        status = LoanStatus.valueOf(status),
        createdAt = created_at,
        updatedAt = updated_at,
        entityVersion = entity_version.toInt(),
        syncVersion = sync_version.toInt(),
        deviceId = device_id,
        deletedAt = deleted_at,
    )

fun SelectLoanById.toDomain(): Loan =
    Loan(
        id = id,
        name = name,
        principal = Money(principal, CurrencyCode(currency_code)),
        annualInterestRateBasisPoints = interest_rate_basis_points.toInt(),
        interestType = LoanInterestType.valueOf(interest_type),
        emiCalculationMethod = LoanEmiCalculationMethod.valueOf(emi_calculation_method),
        compoundingFrequency = LoanCompoundingFrequency.valueOf(compounding_frequency),
        paymentFrequency = LoanPaymentFrequency.valueOf(payment_frequency),
        tenureMonths = tenure_months.toInt(),
        startDate = start_date,
        accountId = account_id,
        disbursedAccountId = disbursed_account_id,
        processingFee = Money(processing_fee, CurrencyCode(currency_code)),
        insuranceAmount = Money(insurance_amount, CurrencyCode(currency_code)),
        lateFeePolicy = late_fee_policy,
        gracePeriodDays = grace_period_days.toInt(),
        moratoriumMonths = moratorium_months.toInt(),
        status = LoanStatus.valueOf(status),
        createdAt = created_at,
        updatedAt = updated_at,
        entityVersion = entity_version.toInt(),
        syncVersion = sync_version.toInt(),
        deviceId = device_id,
        deletedAt = deleted_at,
    )

fun SelectLoanInstallments.toDomain(): LoanInstallment =
    LoanInstallment(
        id = id,
        loanId = loan_id,
        installmentNumber = installment_number.toInt(),
        dueDate = due_date,
        openingBalance = Money(opening_balance, CurrencyCode(currency_code)),
        payment = Money(emi_amount, CurrencyCode(currency_code)),
        principalComponent = Money(principal_component, CurrencyCode(currency_code)),
        interestComponent = Money(interest_component, CurrencyCode(currency_code)),
        closingBalance = Money(closing_balance, CurrencyCode(currency_code)),
        status = LoanInstallmentStatus.valueOf(status),
        createdAt = created_at,
        updatedAt = updated_at,
    )
