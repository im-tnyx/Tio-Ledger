package com.tioledger.finance.engine

class PostingStrategyRegistry(
    private val incomeStrategy: LedgerPostingStrategy<PostingParams.Income> = IncomePostingStrategy(),
    private val expenseStrategy: LedgerPostingStrategy<PostingParams.Expense> = ExpensePostingStrategy(),
    private val transferStrategy: LedgerPostingStrategy<PostingParams.Transfer> = TransferPostingStrategy(),
    private val openingBalanceStrategy: LedgerPostingStrategy<PostingParams.OpeningBalance> = OpeningBalancePostingStrategy(),
    private val adjustmentStrategy: LedgerPostingStrategy<PostingParams.Adjustment> = AdjustmentPostingStrategy(),
) {
    fun resolveIncome(): LedgerPostingStrategy<PostingParams.Income> = incomeStrategy

    fun resolveExpense(): LedgerPostingStrategy<PostingParams.Expense> = expenseStrategy

    fun resolveTransfer(): LedgerPostingStrategy<PostingParams.Transfer> = transferStrategy

    fun resolveOpeningBalance(): LedgerPostingStrategy<PostingParams.OpeningBalance> = openingBalanceStrategy

    fun resolveAdjustment(): LedgerPostingStrategy<PostingParams.Adjustment> = adjustmentStrategy
}
