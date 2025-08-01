package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.model.TooltipData
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.x8bit.bitwarden.ui.platform.components.dropdown.BitwardenMultiSelectButton
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.platform.composition.LocalIntentManager
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.platform.util.displayLabel
import kotlinx.collections.immutable.toImmutableList

/**
 * Displays the appearance screen.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    onNavigateBack: () -> Unit,
    viewModel: AppearanceViewModel = hiltViewModel(),
    intentManager: IntentManager = LocalIntentManager.current,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            AppearanceEvent.NavigateBack -> onNavigateBack.invoke()
            AppearanceEvent.NavigateToWebsiteIconsHelp -> {
                intentManager.launchUri("https://bitwarden.com/help/website-icons/".toUri())
            }
        }
    }

    AppearanceDialogs(
        dialogState = state.dialogState,
        onConfirmEnableDynamicColorsClick = remember(viewModel) {
            { viewModel.trySendAction(AppearanceAction.ConfirmEnableDynamicColorsClick) }
        },
        onDismissDialog = remember(viewModel) {
            { viewModel.trySendAction(AppearanceAction.DismissDialog) }
        },
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.appearance),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.BackClick) }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(modifier = Modifier.height(height = 12.dp))
            LanguageSelectionRow(
                currentSelection = state.language,
                onLanguageSelection = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.LanguageChange(it)) }
                },
                modifier = Modifier
                    .testTag("LanguageChooser")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            ThemeSelectionRow(
                currentSelection = state.theme,
                onThemeSelection = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.ThemeChange(it)) }
                },
                modifier = Modifier
                    .testTag("ThemeChooser")
                    .standardHorizontalMargin()
                    .fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
            if (state.isDynamicColorsSupported) {
                BitwardenSwitch(
                    label = stringResource(id = BitwardenString.use_dynamic_colors),
                    isChecked = state.isDynamicColorsEnabled,
                    onCheckedChange = remember(viewModel) {
                        { viewModel.trySendAction(AppearanceAction.DynamicColorsToggle(it)) }
                    },
                    cardStyle = CardStyle.Full,
                    modifier = Modifier
                        .testTag("DynamicColorsSwitch")
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
                Spacer(modifier = Modifier.height(height = 8.dp))
            }
            BitwardenSwitch(
                label = stringResource(id = BitwardenString.show_website_icons),
                supportingText = stringResource(
                    id = BitwardenString.show_website_icons_description,
                ),
                isChecked = state.showWebsiteIcons,
                onCheckedChange = remember(viewModel) {
                    { viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsToggle(it)) }
                },
                tooltip = TooltipData(
                    onClick = remember(viewModel) {
                        { viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsTooltipClick) }
                    },
                    contentDescription = stringResource(
                        id = BitwardenString.show_website_icons_help,
                    ),
                ),
                cardStyle = CardStyle.Full,
                modifier = Modifier
                    .testTag("ShowWebsiteIconsSwitch")
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
            )
            Spacer(modifier = Modifier.height(height = 16.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun AppearanceDialogs(
    dialogState: AppearanceState.DialogState?,
    onConfirmEnableDynamicColorsClick: () -> Unit,
    onDismissDialog: () -> Unit,
) {
    when (dialogState) {
        AppearanceState.DialogState.EnableDynamicColors -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.use_dynamic_colors_question),
                message = stringResource(
                    id = BitwardenString.dynamic_colors_may_not_adhere_to_accessibility_guidelines,
                ),
                confirmButtonText = stringResource(BitwardenString.okay),
                dismissButtonText = stringResource(BitwardenString.cancel),
                onConfirmClick = onConfirmEnableDynamicColorsClick,
                onDismissClick = onDismissDialog,
                onDismissRequest = onDismissDialog,
            )
        }

        else -> Unit
    }
}

@Composable
private fun LanguageSelectionRow(
    currentSelection: AppLanguage,
    onLanguageSelection: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
) {
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.language),
        options = AppLanguage.entries.map { it.text() }.toImmutableList(),
        selectedOption = currentSelection.text(),
        onOptionSelected = { selectedLanguage ->
            onLanguageSelection(
                AppLanguage.entries.first { selectedLanguage == it.text.toString(resources) },
            )
        },
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}

@Composable
private fun ThemeSelectionRow(
    currentSelection: AppTheme,
    onThemeSelection: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
    resources: Resources = LocalContext.current.resources,
) {
    BitwardenMultiSelectButton(
        label = stringResource(id = BitwardenString.theme),
        options = AppTheme.entries.map { it.displayLabel() }.toImmutableList(),
        selectedOption = currentSelection.displayLabel(),
        onOptionSelected = { selectedTheme ->
            onThemeSelection(
                AppTheme.entries.first { selectedTheme == it.displayLabel.toString(resources) },
            )
        },
        supportingText = stringResource(id = BitwardenString.theme_description),
        cardStyle = CardStyle.Full,
        modifier = modifier,
    )
}
