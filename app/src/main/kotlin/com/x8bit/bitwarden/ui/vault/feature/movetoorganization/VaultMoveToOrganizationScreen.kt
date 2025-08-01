package com.x8bit.bitwarden.ui.vault.feature.movetoorganization

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.x8bit.bitwarden.ui.vault.model.VaultCollection

/**
 * Displays the vault move to organization screen.
 */
@Composable
fun VaultMoveToOrganizationScreen(
    viewModel: VaultMoveToOrganizationViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is VaultMoveToOrganizationEvent.NavigateBack -> onNavigateBack()
        }
    }
    VaultMoveToOrganizationScaffold(
        state = state,
        closeClick = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.BackClick) }
        },
        moveClick = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.MoveClick) }
        },
        dismissClick = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.DismissClick) }
        },
        organizationSelect = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.OrganizationSelect(it)) }
        },
        collectionSelect = remember(viewModel) {
            { viewModel.trySendAction(VaultMoveToOrganizationAction.CollectionSelect(it)) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod")
@Composable
private fun VaultMoveToOrganizationScaffold(
    state: VaultMoveToOrganizationState,
    closeClick: () -> Unit,
    moveClick: () -> Unit,
    dismissClick: () -> Unit,
    organizationSelect: (VaultMoveToOrganizationState.ViewState.Content.Organization) -> Unit,
    collectionSelect: (VaultCollection) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    when (val dialog = state.dialogState) {
        is VaultMoveToOrganizationState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.an_error_has_occurred),
                message = dialog.message(),
                onDismissRequest = dismissClick,
                throwable = dialog.throwable,
            )
        }

        is VaultMoveToOrganizationState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = dialog.message())
        }

        null -> Unit
    }
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = state.appBarText(),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                navigationIconContentDescription = stringResource(id = BitwardenString.close),
                onNavigationIconClick = closeClick,
                actions = {
                    BitwardenTextButton(
                        label = state.appBarButtonText(),
                        onClick = moveClick,
                        isEnabled = state.viewState is
                            VaultMoveToOrganizationState.ViewState.Content,
                        modifier = Modifier.testTag("MoveButton"),
                    )
                },
            )
        },
    ) {
        val modifier = Modifier
            .fillMaxSize()
        when (state.viewState) {
            is VaultMoveToOrganizationState.ViewState.Content -> {
                VaultMoveToOrganizationContent(
                    state = state.viewState,
                    showOnlyCollections = state.onlyShowCollections,
                    organizationSelect = organizationSelect,
                    collectionSelect = collectionSelect,
                    modifier = modifier,
                )
            }

            is VaultMoveToOrganizationState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = state.viewState.message(),
                    modifier = modifier,
                )
            }

            is VaultMoveToOrganizationState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = modifier)
            }

            is VaultMoveToOrganizationState.ViewState.Empty -> {
                VaultMoveToOrganizationEmpty(modifier = modifier)
            }
        }
    }
}
