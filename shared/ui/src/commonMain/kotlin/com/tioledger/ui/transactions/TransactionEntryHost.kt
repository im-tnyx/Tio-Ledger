@file:Suppress("FunctionName")

package com.tioledger.ui.transactions

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

@Composable
fun TransactionEntryHost(
    viewModel: TransactionEntryViewModel = koinInject(),
    onNavigateBack: () -> Unit = {},
) {
    val event by viewModel.event.collectAsState()

    TransactionEntryRoute(
        viewModel = viewModel,
        onNavigateBack = onNavigateBack,
    )

    val dateSelectionRequest = event as? TransactionEntryEvent.DateSelectionRequested
    if (dateSelectionRequest != null) {
        TransactionDatePickerDialog(
            currentTimestamp = dateSelectionRequest.currentTimestamp,
            onDismiss = {
                viewModel.onAction(TransactionEntryAction.EventConsumed)
            },
            onDateSelected = { selection ->
                viewModel.onAction(
                    TransactionEntryAction.DateSelected(
                        timestamp = selection.timestamp,
                        label = selection.label,
                    ),
                )
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerDialog(
    currentTimestamp: Long,
    onDismiss: () -> Unit,
    onDateSelected: (TransactionDateSelection) -> Unit,
) {
    val datePickerState =
        androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis =
                transactionTimestampToDatePickerMillis(
                    timestamp = currentTimestamp,
                    timeZone = TimeZone.currentSystemDefault(),
                ),
        )
    val selectedDateMillis = datePickerState.selectedDateMillis

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    selectedDateMillis?.let { pickerTimestamp ->
                        onDateSelected(
                            datePickerMillisToTransactionDateSelection(
                                pickerTimestamp = pickerTimestamp,
                                timeZone = TimeZone.currentSystemDefault(),
                            ),
                        )
                    }
                },
                enabled = selectedDateMillis != null,
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    ) {
        DatePicker(state = datePickerState)
    }
}

internal data class TransactionDateSelection(
    val timestamp: Long,
    val label: String,
)

internal fun transactionTimestampToDatePickerMillis(
    timestamp: Long,
    timeZone: TimeZone,
): Long {
    val localDate =
        Instant
            .fromEpochMilliseconds(timestamp)
            .toLocalDateTime(timeZone)
            .date
    return localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
}

internal fun datePickerMillisToTransactionDateSelection(
    pickerTimestamp: Long,
    timeZone: TimeZone,
): TransactionDateSelection {
    val selectedDate = pickerTimestamp.toUtcDate()
    return TransactionDateSelection(
        timestamp = selectedDate.atStartOfDayIn(timeZone).toEpochMilliseconds(),
        label = selectedDate.toString(),
    )
}

private fun Long.toUtcDate(): LocalDate =
    Instant
        .fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.UTC)
        .date
