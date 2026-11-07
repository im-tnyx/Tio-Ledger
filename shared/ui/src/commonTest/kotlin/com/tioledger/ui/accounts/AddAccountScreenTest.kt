package com.tioledger.ui.accounts

import com.tioledger.application.usecase.account.CreateAccountCommand
import com.tioledger.domain.model.AccountType
import com.tioledger.ui.more.AccountGroupEntry
import com.tioledger.ui.more.AccountGroupPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AddAccountScreenTest {
    @Test
    fun accountGroupPolicyMapsToCorrectAccountType() {
        assertEquals(AccountType.CASH, AccountGroupPolicy.CASH.accountType)
        assertEquals(AccountType.BANK, AccountGroupPolicy.BANK.accountType)
        assertEquals(AccountType.CREDIT_CARD, AccountGroupPolicy.CREDIT_CARD.accountType)
        assertEquals(AccountType.LOAN_LINKED, AccountGroupPolicy.LOAN.accountType)
        assertEquals(AccountType.INVESTMENT, AccountGroupPolicy.INVESTMENT.accountType)
        assertEquals(AccountType.WALLET, AccountGroupPolicy.OTHER.accountType)
    }

    @Test
    fun createAccountCommandPayloadMatchesPolicy() {
        val group =
            AccountGroupEntry(
                id = "group_hdfc_card",
                title = "HDFC Regalia",
                subtitle = "Card",
                policy = AccountGroupPolicy.CREDIT_CARD,
                isPermanent = false,
            )

        val command =
            CreateAccountCommand(
                id = "acc-test-1",
                name = group.title,
                type = group.policy.accountType,
                currencyCode = "USD",
                createdAt = 1_000_000L,
            )

        assertEquals("acc-test-1", command.id)
        assertEquals("HDFC Regalia", command.name)
        assertEquals(AccountType.CREDIT_CARD, command.type)
        assertEquals("USD", command.currencyCode)
        assertNotNull(command)
    }

    @Test
    fun loanPolicyGeneratesLoanLinkedAccountType() {
        val loanGroup =
            AccountGroupEntry(
                id = "group_home_loan",
                title = "SBI Home Loan",
                subtitle = "Loan",
                policy = AccountGroupPolicy.LOAN,
                isPermanent = false,
            )

        val command =
            CreateAccountCommand(
                id = "acc-loan-1",
                name = loanGroup.title,
                type = loanGroup.policy.accountType,
                currencyCode = "INR",
                createdAt = 2_000_000L,
            )

        assertEquals(AccountType.LOAN_LINKED, command.type)
        assertEquals("SBI Home Loan", command.name)
    }
}
