package com.x8bit.bitwarden.ui.auth.feature.expiredregistrationlink

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.appbar.NavigationIcon
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.components.scaffold.BitwardenScaffold

/**
 * Top level screen component for the ExpiredRegistrationLink screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiredRegistrationLinkScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
    viewModel: ExpiredRegistrationLinkViewModel = hiltViewModel(),
) {
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            ExpiredRegistrationLinkEvent.NavigateBack -> onNavigateBack()
            ExpiredRegistrationLinkEvent.NavigateToLogin -> onNavigateToLogin()
            ExpiredRegistrationLinkEvent.NavigateToStartRegistration -> {
                onNavigateToStartRegistration()
            }
        }
    }
    val sendCloseClicked = remember(viewModel) {
        {
            viewModel.trySendAction(ExpiredRegistrationLinkAction.CloseClicked)
        }
    }

    BackHandler(onBack = sendCloseClicked)

    BitwardenScaffold(
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.create_account),
                scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(),
                navigationIcon = NavigationIcon(
                    navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_close),
                    navigationIconContentDescription = stringResource(id = BitwardenString.close),
                    onNavigationIconClick = sendCloseClicked,
                ),
            )
        },
    ) {
        ExpiredRegistrationLinkContent(
            onNavigateToLogin = remember(viewModel) {
                {
                    viewModel.trySendAction(ExpiredRegistrationLinkAction.GoToLoginClicked)
                }
            },
            onNavigateToStartRegistration = remember(viewModel) {
                {
                    viewModel.trySendAction(
                        ExpiredRegistrationLinkAction.RestartRegistrationClicked,
                    )
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        )
    }
}

@Composable
private fun ExpiredRegistrationLinkContent(
    onNavigateToLogin: () -> Unit,
    onNavigateToStartRegistration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(BitwardenString.expired_link),
            style = BitwardenTheme.typography.titleMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(BitwardenString.please_restart_registration_or_try_logging_in),
            style = BitwardenTheme.typography.bodyMedium,
            color = BitwardenTheme.colorScheme.text.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(24.dp))
        BitwardenFilledButton(
            label = stringResource(BitwardenString.restart_registration),
            onClick = onNavigateToStartRegistration,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.height(12.dp))
        BitwardenOutlinedButton(
            label = stringResource(id = BitwardenString.log_in_verb),
            onClick = onNavigateToLogin,
            modifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Preview(showBackground = true)
@Composable
private fun ExpiredRegistrationLinkScreen_preview() {
    BitwardenTheme {
        ExpiredRegistrationLinkContent(
            onNavigateToLogin = {},
            onNavigateToStartRegistration = {},
        )
    }
}
