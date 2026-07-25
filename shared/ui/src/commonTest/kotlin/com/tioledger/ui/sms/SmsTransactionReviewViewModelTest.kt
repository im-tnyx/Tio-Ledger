package com.tioledger.ui.sms

import com.tioledger.application.model.ApplicationError
import com.tioledger.application.model.ApplicationResult
import com.tioledger.application.model.UseCaseOutcome
import com.tioledger.application.usecase.sms.ConfirmSmsTransactionCommand
import com.tioledger.application.usecase.sms.PreparedSmsTransactionReview
import com.tioledger.application.usecase.sms.SmsReviewAccountOption
import com.tioledger.application.usecase.sms.SmsReviewCategoryOption
import com.tioledger.application.usecase.sms.SmsReviewPreparation
import com.tioledger.domain.model.AccountType
import com.tioledger.domain.model.CategoryType
import com.tioledger.domain.model.SmsIgnoredReason
import com.tioledger.domain.model.SmsMissingField
import com.tioledger.domain.model.SmsParseConfidence
import com.tioledger.domain.model.SmsPaymentRail
import com.tioledger.domain.model.SmsTransactionDirection
import com.tioledger.domain.model.SmsTransactionSuggestion
import com.tioledger.domain.model.Transaction
import com.tioledger.domain.model.TransactionRecord
import com.tioledger.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SmsTransactionReviewViewModelTest {
    @Test
    fun preparedReviewClearsRawMessageAndMapsEditableFields() {
        val viewModel = viewModel(preparation = reviewPreparation())

        viewModel.onAction(SmsTransactionReviewAction.MessageChanged(RAW_MESSAGE))
        viewModel.onAction(SmsTransactionReviewAction.ParseClicked)

        val state = viewModel.uiState.value
        assertEquals(SmsReviewStage.REVIEW, state.stage)
        assertEquals("", state.messageInput)
        assertEquals("1250.50", state.amount)
        assertEquals("INR", state.currencyCode)
        assertEquals(SmsTransactionDirection.EXPENSE, state.direction)
        assertEquals("bank-1234", state.selectedAccountId)
        assertEquals("Grocery Mart", state.note)
        assertEquals(SmsPaymentRail.CARD, state.paymentRail)
        assertTrue(SmsMissingField.CATEGORY in state.unresolvedFields)
        assertFalse(state.canSave)
    }

    @Test
    fun ignoredMessageClearsRawTextAndDoesNotInvokeConfirmation() {
        var confirmationCalls = 0
        val viewModel =
            viewModel(
                preparation = SmsReviewPreparation.Ignored(SmsIgnoredReason.OTP_OR_SECURITY_CODE, emptyList()),
                confirm = {
                    confirmationCalls += 1
                    success(transactionRecord())
                },
            )

        viewModel.onAction(SmsTransactionReviewAction.MessageChanged("Your OTP is 123456"))
        viewModel.onAction(SmsTransactionReviewAction.ParseClicked)

        assertEquals(SmsReviewStage.IGNORED, viewModel.uiState.value.stage)
        assertEquals("", viewModel.uiState.value.messageInput)
        assertEquals(0, confirmationCalls)
    }

    @Test
    fun rejectEmitsEventWithoutInvokingConfirmation() {
        var confirmationCalls = 0
        val viewModel =
            viewModel(
                preparation = reviewPreparation(),
                confirm = {
                    confirmationCalls += 1
                    success(transactionRecord())
                },
            )
        viewModel.onAction(SmsTransactionReviewAction.MessageChanged(RAW_MESSAGE))
        viewModel.onAction(SmsTransactionReviewAction.ParseClicked)

        viewModel.onAction(SmsTransactionReviewAction.RejectClicked)

        assertEquals(SmsReviewStage.REJECTED, viewModel.uiState.value.stage)
        assertIs<SmsTransactionReviewEvent.Rejected>(viewModel.event.value)
        assertEquals(0, confirmationCalls)
    }

    @Test
    fun saveUsesEditedStructuredFieldsAndEmitsSuccess() {
        var capturedCommand: ConfirmSmsTransactionCommand? = null
        val viewModel =
            viewModel(
                preparation = reviewPreparation(),
                confirm = { command ->
                    capturedCommand = command
                    success(transactionRecord())
                },
            )
        viewModel.onAction(SmsTransactionReviewAction.MessageChanged(RAW_MESSAGE))
        viewModel.onAction(SmsTransactionReviewAction.ParseClicked)
        viewModel.onAction(SmsTransactionReviewAction.CategoryClicked)
        viewModel.onAction(SmsTransactionReviewAction.CategorySelected("food"))
        viewModel.onAction(SmsTransactionReviewAction.AmountChanged("25.75"))
        viewModel.onAction(SmsTransactionReviewAction.NoteChanged("Edited note"))

        viewModel.onAction(SmsTransactionReviewAction.SaveClicked)

        val command = requireNotNull(capturedCommand)
        assertEquals(2_575L, command.amountMinorUnits)
        assertEquals("INR", command.currencyCode)
        assertEquals("bank-1234", command.accountId)
        assertEquals("food", command.categoryId)
        assertEquals("Edited note", command.description)
        assertTrue(command.userConfirmed)
        assertEquals(SmsReviewStage.SAVED, viewModel.uiState.value.stage)
        assertIs<SmsTransactionReviewEvent.TransactionSaved>(viewModel.event.value)
    }

    @Test
    fun featureFlagFailureShowsDisabledStateAndDropsMessageText() {
        val viewModel =
            SmsTransactionReviewViewModel(
                prepareReview = {
                    ApplicationResult.Failure(
                        ApplicationError.Validation(
                            field = "featureFlag",
                            reason = "SMS-assisted transaction review is disabled.",
                        ),
                    )
                },
                confirmReview = { success(transactionRecord()) },
                nowProvider = { NOW },
                timeZoneIdProvider = { "Asia/Kolkata" },
            )

        viewModel.onAction(SmsTransactionReviewAction.MessageChanged(RAW_MESSAGE))
        viewModel.onAction(SmsTransactionReviewAction.ParseClicked)

        assertEquals(SmsReviewStage.DISABLED, viewModel.uiState.value.stage)
        assertEquals("", viewModel.uiState.value.messageInput)
        assertNull(viewModel.uiState.value.validationErrorMessage)
    }
}

private fun viewModel(
    preparation: SmsReviewPreparation,
    confirm: (ConfirmSmsTransactionCommand) -> ApplicationResult<TransactionRecord> = {
        success(transactionRecord())
    },
): SmsTransactionReviewViewModel =
    SmsTransactionReviewViewModel(
        prepareReview = { success(preparation) },
        confirmReview = confirm,
        nowProvider = { NOW },
        timeZoneIdProvider = { "Asia/Kolkata" },
    )

private fun reviewPreparation(): SmsReviewPreparation =
    SmsReviewPreparation.Review(
        PreparedSmsTransactionReview(
            suggestion =
                SmsTransactionSuggestion(
                    amountMinorUnits = 125_050L,
                    currencyCode = "INR",
                    direction = SmsTransactionDirection.EXPENSE,
                    occurredAt = NOW,
                    accountHint = "1234",
                    merchantOrCounterparty = "Grocery Mart",
                    paymentRail = SmsPaymentRail.CARD,
                    confidence = SmsParseConfidence.HIGH,
                    missingFields = listOf(SmsMissingField.ACCOUNT, SmsMissingField.CATEGORY),
                    evidence = emptyList(),
                ),
            accountOptions =
                listOf(
                    SmsReviewAccountOption(
                        id = "bank-1234",
                        name = "Everyday account",
                        type = AccountType.BANK,
                        currencyCode = "INR",
                    ),
                ),
            categoryOptions =
                listOf(
                    SmsReviewCategoryOption(
                        id = "food",
                        name = "Food",
                        type = CategoryType.EXPENSE,
                    ),
                ),
            suggestedAccountId = "bank-1234",
            unresolvedFields = listOf(SmsMissingField.CATEGORY),
        ),
    )

private fun transactionRecord(): TransactionRecord =
    TransactionRecord(
        transaction =
            Transaction(
                id = "txn-sms-1",
                timestamp = NOW,
                description = "SMS review",
                type = TransactionType.EXPENSE,
                createdAt = NOW,
                updatedAt = NOW,
            ),
        splits = emptyList(),
        ledgerEntries = emptyList(),
    )

private fun <T> success(value: T): ApplicationResult.Success<T> = ApplicationResult.Success(UseCaseOutcome(value))

private const val RAW_MESSAGE = "INR 1,250.50 debited from account 1234 at Grocery Mart"
private const val NOW = 1_721_800_000_000L
