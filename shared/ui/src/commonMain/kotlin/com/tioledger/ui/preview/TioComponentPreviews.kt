@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tioledger.ui.components.TioAccountCard
import com.tioledger.ui.components.TioAccountRow
import com.tioledger.ui.components.TioAmountText
import com.tioledger.ui.components.TioAmountTone
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBadge
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioBottomSheetContent
import com.tioledger.ui.components.TioCard
import com.tioledger.ui.components.TioCategoryChip
import com.tioledger.ui.components.TioCategoryRow
import com.tioledger.ui.components.TioCurrencyBadge
import com.tioledger.ui.components.TioCurrencyText
import com.tioledger.ui.components.TioDialog
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioErrorState
import com.tioledger.ui.components.TioFilterChip
import com.tioledger.ui.components.TioFloatingActionButton
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.components.TioLoadingState
import com.tioledger.ui.components.TioNavigationRail
import com.tioledger.ui.components.TioPrimaryButton
import com.tioledger.ui.components.TioSearchBar
import com.tioledger.ui.components.TioSearchField
import com.tioledger.ui.components.TioSecondaryButton
import com.tioledger.ui.components.TioSectionHeader
import com.tioledger.ui.components.TioSummaryCard
import com.tioledger.ui.components.TioTransactionRow
import com.tioledger.ui.design.TioIconToken
import com.tioledger.ui.design.TioLedgerTheme
import com.tioledger.ui.templates.TioDashboardTemplate
import com.tioledger.ui.templates.TioDetailScreenTemplate
import com.tioledger.ui.templates.TioFormScreenTemplate
import com.tioledger.ui.templates.TioListScreenTemplate
import com.tioledger.ui.templates.TioTemplatePreviewData
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
private fun ComponentCatalogLightPreview() {
    TioComponentCatalogPreview(darkTheme = false)
}

@Preview
@Composable
private fun ComponentCatalogDarkPreview() {
    TioComponentCatalogPreview(darkTheme = true)
}

@Preview
@Composable
private fun ComponentCatalogCompactPreview() {
    TioComponentCatalogPreview(darkTheme = false)
}

@Preview
@Composable
private fun ComponentCatalogTabletPreview() {
    TioComponentCatalogPreview(darkTheme = false, showRail = true)
}

@Preview
@Composable
private fun ListTemplatePreview() {
    TioLedgerTheme {
        TioListScreenTemplate(
            title = "Accounts",
            items = TioTemplatePreviewData.listItems,
        )
    }
}

@Preview
@Composable
private fun DetailTemplatePreview() {
    TioLedgerTheme {
        TioDetailScreenTemplate(
            title = "Details",
            summary = "Reference placeholder",
            rows = TioTemplatePreviewData.listItems,
        )
    }
}

@Preview
@Composable
private fun FormTemplatePreview() {
    TioLedgerTheme {
        TioFormScreenTemplate(
            title = "Form",
            fields = listOf("Name", "Amount", "Memo"),
        )
    }
}

@Preview
@Composable
private fun DashboardTemplatePreview() {
    TioLedgerTheme {
        TioDashboardTemplate(summaryItems = TioTemplatePreviewData.summaryItems)
    }
}

@Composable
private fun TioComponentCatalogPreview(
    darkTheme: Boolean,
    showRail: Boolean = false,
) {
    TioLedgerTheme(darkTheme = darkTheme) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (showRail) {
                TioNavigationRail(
                    items = TioTemplatePreviewData.navigationItems,
                    onItemSelected = {},
                )
            }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
            ) {
                TioAppBar(title = "Tio Ledger")
                TioSectionHeader(title = "Overview")
                TioSummaryCard(
                    label = "Total",
                    value = "INR 0.00",
                    subtitle = "Preview only",
                )
                TioCard(elevated = true) {
                    Column {
                        TioAmountText("INR 0.00", tone = TioAmountTone.Neutral)
                        TioCurrencyText("INR", label = "Currency")
                        TioCurrencyBadge("INR")
                    }
                }
                TioSearchField(value = "", onValueChange = {})
                TioSearchBar(value = "", onValueChange = {}, placeholder = "Search records")
                TioListItem(title = "Daily", subtitle = "Money Manager-style row")
                TioTransactionRow(
                    title = "Lunch",
                    subtitle = "Food",
                    amount = "INR 0.00",
                    amountTone = TioAmountTone.Negative,
                )
                TioAccountRow(name = "Cash", accountType = "Wallet", balance = "INR 0.00")
                TioAccountCard(
                    name = "Bank",
                    accountType = "Savings",
                    balance = "INR 0.00",
                    currencyCode = "INR",
                )
                TioCategoryRow(name = "Food", subtitle = "Category")
                TioCategoryChip(label = "Food", selected = true, onClick = {})
                TioFilterChip(label = "Monthly", selected = true, onClick = {})
                TioBadge(label = "New")
                TioEmptyState(title = "No records", message = "Add a record to see it here.")
                TioLoadingState(label = "Loading")
                TioErrorState(
                    title = "Unable to load",
                    message = "Try again.",
                    retryLabel = "Retry",
                    onRetry = {},
                )
                TioBottomSheetContent(title = "Filters") {
                    Text("Placeholder filters")
                }
                TioBottomNavigation(items = TioTemplatePreviewData.navigationItems, onItemSelected = {})
                TioPrimaryButton(label = "Primary", onClick = {})
                TioSecondaryButton(label = "Secondary", onClick = {})
                TioFloatingActionButton(onClick = {}, icon = TioIconToken.Add)
            }
        }
    }
}

@Preview
@Composable
private fun DialogComponentPreview() {
    TioLedgerTheme {
        TioDialog(
            title = "Confirm",
            text = "Review before saving.",
            confirmLabel = "OK",
            dismissLabel = "Cancel",
            onConfirm = {},
            onDismiss = {},
        )
    }
}
