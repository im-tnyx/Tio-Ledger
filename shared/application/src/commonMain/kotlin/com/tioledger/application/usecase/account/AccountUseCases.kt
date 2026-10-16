package com.tioledger.application.usecase.account

import com.tioledger.application.internal.mapRepositoryResult
import com.tioledger.application.internal.normalizedCurrencyCode
import com.tioledger.application.internal.normalizedId
import com.tioledger.application.internal.validateCurrencyCode
import com.tioledger.application.internal.validateId
import com.tioledger.application.internal.validateName
import com.tioledger.application.internal.validateTimestamp
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.event.DomainEvent
import com.tioledger.domain.model.Account
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.repository.AccountRepository
import com.tioledger.domain.repository.LedgerRepository
import com.tioledger.finance.engine.BalanceCalculator

data class AccountBalanceSummary(
    val account: Account,
    val balance: Money,
)

data class AccountCurrencyTotals(
    val currencyCode: String,
    val assets: Money,
    val liabilities: Money,
    val total: Money,
)

data class AccountsBalanceOverview(
    val accounts: List<AccountBalanceSummary>,
    val totals: List<AccountCurrencyTotals>,
    val groups: List<AccountTypeBalanceGroup>,
)

data class AccountTypeBalanceGroup(
    val type: AccountType,
    val accounts: List<AccountBalanceSummary>,
    val totals: List<Money>,
)

/** Reproject a loaded snapshot without reading accounts or ledger history again. */
fun AccountsBalanceOverview.withSearchQuery(searchQuery: String): AccountsBalanceOverview =
    copy(groups = accounts.toTypeGroups(searchQuery))

class ListAccountSummariesUseCase(
    private val accountRepository: AccountRepository,
    private val ledgerRepository: LedgerRepository,
    private val balanceCalculator: BalanceCalculator,
) {
    operator fun invoke(
        includeArchived: Boolean = false,
        searchQuery: String = "",
    ): ApplicationResult<AccountsBalanceOverview> {
        val accounts =
            when (val result = accountRepository.findAll(includeArchived)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }

        val summaries =
            accounts.map { account ->
                val entries =
                    when (val result = ledgerRepository.findEntriesByAccount(account.id)) {
                        is LedgerResult.Success -> result.value
                        is LedgerResult.Failure -> {
                            return ApplicationResult.Failure(ApplicationError.Repository(result.error))
                        }
                    }
                val balance =
                    when (
                        val result =
                            balanceCalculator.calculateBalance(
                                entries = entries,
                                normalBalance = account.type.ledgerClass.normalBalance,
                                currency = CurrencyCode(account.currencyCode),
                            )
                    ) {
                        is LedgerResult.Success -> result.value
                        is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Ledger(result.error))
                    }
                AccountBalanceSummary(account, balance)
            }

        return ApplicationResult.Success(
            UseCaseOutcome(
                value =
                    AccountsBalanceOverview(
                        accounts = summaries,
                        totals = summaries.toCurrencyTotals(),
                        groups = summaries.toTypeGroups(searchQuery),
                    ),
            ),
        )
    }
}

private fun List<AccountBalanceSummary>.toTypeGroups(searchQuery: String): List<AccountTypeBalanceGroup> {
    val visible = filter { searchQuery.isBlank() || it.account.name.contains(searchQuery, ignoreCase = true) }
    return AccountType.entries.mapNotNull { type ->
        val accounts = visible.filter { it.account.type == type }
        if (accounts.isEmpty()) {
            null
        } else {
            AccountTypeBalanceGroup(
                type = type,
                accounts = accounts,
                totals =
                    accounts.toCurrencyTotals().map { currencyTotals ->
                        // A type group contains one ledger class; retain liability balances, not net assets.
                        if (type.ledgerClass == com.tioledger.domain.model.LedgerClass.ASSET) {
                            currencyTotals.assets
                        } else {
                            currencyTotals.liabilities
                        }
                    },
            )
        }
    }
}

private fun List<AccountBalanceSummary>.toCurrencyTotals(): List<AccountCurrencyTotals> {
    return groupBy { it.account.currencyCode }
        .map { (currencyCode, summaries) ->
            val currency = CurrencyCode(currencyCode)
            val zero = Money.zero(currency)
            val assets =
                summaries
                    .filter { it.account.type.ledgerClass == com.tioledger.domain.model.LedgerClass.ASSET }
                    .fold(zero) { total, summary -> total + summary.balance }
            val liabilities =
                summaries
                    .filter { it.account.type.ledgerClass == com.tioledger.domain.model.LedgerClass.LIABILITY }
                    .fold(zero) { total, summary -> total + summary.balance }
            AccountCurrencyTotals(
                currencyCode = currencyCode,
                assets = assets,
                liabilities = liabilities,
                total = assets - liabilities,
            )
        }
        .sortedBy { it.currencyCode }
}

data class CreateAccountCommand(
    val id: String,
    val name: String,
    val type: AccountType,
    val currencyCode: String,
    val displayOrder: Int = 0,
    val createdAt: Long,
    val deviceId: String? = null,
    val updatedBy: String? = null,
)

class CreateAccountUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(command: CreateAccountCommand): ApplicationResult<Account> {
        validateId(command.id, "id")?.let { return ApplicationResult.Failure(it) }
        validateName(command.name)?.let { return ApplicationResult.Failure(it) }
        validateCurrencyCode(command.currencyCode)?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.createdAt, "createdAt")?.let { return ApplicationResult.Failure(it) }

        val account =
            Account(
                id = normalizedId(command.id),
                name = command.name.trim(),
                type = command.type,
                currencyCode = normalizedCurrencyCode(command.currencyCode),
                displayOrder = command.displayOrder,
                createdAt = command.createdAt,
                updatedAt = command.createdAt,
                deviceId = command.deviceId,
                updatedBy = command.updatedBy,
            )

        return accountRepository.create(account).mapRepositoryResult(
            events = { created ->
                listOf(
                    DomainEvent.AccountCreated(
                        accountId = created.id,
                        accountType = created.type,
                        occurredAt = command.createdAt,
                    ),
                )
            },
            transform = { it },
        )
    }
}

data class UpdateAccountCommand(
    val accountId: String,
    val name: String,
    val displayOrder: Int,
    val updatedAt: Long,
    val updatedBy: String? = null,
)

class UpdateAccountUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(command: UpdateAccountCommand): ApplicationResult<Account> {
        validateId(command.accountId, "accountId")?.let { return ApplicationResult.Failure(it) }
        validateName(command.name)?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.updatedAt, "updatedAt")?.let { return ApplicationResult.Failure(it) }

        val accountId = normalizedId(command.accountId)
        val existing =
            when (val result = accountRepository.findById(accountId)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }

        if (existing.isArchived) {
            return ApplicationResult.Failure(ApplicationError.Validation("accountId", "archived account cannot be updated"))
        }

        val updated =
            existing.copy(
                name = command.name.trim(),
                displayOrder = command.displayOrder,
                updatedAt = command.updatedAt,
                updatedBy = command.updatedBy,
            )

        return accountRepository.update(updated).mapRepositoryResult(
            events = { account -> listOf(DomainEvent.AccountUpdated(account.id, command.updatedAt)) },
            transform = { it },
        )
    }
}

data class ArchiveAccountCommand(
    val accountId: String,
    val archivedAt: Long,
    val updatedBy: String? = null,
)

class ArchiveAccountUseCase(private val accountRepository: AccountRepository) {
    operator fun invoke(command: ArchiveAccountCommand): ApplicationResult<Account> {
        validateId(command.accountId, "accountId")?.let { return ApplicationResult.Failure(it) }
        validateTimestamp(command.archivedAt, "archivedAt")?.let { return ApplicationResult.Failure(it) }

        val accountId = normalizedId(command.accountId)
        val existing =
            when (val result = accountRepository.findById(accountId)) {
                is LedgerResult.Success -> result.value
                is LedgerResult.Failure -> return ApplicationResult.Failure(ApplicationError.Repository(result.error))
            }

        if (existing.isArchived) {
            return ApplicationResult.Failure(ApplicationError.Validation("accountId", "account is already archived"))
        }

        val archived =
            existing.copy(
                isArchived = true,
                updatedAt = command.archivedAt,
                updatedBy = command.updatedBy,
            )

        return accountRepository.update(archived).mapRepositoryResult(
            events = { account -> listOf(DomainEvent.AccountArchived(account.id, command.archivedAt)) },
            transform = { it },
        )
    }
}
