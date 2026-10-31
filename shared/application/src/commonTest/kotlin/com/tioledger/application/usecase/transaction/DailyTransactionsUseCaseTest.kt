package com.tioledger.application.usecase.transaction

import com.tioledger.analytics.SpendingAnalyticsCalculator
import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.usecase.analytics.GetSpendingAnalyticsUseCase
import com.tioledger.application.usecase.analytics.SpendingReportPeriod
import com.tioledger.core.model.CurrencyCode
import com.tioledger.core.model.LedgerError
import com.tioledger.core.model.LedgerResult
import com.tioledger.core.model.Money
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.LedgerEntryType
import com.tioledger.domain.model.TransactionHistoryRecord
import com.tioledger.domain.model.TransactionHistorySplit
import com.tioledger.domain.model.TransactionType
import com.tioledger.domain.repository.TransactionHistoryRepository
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DailyTransactionsUseCaseTest {
    @Test
    fun rejectsPreEpochMonthWithTypedValidationFailure() {
        val result =
            dailyUseCase(emptyList())(
                DailyTransactionsQuery(
                    month = LocalDate(1969, 12, 1),
                    timeZoneId = TimeZone.UTC.id,
                ),
            )

        val failure = assertIs<ApplicationResult.Failure>(result)
        val validation = assertIs<ApplicationError.Validation>(failure.error)
        assertEquals("month", validation.field)
    }

    @Test
    fun returnsHalfOpenLeapYearMonthAndYearRolloverBoundaries() {
        val leapMonth = LocalDate(2028, 2, 1)
        val records =
            listOf(
                record("before", localTimestamp(2028, 1, 31, 23, 59), TransactionType.EXPENSE),
                record("last-day", localTimestamp(2028, 2, 29, 23, 59), TransactionType.EXPENSE),
                record("after", localTimestamp(2028, 3, 1), TransactionType.EXPENSE),
            )
        val useCase = dailyUseCase(records)

        val leapResult = success(useCase(DailyTransactionsQuery(leapMonth, UTC)))
        assertEquals(leapMonth.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), leapResult.startInclusive)
        assertEquals(LocalDate(2028, 3, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), leapResult.endExclusive)
        assertEquals(listOf("last-day"), leapResult.dayGroups.flatMap { it.transactions }.map { it.id })

        val yearEndResult = success(useCase(DailyTransactionsQuery(LocalDate(2028, 12, 1), UTC)))
        assertEquals(LocalDate(2029, 1, 1).atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(), yearEndResult.endExclusive)
    }

    @Test
    fun usesExplicitTimezoneForDateGroupingAndDstAwareMonthBoundaries() {
        val instant = localTimestamp(2026, 3, 1, 0, 30, UTC)
        val records = listOf(record("same-instant", instant, TransactionType.EXPENSE))
        val useCase = dailyUseCase(records)

        val utcResult = success(useCase(DailyTransactionsQuery(LocalDate(2026, 3, 1), UTC)))
        val newYorkResult = success(useCase(DailyTransactionsQuery(LocalDate(2026, 2, 1), NEW_YORK)))

        assertEquals(LocalDate(2026, 3, 1), utcResult.dayGroups.single().date)
        assertEquals(LocalDate(2026, 2, 28), newYorkResult.dayGroups.single().date)

        val dstMarchResult = success(useCase(DailyTransactionsQuery(LocalDate(2026, 3, 1), NEW_YORK)))
        assertEquals(
            LocalDate(2026, 3, 1).atStartOfDayIn(TimeZone.of(NEW_YORK)).toEpochMilliseconds(),
            dstMarchResult.startInclusive,
        )
        assertEquals(
            LocalDate(2026, 4, 1).atStartOfDayIn(TimeZone.of(NEW_YORK)).toEpochMilliseconds(),
            dstMarchResult.endExclusive,
        )
        assertEquals((31L * HOURS_PER_DAY - 1L) * MILLIS_PER_HOUR, dstMarchResult.endExclusive - dstMarchResult.startInclusive)
    }

    @Test
    fun groupsAsiaKolkataByLocalDateAndUsesLocalMonthBoundaries() {
        val timeZoneId = ASIA_KOLKATA
        val selectedMonth = LocalDate(2026, 7, 1)
        val insideMonth = localTimestamp(2026, 7, 1, 0, 30, timeZoneId)
        val records =
            listOf(
                record(
                    "before-month",
                    selectedMonth.atStartOfDayIn(TimeZone.of(timeZoneId)).toEpochMilliseconds() - 1L,
                    TransactionType.EXPENSE,
                ),
                record("inside-month", insideMonth, TransactionType.EXPENSE),
                record(
                    "at-next-month",
                    LocalDate(2026, 8, 1).atStartOfDayIn(TimeZone.of(timeZoneId)).toEpochMilliseconds(),
                    TransactionType.EXPENSE,
                ),
            )

        assertEquals(LocalDate(2026, 6, 30), Instant.fromEpochMilliseconds(insideMonth).toLocalDateTime(TimeZone.UTC).date)

        val result = success(dailyUseCase(records)(DailyTransactionsQuery(selectedMonth, timeZoneId)))

        assertEquals(selectedMonth.atStartOfDayIn(TimeZone.of(timeZoneId)).toEpochMilliseconds(), result.startInclusive)
        assertEquals(LocalDate(2026, 8, 1).atStartOfDayIn(TimeZone.of(timeZoneId)).toEpochMilliseconds(), result.endExclusive)
        assertEquals(listOf(LocalDate(2026, 7, 1)), result.dayGroups.map { it.date })
        assertEquals(listOf("inside-month"), result.dayGroups.single().transactions.map { it.id })
    }

    @Test
    fun ordersDateGroupsAndRowsDeterministically() {
        val firstDay = localTimestamp(2026, 7, 10)
        val records =
            listOf(
                record("older-day", localTimestamp(2026, 7, 9), TransactionType.EXPENSE),
                record("same-time-a", firstDay + 1_000L, TransactionType.EXPENSE),
                record("same-time-z", firstDay + 1_000L, TransactionType.EXPENSE),
                record("newer-time", firstDay + 2_000L, TransactionType.EXPENSE),
            )

        val result = success(dailyUseCase(records)(DailyTransactionsQuery(LocalDate(2026, 7, 1), UTC)))

        assertEquals(listOf(LocalDate(2026, 7, 10), LocalDate(2026, 7, 9)), result.dayGroups.map { it.date })
        assertEquals(
            listOf("newer-time", "same-time-z", "same-time-a"),
            result.dayGroups.first().transactions.map { it.id },
        )
    }

    @Test
    fun includesAllRowCurrenciesButCountsOnlyIncomeAndExpense() {
        val records =
            listOf(
                record("income", localTimestamp(2026, 7, 5), TransactionType.INCOME, "INR", 10_000L),
                record("expense", localTimestamp(2026, 7, 5), TransactionType.EXPENSE, "INR", 3_000L),
                transferRecord("transfer", localTimestamp(2026, 7, 5), "USD", "EUR", 5_000L),
                record("loan", localTimestamp(2026, 7, 5), TransactionType.LOAN_DISBURSEMENT, "INR", 2_000L),
                record("repayment", localTimestamp(2026, 7, 5), TransactionType.REPAYMENT, "INR", 1_000L),
                record("adjustment", localTimestamp(2026, 7, 5), TransactionType.ADJUSTMENT, "EUR", 9_000L),
            )

        val result = success(dailyUseCase(records)(DailyTransactionsQuery(LocalDate(2026, 7, 1), UTC)))

        assertEquals(listOf("EUR", "INR", "USD"), result.currencySections.map { it.currencyCode })
        assertEquals(Money.zero(CurrencyCode("EUR")), result.currencySections[0].income)
        assertEquals(Money.zero(CurrencyCode("EUR")), result.currencySections[0].expense)
        assertEquals(Money.zero(CurrencyCode("EUR")), result.currencySections[0].net)
        assertEquals(Money(10_000L, CurrencyCode("INR")), result.currencySections[1].income)
        assertEquals(Money(3_000L, CurrencyCode("INR")), result.currencySections[1].expense)
        assertEquals(Money(7_000L, CurrencyCode("INR")), result.currencySections[1].net)
        assertEquals(Money.zero(CurrencyCode("USD")), result.currencySections[2].net)
        assertEquals(
            setOf(
                TransactionType.ADJUSTMENT,
                TransactionType.EXPENSE,
                TransactionType.INCOME,
                TransactionType.LOAN_DISBURSEMENT,
                TransactionType.REPAYMENT,
                TransactionType.TRANSFER,
            ),
            result.dayGroups.single().transactions.map { it.type }.toSet(),
        )
    }

    @Test
    fun preservesTransferSourceAmountAndCurrency() {
        val result =
            success(
                dailyUseCase(
                    listOf(transferRecord("transfer", localTimestamp(2026, 7, 5), "USD", "EUR", 5_000L)),
                )(DailyTransactionsQuery(LocalDate(2026, 7, 1), UTC)),
            )
        val transfer = result.dayGroups.single().transactions.single()

        assertEquals(Money(5_000L, CurrencyCode("USD")), transfer.amount)
        assertEquals("source", transfer.sourceAccountId)
        assertEquals("destination", transfer.destinationAccountId)
    }

    @Test
    fun locksObservedFirstSplitSemanticsAndMatchesReportsTotals() {
        val records =
            listOf(
                TransactionHistoryRecord(
                    id = "multi-split-expense",
                    timestamp = localTimestamp(2026, 7, 5),
                    description = "multi split",
                    type = TransactionType.EXPENSE,
                    splits =
                        listOf(
                            split("food", "INR", 1_000L, categoryId = "food", categoryName = "Food"),
                            split("rent", "INR", 9_000L, categoryId = "rent", categoryName = "Rent"),
                        ),
                ),
            )
        val repository = FakeTransactionHistoryRepository(LedgerResult.Success(records))
        val dailyUseCase = GetDailyTransactionsUseCase(repository, SpendingAnalyticsCalculator())
        val reportsUseCase = GetSpendingAnalyticsUseCase(repository, SpendingAnalyticsCalculator())
        val query = DailyTransactionsQuery(LocalDate(2026, 7, 1), UTC)

        val daily = success(dailyUseCase(query))
        val report =
            assertIs<ApplicationResult.Success<com.tioledger.application.usecase.analytics.SpendingAnalyticsReport>>(
                reportsUseCase(
                    period = SpendingReportPeriod.MONTHLY,
                    anchorTimestamp = query.month.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds(),
                    timeZoneId = UTC,
                ),
            ).outcome.value

        assertEquals(Money(1_000L, CurrencyCode("INR")), daily.currencySections.single().expense)
        assertEquals(Money(1_000L, CurrencyCode("INR")), daily.dayGroups.single().transactions.single().amount)
        assertEquals(report.currencyReports.single().expense, daily.currencySections.single().expense)
    }

    @Test
    fun returnsEmptyFutureMonthWithoutInventingCurrencySections() {
        val result =
            success(
                dailyUseCase(listOf(record("past", localTimestamp(2026, 7, 5), TransactionType.EXPENSE)))
                    (DailyTransactionsQuery(LocalDate(2099, 1, 1), UTC)),
            )

        assertTrue(result.dayGroups.isEmpty())
        assertTrue(result.currencySections.isEmpty())
    }

    @Test
    fun propagatesRepositoryAndValidationFailuresWithoutWrites() {
        val repository =
            FakeTransactionHistoryRepository(
                LedgerResult.Failure(LedgerError.Unknown("db unavailable")),
            )
        val useCase = dailyUseCase(repository)

        val repositoryResult = useCase(DailyTransactionsQuery(LocalDate(2026, 7, 1), UTC))
        assertIs<ApplicationError.Repository>(assertIs<ApplicationResult.Failure>(repositoryResult).error)
        assertEquals(1, repository.readCount)

        val blankZone = useCase(DailyTransactionsQuery(LocalDate(2026, 7, 1), " "))
        assertIs<ApplicationError.Validation>(assertIs<ApplicationResult.Failure>(blankZone).error)
        assertEquals(1, repository.readCount)

        val invalidZone = useCase(DailyTransactionsQuery(LocalDate(2026, 7, 1), "Not/AZone"))
        assertIs<ApplicationError.Validation>(assertIs<ApplicationResult.Failure>(invalidZone).error)
        assertEquals(1, repository.readCount)

        val nonCanonicalMonth = useCase(DailyTransactionsQuery(LocalDate(2026, 7, 5), UTC))
        assertIs<ApplicationError.Validation>(assertIs<ApplicationResult.Failure>(nonCanonicalMonth).error)
        assertEquals(1, repository.readCount)
    }

    @Test
    fun repeatedCallsAreDeterministicAndReadOnly() {
        val repository =
            FakeTransactionHistoryRepository(
                LedgerResult.Success(
                    listOf(
                        record("expense", localTimestamp(2026, 7, 5), TransactionType.EXPENSE, "INR", 3_000L),
                    ),
                ),
            )
        val useCase = dailyUseCase(repository)
        val query = DailyTransactionsQuery(LocalDate(2026, 7, 1), UTC)

        val first = useCase(query)
        val second = useCase(query)

        assertEquals(first, second)
        assertEquals(2, repository.readCount)
    }

    private fun dailyUseCase(records: List<TransactionHistoryRecord>): GetDailyTransactionsUseCase =
        GetDailyTransactionsUseCase(
            transactionHistoryRepository = FakeTransactionHistoryRepository(LedgerResult.Success(records)),
            calculator = SpendingAnalyticsCalculator(),
        )

    private fun dailyUseCase(repository: FakeTransactionHistoryRepository): GetDailyTransactionsUseCase =
        GetDailyTransactionsUseCase(repository, SpendingAnalyticsCalculator())

    private fun success(result: ApplicationResult<DailyTransactionsSnapshot>): DailyTransactionsSnapshot =
        assertIs<ApplicationResult.Success<DailyTransactionsSnapshot>>(result).outcome.value

    private fun record(
        id: String,
        timestamp: Long,
        type: TransactionType,
        currencyCode: String = "INR",
        amount: Long = 1_000L,
    ): TransactionHistoryRecord =
        TransactionHistoryRecord(
            id = id,
            timestamp = timestamp,
            description = id,
            type = type,
            splits = listOf(split("account", currencyCode, amount)),
        )

    private fun transferRecord(
        id: String,
        timestamp: Long,
        sourceCurrency: String,
        destinationCurrency: String,
        amount: Long,
    ): TransactionHistoryRecord =
        TransactionHistoryRecord(
            id = id,
            timestamp = timestamp,
            description = id,
            type = TransactionType.TRANSFER,
            splits =
                listOf(
                    split(
                        accountId = "destination",
                        currencyCode = destinationCurrency,
                        amount = amount,
                        entryType = LedgerEntryType.DEBIT,
                    ),
                    split(
                        accountId = "source",
                        currencyCode = sourceCurrency,
                        amount = amount,
                        entryType = LedgerEntryType.CREDIT,
                    ),
                ),
        )

    private fun split(
        accountId: String,
        currencyCode: String,
        amount: Long,
        entryType: LedgerEntryType = LedgerEntryType.DEBIT,
        categoryId: String? = null,
        categoryName: String? = null,
    ): TransactionHistorySplit =
        TransactionHistorySplit(
            id = "$accountId-$currencyCode-$amount",
            accountId = accountId,
            accountName = accountId,
            accountType = AccountType.BANK,
            amount = Money(amount, CurrencyCode(currencyCode)),
            categoryId = categoryId,
            categoryName = categoryName,
            entryType = entryType,
        )

    private fun localTimestamp(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0,
        timeZoneId: String = UTC,
    ): Long =
        LocalDate(year, month, day)
            .atStartOfDayIn(TimeZone.of(timeZoneId))
            .toEpochMilliseconds() +
            hour * MILLIS_PER_HOUR + minute * MILLIS_PER_MINUTE

    private class FakeTransactionHistoryRepository(
        private val result: LedgerResult<List<TransactionHistoryRecord>>,
    ) : TransactionHistoryRepository {
        var readCount: Int = 0

        override fun findAll(): LedgerResult<List<TransactionHistoryRecord>> {
            readCount += 1
            return result
        }
    }

    private companion object {
        const val UTC = "UTC"
        const val NEW_YORK = "America/New_York"
        const val ASIA_KOLKATA = "Asia/Kolkata"
        const val MILLIS_PER_MINUTE = 60_000L
        const val MILLIS_PER_HOUR = 60L * MILLIS_PER_MINUTE
        const val HOURS_PER_DAY = 24L
    }
}
