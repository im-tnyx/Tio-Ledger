@file:Suppress("FunctionName")

package com.tioledger.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tioledger.domain.model.CurrencyDecimalDisplay
import com.tioledger.domain.model.CurrencyUnitPosition
import com.tioledger.ui.components.TioAppBar
import com.tioledger.ui.design.TioDimensions
import com.tioledger.ui.design.TioSpacing
import com.tioledger.ui.navigation.MainRoute
import org.koin.compose.koinInject

private val CoralAccentColor = Color(0xFFF25A4B)

@Composable
fun CurrencySettingsRoute(
    onNavigateBack: () -> Unit = {},
    onNavigate: (MainRoute) -> Unit = {},
    viewModel: CurrencySettingsViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()
    CurrencySettingsScreen(
        state = state,
        onAction = { action ->
            viewModel.onAction(action, onSaveSuccess = onNavigateBack)
        },
        onNavigateBack = {
            viewModel.onAction(CurrencySettingsAction.ResetDraft)
            onNavigateBack()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySettingsScreen(
    state: CurrencySettingsUiState,
    onAction: (CurrencySettingsAction) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TioAppBar(
                title = "Main Currency Setting",
                navigationIcon = {
                    TextButton(
                        onClick = onNavigateBack,
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.semantics { contentDescription = "Navigate back to Settings" },
                    ) {
                        Text(
                            text = "‹ Settings",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { onAction(CurrencySettingsAction.OpenChangeCurrency) },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TioSpacing.lg)
                        .padding(top = TioSpacing.xl, bottom = TioSpacing.xxl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = state.headerTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(TioSpacing.xs))
                Text(
                    text = state.formattedPreview,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider()

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = TioDimensions.accountRowHeight)
                        .clickable { onAction(CurrencySettingsAction.OpenUnitPositionSheet) }
                        .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Unit position",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.unitPosition.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider()

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = TioDimensions.accountRowHeight)
                        .clickable { onAction(CurrencySettingsAction.OpenDecimalPointSheet) }
                        .padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Decimal point",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = state.decimalDisplay.label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(TioSpacing.xxl))

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = TioSpacing.lg),
            ) {
                Button(
                    onClick = { onAction(CurrencySettingsAction.Save) },
                    enabled = !state.isSaving,
                    shape = RoundedCornerShape(8.dp),
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = CoralAccentColor,
                            contentColor = Color.White,
                        ),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                    } else {
                        Text(
                            text = "Save",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        )
                    }
                }

                state.errorMessage?.let { errorText ->
                    Spacer(modifier = Modifier.height(TioSpacing.sm))
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (state.isUnitPositionSheetVisible) {
        CurrencySelectionBottomSheet(
            title = "Unit position",
            options = CurrencyUnitPosition.entries,
            optionLabel = { it.displayName },
            onSelect = { onAction(CurrencySettingsAction.UnitPositionSelected(it)) },
            onDismiss = { onAction(CurrencySettingsAction.DismissUnitPositionSheet) },
        )
    }

    if (state.isDecimalPointSheetVisible) {
        CurrencySelectionBottomSheet(
            title = "Decimal point",
            options = CurrencyDecimalDisplay.entries,
            optionLabel = { it.label },
            onSelect = { onAction(CurrencySettingsAction.DecimalPointSelected(it)) },
            onDismiss = { onAction(CurrencySettingsAction.DismissDecimalPointSheet) },
        )
    }

    if (state.isChangeCurrencyDialogVisible) {
        CurrencySelectionBottomSheet(
            title = "Change Currency",
            options = state.supportedCurrencies,
            optionLabel = { it.displayName },
            onSelect = { onAction(CurrencySettingsAction.CurrencySelected(it)) },
            onDismiss = { onAction(CurrencySettingsAction.DismissChangeCurrency) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> CurrencySelectionBottomSheet(
    title: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TioSpacing.md, vertical = TioSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(TioSpacing.sm),
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item(key = "header") {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = TioSpacing.lg, vertical = TioSpacing.md),
                        )
                        HorizontalDivider()
                    }
                    items(options.size, key = { it }) { index ->
                        val item = options[index]
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(item) }
                                    .padding(horizontal = TioSpacing.lg, vertical = 14.dp),
                        ) {
                            Text(
                                text = optionLabel(item),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        if (index < options.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() },
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(TioSpacing.sm))
        }
    }
}
