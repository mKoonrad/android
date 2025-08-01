package com.bitwarden.authenticator.ui.authenticator.feature.search

import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.authenticator.ui.authenticator.feature.search.handlers.SearchHandlers
import com.bitwarden.authenticator.ui.platform.components.appbar.AuthenticatorSearchTopAppBar
import com.bitwarden.authenticator.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.bottomDivider
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * The search screen for authenticator items.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSearchScreen(
    viewModel: ItemSearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val searchHandlers = remember(viewModel) { SearchHandlers.create(viewModel) }
    val context = LocalContext.current
    val resources = context.resources

    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            is ItemSearchEvent.NavigateBack -> onNavigateBack()
            is ItemSearchEvent.ShowToast -> {
                Toast
                    .makeText(context, event.message(resources), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AuthenticatorSearchTopAppBar(
                modifier = Modifier
                    .semantics { testTag = "SearchFieldEntry" }
                    .bottomDivider(),
                searchTerm = state.searchTerm,
                placeholder = stringResource(id = BitwardenString.search_codes),
                onSearchTermChange = searchHandlers.onSearchTermChange,
                scrollBehavior = scrollBehavior,
                navigationIcon = NavigationIcon(
                    navigationIcon = painterResource(id = BitwardenDrawable.ic_back),
                    navigationIconContentDescription = stringResource(id = BitwardenString.back),
                    onNavigationIconClick = searchHandlers.onBackClick,
                ),
            )
        },
    ) { innerPadding ->
        when (val viewState = state.viewState) {
            is ItemSearchState.ViewState.Content -> {
                ItemSearchContent(
                    viewState = viewState,
                    searchHandlers = searchHandlers,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding),
                )
            }

            is ItemSearchState.ViewState.Empty -> {
                ItemSearchEmptyContent(
                    viewState = viewState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = innerPadding),
                )
            }
        }
    }
}
