@file:Suppress("FunctionName", "UnusedPrivateMember")

package com.tioledger.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tioledger.ui.components.TioAccountRow
import com.tioledger.ui.components.TioAmountText
import com.tioledger.ui.components.TioAmountTone
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.components.TioBottomNavigation
import com.tioledger.ui.components.TioBottomSheetContent
import com.tioledger.ui.components.TioCard
import com.tioledger.ui.components.TioCategoryRow
import com.tioledger.ui.components.TioCurrencyBadge
import com.tioledger.ui.components.TioDialog
import com.tioledger.ui.components.TioEmptyState
import com.tioledger.ui.components.TioFilterChip
import com.tioledger.ui.components.TioFloatingActionButton
import com.tioledger.ui.components.TioListItem
import com.tioledger.ui.components.TioSearchField
import com.tioledger.ui.components.TioSectionHeader
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
private fun ComponentCatalogSmallWidthPreview() {
    TioComponentCatalogPreview(darkTheme = false)
}

@Preview
@Composable
private fun ComponentCatalogLargeWidthPreview() {
    TioComponentCatalogPreview(darkTheme = false)
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
private fun TioComponentCatalogPreview(darkTheme: Boolean) {
    TioLedgerTheme(darkTheme = darkTheme) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
        ) {
            TioAppBar(title = "Tio Ledger")
            TioSectionHeader(title = "Overview")
            TioCard(elevated = true) {
                Column {
                    TioAmountText("₹0.00", tone = TioAmountTone.Neutral)
                    TioCurrencyBadge("INR")
                }
            }
            TioSearchField(value = "", onValueChange = {})
            TioListItem(title = "Daily", subtitle = "Money Manager-style row")
            TioAccountRow(name = "Cash", accountType = "Wallet", balance = "₹0.00")
            TioCategoryRow(name = "Food", subtitle = "Category")
            TioFilterChip(label = "Monthly", selected = true, onClick = {})
            TioEmptyState(title = "No records", message = "Add a record to see it here.")
            TioBottomSheetContent(title = "Filters") {
                Text("Placeholder filters")
            }
            TioBottomNavigation(items = TioTemplatePreviewData.navigationItems, onItemSelected = {})
            TioFloatingActionButton(onClick = {})
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
