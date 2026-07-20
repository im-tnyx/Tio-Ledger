package com.tioledger.application.usecase.loan

import com.tioledger.application.internal.normalizedId
import com.tioledger.application.internal.validateId
import com.tioledger.application.internal.validateName
import com.tioledger.application.internal.validateTimestamp
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.core.util.IdGenerator
import com.tioledger.domain.event.DomainEvent
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerClass
import com.tioledger.domain.model.Loan
import com.tioledger.domain.model.LoanCompoundingFrequency
import com.tioledger.domain.model.LoanDetails
import com.tioledger.domain.model.LoanEmiCalculationMethod
import com.tioledger.domain.model.LoanInstallment
import com.tioledger.domain.model.LoanInstallmentStatus
import com.tioledger.domain.model.LoanInterestType
import com.tioledger.domain.model.LoanStatus
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.LoanRepository
import com.tioledger.loan.engine.LoanCalculationError
import com.tioledger.loan.engine.LoanCalculationResult
import com.tioledger.loan.engine.LoanCalculator
import com.tioledger.loan.engine.LoanTerms
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import com.tioledger.domain.model.LoanPaymentFrequency as DomainLoanPaymentFrequency
import com.tioledger.loan.engine.LoanPaymentFrequency as EngineLoanPaymentFrequency

/**
 * Application-ready loan summary derived only from persisted loan terms and installment rows.
 */
data class LoanOverview(
    val loan: Loan,
    val scheduledEmi: Money,
    val totalInterest: Money,
    val totalPayable: Money,
    val outstandingPrincipal: Money,
    val remainingInstallments: Int,
    val nextDueDate: Long?,
)

data class LoanDetailsView(
    val overview: LoanOverview,
    val schedule: List<LoanInstallment>,
)

class ListLoansUseCase(
    private val loanRepository: LoanRepository,
) {
    operator fun invoke(): ApplicationResult<List<LoanOverview>> {
        val loans =
            when (val result = loanRepository.findAll()) {
                is LedgerResult.Success -> result.value.filter { it.deletedAt == null }
                is LedgerResult.Failure -> {
                    return ApplicationResult.Failure(ApplicationError.Repository(result.error))
                }
            }

        val overviews = mutableListOf<LoanOverview>()
        for (loan in loans) {
            val details =
                when (val result = loanRepository.findDetails(loan.id)) {
                    is LedgerResult.Success -> result.value
                    is LedgerResult.Failure -> {
                        return ApplicationResult.Failure(ApplicationError.Repository(result.error))
                    }
                }
            when (val result = details.toView()) {
                is ApplicationResult.Success -> overviews += result.outcome.value.overview
                is ApplicationResult.Failure -> return result
            }
        }

        return ApplicationResult.Success(
            UseCaseOutcome(
                value =
                    overviews.sortedWith(
                        compareBy<LoanOverview> { it.loan.name.lowercase() }
                            .thenBy { it.loan.id },
                    ),
            ),
        )
    }
}

class GetLoanDetailsUseCase(
    private val loanRepository: LoanRepository,
) {
    operator fun invoke(loanId: String): ApplicationResult<LoanDetailsView> {
        validateId(loanId, "loanId")?.let { return ApplicationResult.Failure(it) }
        return when (val result = loanRepository.findDetails(normalizedId(loanId))) {
            is LedgerResult.Success -> result.value.toView()
            is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(result.error))
        }
    }
}

data class CreateLoanCommand(
    val id: String,
    val name: String,
    val principalAmount: Long,
    val annualInterestRateBasisPoints: Int,
    val tenureMonths: Int,
    val startDate: LocalDate,
    val accountId: String,
    val disbursedAccountId: String,
    val createdAt: Long,
    val deviceId: String? = null,
)

class CreateLoanUseCase(
    private val loanRepository: LoanRepository,
    private val accountRepository: AccountRepository,
    private val loanCalculator: LoanCalculator,
    private val idGenerator: IdGenerator,
) {
    operator fun invoke(command: CreateLoanCommand): ApplicationResult<LoanDetailsView> {
        validateInput(command)?.let { return ApplicationResult.Failure(it) }

        val loanAccountId = normalizedId(command.accountId)
        val disbursedAccountId = normalizedId(command.disbursedAccountId)
        if (loanAccountId == disbursedAccountId) {
            return ApplicationResult.Failure(
                ApplicationError.Validation(
                    field = "disbursedAccountId",
                    reason = "must differ from the linked loan account",
                ),
            )
        }

        val loanAccount =
            when (val result = accountRepository.findById(loanAccountId)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> {
                    return ApplicationResult.Failure(ApplicationError.Repository(result.error))
                }
            }
        validateLoanAccount(loanAccount)?.let { return ApplicationResult.Failure(it) }

        val disbursedAccount =
            when (val result = accountRepository.findById(disbursedAccountId)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> {
                    return ApplicationResult.Failure(ApplicationError.Repository(result.error))
                }
            }
        validateDisbursedAccount(disbursedAccount)?.let { return ApplicationResult.Failure(it) }

        val currency = CurrencyCode(loanAccount.currencyCode)
        if (currency != CurrencyCode(disbursedAccount.currencyCode)) {
            return ApplicationResult.Failure(
                ApplicationError.Validation(
                    field = "disbursedAccountId",
                    reason = "must use the same currency as the linked loan account",
                ),
            )
        }

        val quote =
            when (
                val result =
                    loanCalculator.calculate(
                        LoanTerms(
                            principal = Money(command.principalAmount, currency),
                            annualInterestRateBasisPoints = command.annualInterestRateBasisPoints,
                            tenureMonths = command.tenureMonths,
                            startDate = command.startDate,
                            paymentFrequency = EngineLoanPaymentFrequency.MONTHLY,
                        ),
                    )
            ) {
                is LoanCalculationResult.Success -> result.value
                is LoanCalculationResult.Failure -> {
                    return ApplicationResult.Failure(result.error.toApplicationError())
                }
            }

        val loanId = normalizedId(command.id)
        val loan =
            Loan(
                id = loanId,
                name = command.name.trim(),
                principal = quote.terms.principal,
                annualInterestRateBasisPoints = command.annualInterestRateBasisPoints,
                interestType = LoanInterestType.FIXED,
                emiCalculationMethod = LoanEmiCalculationMethod.REDUCING_BALANCE,
                compoundingFrequency = LoanCompoundingFrequency.MONTHLY,
                paymentFrequency = DomainLoanPaymentFrequency.MONTHLY,
                tenureMonths = command.tenureMonths,
                startDate = command.startDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                accountId = loanAccount.id,
                disbursedAccountId = disbursedAccount.id,
                processingFee = Money.zero(currency),
                insuranceAmount = Money.zero(currency),
                status = LoanStatus.ACTIVE,
                createdAt = command.createdAt,
                updatedAt = command.createdAt,
                deviceId = command.deviceId,
            )
        val schedule =
            quote.schedule.map { installment ->
                LoanInstallment(
                    id = idGenerator.nextId(),
                    loanId = loanId,
                    installmentNumber = installment.installmentNumber,
                    dueDate = installment.dueDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                    openingBalance = installment.openingBalance,
                    payment = installment.payment,
                    principalComponent = installment.principalComponent,
                    interestComponent = installment.interestComponent,
                    closingBalance = installment.closingBalance,
                    status = LoanInstallmentStatus.PENDING,
                    createdAt = command.createdAt,
                    updatedAt = command.createdAt,
                )
            }
        val details = LoanDetails(loan = loan, schedule = schedule)

        return when (val result = loanRepository.create(details)) {
            is LedgerResult.Success -> {
                when (val view = result.value.toView()) {
                    is ApplicationResult.Success ->
                        ApplicationResult.Success(
                            UseCaseOutcome(
                                value = view.outcome.value,
                                events =
                                    listOf(
                                        DomainEvent.LoanCreated(
                                            loanId = result.value.loan.id,
                                            occurredAt = command.createdAt,
                                        ),
                                    ),
                            ),
                        )
                    is ApplicationResult.Failure -> view
                }
            }
            is LedgerResult.Failure -> ApplicationResult.Failure(ApplicationError.Repository(result.error))
        }
    }

    private fun validateLoanAccount(account: Account): ApplicationError.Validation? =
        when {
            account.isArchived || account.deletedAt != null ->
                ApplicationError.Validation("accountId", "linked loan account must be active")
            account.type != AccountType.LOAN_LINKED ->
                ApplicationError.Validation("accountId", "must reference a LOAN_LINKED account")
            else -> null
        }

    private fun validateDisbursedAccount(account: Account): ApplicationError.Validation? =
        when {
            account.isArchived || account.deletedAt != null ->
                ApplicationError.Validation("disbursedAccountId", "disbursed account must be active")
            account.type == AccountType.LOAN_LINKED || account.type.ledgerClass != LedgerClass.ASSET ->
                ApplicationError.Validation("disbursedAccountId", "must reference a non-loan asset account")
            else -> null
        }
}

private fun validateInput(command: CreateLoanCommand): ApplicationError.Validation? {
    validateId(command.id, "id")?.let { return it }
    validateName(command.name)?.let { return it }
    if (command.principalAmount <= 0L) {
        return ApplicationError.Validation("principalAmount", "must be greater than zero")
    }
    if (command.annualInterestRateBasisPoints < 0) {
        return ApplicationError.Validation("annualInterestRateBasisPoints", "must be zero or greater")
    }
    if (command.tenureMonths < 1) {
        return ApplicationError.Validation("tenureMonths", "must be at least one month")
    }
    validateId(command.accountId, "accountId")?.let { return it }
    validateId(command.disbursedAccountId, "disbursedAccountId")?.let { return it }
    validateTimestamp(command.createdAt, "createdAt")?.let { return it }
    return null
}

private fun LoanCalculationError.toApplicationError(): ApplicationError =
    when (this) {
        LoanCalculationError.InvalidPrincipal ->
            ApplicationError.Validation("principalAmount", "must be greater than zero")
        LoanCalculationError.NegativeInterestRate ->
            ApplicationError.Validation("annualInterestRateBasisPoints", "must be zero or greater")
        LoanCalculationError.InvalidTenure ->
            ApplicationError.Validation("tenureMonths", "must be at least one month")
        is LoanCalculationError.UnsupportedPaymentFrequency ->
            ApplicationError.Validation("paymentFrequency", "only monthly payments are supported in v1")
        LoanCalculationError.DateOutOfRange ->
            ApplicationError.Validation("startDate", "is outside the supported calendar range")
        LoanCalculationError.ArithmeticOverflow ->
            ApplicationError.Validation("loanTerms", "calculation exceeds the supported numeric range")
        LoanCalculationError.ScheduleDidNotClose ->
            ApplicationError.Ledger(LedgerError.Unknown("loan schedule did not close at zero"))
    }

private fun LoanDetails.toView(): ApplicationResult<LoanDetailsView> {
    val currency = loan.principal.currency
    val mismatchedMoney =
        schedule
            .asSequence()
            .flatMap { installment ->
                sequenceOf(
                    installment.openingBalance,
                    installment.payment,
                    installment.principalComponent,
                    installment.interestComponent,
                    installment.closingBalance,
                )
            }.firstOrNull { it.currency != currency }
    if (mismatchedMoney != null) {
        return ApplicationResult.Failure(
            ApplicationError.Ledger(
                LedgerError.CurrencyMismatch(
                    currency1 = currency.normalized,
                    currency2 = mismatchedMoney.currency.normalized,
                ),
            ),
        )
    }

    return try {
        val zero = Money.zero(currency)
        val remainingSchedule = schedule.filter { it.status != LoanInstallmentStatus.PAID }
        val overview =
            LoanOverview(
                loan = loan,
                scheduledEmi = schedule.firstOrNull()?.payment ?: zero,
                totalInterest = schedule.fold(zero) { total, installment -> total + installment.interestComponent },
                totalPayable = schedule.fold(zero) { total, installment -> total + installment.payment },
                outstandingPrincipal = remainingSchedule.firstOrNull()?.openingBalance ?: zero,
                remainingInstallments = remainingSchedule.size,
                nextDueDate = remainingSchedule.firstOrNull()?.dueDate,
            )
        ApplicationResult.Success(
            UseCaseOutcome(
                value = LoanDetailsView(overview = overview, schedule = schedule),
            ),
        )
    } catch (_: ArithmeticException) {
        ApplicationResult.Failure(
            ApplicationError.Ledger(
                LedgerError.Unknown("loan schedule totals exceed the supported numeric range"),
            ),
        )
    }
}
