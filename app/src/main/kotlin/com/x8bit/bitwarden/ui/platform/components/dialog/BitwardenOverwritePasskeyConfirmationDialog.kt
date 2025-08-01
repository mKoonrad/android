package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * A reusable dialog for confirming whether or not the user wants to overwrite an existing FIDO 2
 * credential.
 *
 * @param onConfirmClick A callback for when the overwrite confirmation button is clicked.
 * @param onDismissRequest A callback for when the dialog is requesting dismissal.
 */
@Suppress("MaxLineLength")
@Composable
fun BitwardenOverwritePasskeyConfirmationDialog(
    onConfirmClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BitwardenTwoButtonDialog(
        title = stringResource(id = BitwardenString.overwrite_passkey),
        message = stringResource(id = BitwardenString.this_item_already_contains_a_passkey_are_you_sure_you_want_to_overwrite_the_current_passkey),
        confirmButtonText = stringResource(id = BitwardenString.okay),
        dismissButtonText = stringResource(id = BitwardenString.cancel),
        onConfirmClick = onConfirmClick,
        onDismissClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    )
}
