package com.x8bit.bitwarden.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.bitwarden.network.model.OrganizationType
import com.bitwarden.network.util.parseJwtTokenDataOrNull
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.credentials.model.Fido2CredentialAssertionRequest
import com.x8bit.bitwarden.data.credentials.model.GetCredentialsRequest
import com.x8bit.bitwarden.data.credentials.model.ProviderGetPasswordCredentialRequest
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import com.x8bit.bitwarden.ui.tools.feature.send.model.SendItemType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages root level navigation state of the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    specialCircumstanceManager: SpecialCircumstanceManager,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState.Splash,
) {
    init {
        combine(
            authRepository.authStateFlow,
            authRepository.userStateFlow,
            specialCircumstanceManager.specialCircumstanceStateFlow,
        ) { authState, userState, specialCircumstance ->
            RootNavAction.Internal.UserStateUpdateReceive(
                authState = authState,
                userState = userState,
                specialCircumstance = specialCircumstance,
            )
        }
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            is RootNavAction.Internal.UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
        }
    }

    @Suppress("CyclomaticComplexMethod", "MaxLineLength", "LongMethod")
    private fun handleUserStateUpdateReceive(
        action: RootNavAction.Internal.UserStateUpdateReceive,
    ) {
        val userState = action.userState
        val specialCircumstance = action.specialCircumstance
        val updatedRootNavState = when {
            userState?.activeAccount?.trustedDevice?.isDeviceTrusted == false &&
                authRepository.tdeLoginComplete != true &&
                !userState.activeAccount.isVaultUnlocked -> RootNavState.TrustedDevice

            userState?.activeAccount?.needsMasterPassword == true -> RootNavState.SetPassword

            userState?.activeAccount?.needsPasswordReset == true -> RootNavState.ResetPassword

            specialCircumstance is SpecialCircumstance.RegistrationEvent -> {
                getRegistrationEventNavState(specialCircumstance)
            }

            userState == null ||
                !userState.activeAccount.isLoggedIn ||
                userState.hasPendingAccountAddition -> {
                if (authRepository.showWelcomeCarousel) {
                    RootNavState.AuthWithWelcome
                } else {
                    RootNavState.Auth
                }
            }

            userState.activeAccount.isVaultUnlocked &&
                userState.shouldShowRemovePassword(authState = action.authState) -> {
                RootNavState.RemovePassword
            }

            userState.activeAccount.isVaultUnlocked &&
                userState.activeAccount.onboardingStatus != OnboardingStatus.COMPLETE -> {
                when (userState.activeAccount.onboardingStatus) {
                    OnboardingStatus.NOT_STARTED,
                    OnboardingStatus.ACCOUNT_LOCK_SETUP,
                        -> RootNavState.OnboardingAccountLockSetup

                    OnboardingStatus.AUTOFILL_SETUP -> RootNavState.OnboardingAutoFillSetup
                    OnboardingStatus.FINAL_STEP -> RootNavState.OnboardingStepsComplete
                    OnboardingStatus.COMPLETE -> throw IllegalStateException("Should not have entered here.")
                }
            }

            userState.activeAccount.isVaultUnlocked -> {
                when (specialCircumstance) {
                    is SpecialCircumstance.AutofillSave -> {
                        RootNavState.VaultUnlockedForAutofillSave(
                            autofillSaveItem = specialCircumstance.autofillSaveItem,
                        )
                    }

                    is SpecialCircumstance.AutofillSelection -> {
                        RootNavState.VaultUnlockedForAutofillSelection(
                            activeUserId = userState.activeAccount.userId,
                            type = specialCircumstance.autofillSelectionData.type,
                        )
                    }

                    is SpecialCircumstance.AddTotpLoginItem -> {
                        RootNavState.VaultUnlockedForNewTotp(
                            activeUserId = userState.activeAccount.userId,
                        )
                    }

                    is SpecialCircumstance.ShareNewSend -> {
                        RootNavState.VaultUnlockedForNewSend(
                            sendType = when (specialCircumstance.data) {
                                is IntentManager.ShareData.FileSend -> SendItemType.FILE
                                is IntentManager.ShareData.TextSend -> SendItemType.TEXT
                            },
                        )
                    }

                    is SpecialCircumstance.PasswordlessRequest -> {
                        RootNavState.VaultUnlockedForAuthRequest
                    }

                    is SpecialCircumstance.ProviderCreateCredential -> {
                        RootNavState.VaultUnlockedForFido2Save(
                            activeUserId = userState.activeUserId,
                            createCredentialRequest =
                                specialCircumstance.createCredentialRequest,
                        )
                    }

                    is SpecialCircumstance.Fido2Assertion -> {
                        RootNavState.VaultUnlockedForFido2Assertion(
                            activeUserId = userState.activeUserId,
                            fido2CredentialAssertionRequest =
                                specialCircumstance.fido2AssertionRequest,
                        )
                    }

                    is SpecialCircumstance.ProviderGetPasswordRequest -> {
                        RootNavState.VaultUnlockedForPasswordGet(
                            activeUserId = userState.activeUserId,
                            providerGetPasswordCredentialRequest =
                                specialCircumstance.passwordGetRequest,
                        )
                    }

                    is SpecialCircumstance.ProviderGetCredentials -> {
                        RootNavState.VaultUnlockedForProviderGetCredentials(
                            activeUserId = userState.activeUserId,
                            getCredentialsRequest =
                                specialCircumstance.getCredentialsRequest,
                        )
                    }

                    SpecialCircumstance.AccountSecurityShortcut,
                    SpecialCircumstance.GeneratorShortcut,
                    SpecialCircumstance.VaultShortcut,
                    SpecialCircumstance.SendShortcut,
                    is SpecialCircumstance.SearchShortcut,
                    SpecialCircumstance.VerificationCodeShortcut,
                    null,
                        -> RootNavState.VaultUnlocked(activeUserId = userState.activeAccount.userId)

                    is SpecialCircumstance.RegistrationEvent -> {
                        throw IllegalStateException(
                            "Special circumstance should have been already handled.",
                        )
                    }
                }
            }

            else -> RootNavState.VaultLocked
        }
        mutableStateFlow.update { updatedRootNavState }
    }

    private fun getRegistrationEventNavState(
        registrationEvent: SpecialCircumstance.RegistrationEvent,
    ): RootNavState = when (registrationEvent) {
        is SpecialCircumstance.RegistrationEvent.CompleteRegistration -> {
            RootNavState.CompleteOngoingRegistration(
                email = registrationEvent.completeRegistrationData.email,
                verificationToken = registrationEvent.completeRegistrationData.verificationToken,
                fromEmail = registrationEvent.completeRegistrationData.fromEmail,
                timestamp = registrationEvent.timestamp,
            )
        }

        SpecialCircumstance.RegistrationEvent.ExpiredRegistrationLink -> {
            RootNavState.ExpiredRegistrationLink
        }
    }

    private fun UserState.shouldShowRemovePassword(authState: AuthState): Boolean {
        val isLoggedInUsingSso = (authState as? AuthState.Authenticated)
            ?.accessToken
            ?.let(::parseJwtTokenDataOrNull)
            ?.isExternal == true
        val usesKeyConnectorAndNotAdmin = this.activeAccount.organizations.any {
            it.shouldUseKeyConnector &&
                it.role != OrganizationType.OWNER &&
                it.role != OrganizationType.ADMIN
        }
        val userIsNotUsingKeyConnector = !this.activeAccount.isUsingKeyConnector
        return isLoggedInUsingSso && usesKeyConnectorAndNotAdmin && userIsNotUsingKeyConnector
    }
}

/**
 * Models root level destinations for the app.
 */
sealed class RootNavState : Parcelable {
    /**
     * App should show auth nav graph.
     */
    @Parcelize
    data object Auth : RootNavState()

    /**
     * App should show auth nav graph starting with the welcome carousel.
     */
    @Parcelize
    data object AuthWithWelcome : RootNavState()

    /**
     * App should show remove password graph.
     */
    @Parcelize
    data object RemovePassword : RootNavState()

    /**
     * App should show reset password graph.
     */
    @Parcelize
    data object ResetPassword : RootNavState()

    /**
     * App should show set password graph.
     */
    @Parcelize
    data object SetPassword : RootNavState()

    /**
     * App should show splash nav graph.
     */
    @Parcelize
    data object Splash : RootNavState()

    /**
     * App should show the trusted device destination.
     */
    @Parcelize
    data object TrustedDevice : RootNavState()

    /**
     * App should show vault locked nav graph.
     */
    @Parcelize
    data object VaultLocked : RootNavState()

    /**
     * App should show vault unlocked nav graph for the given [activeUserId].
     */
    @Parcelize
    data class VaultUnlocked(
        val activeUserId: String,
    ) : RootNavState()

    /**
     * App should show an add item screen for a user to complete the saving of data collected by
     * the autofill framework.
     */
    @Parcelize
    data class VaultUnlockedForAutofillSave(
        val autofillSaveItem: AutofillSaveItem,
    ) : RootNavState()

    /**
     * App should show a selection screen for autofill for an unlocked user.
     */
    @Parcelize
    data class VaultUnlockedForAutofillSelection(
        val activeUserId: String,
        val type: AutofillSelectionData.Type,
    ) : RootNavState()

    /**
     * App should show an add item screen for a user to complete the saving of data collected by
     * the fido2 credential manager framework
     *
     * @param activeUserId ID of the active user. Indirectly used to notify [RootNavViewModel] the
     * active user has changed.
     * @param createCredentialRequest System request containing FIDO credential data.
     */
    @Parcelize
    data class VaultUnlockedForFido2Save(
        val activeUserId: String,
        val createCredentialRequest: CreateCredentialRequest,
    ) : RootNavState()

    /**
     * App should perform FIDO 2 credential assertion for the user.
     */
    @Parcelize
    data class VaultUnlockedForFido2Assertion(
        val activeUserId: String,
        val fido2CredentialAssertionRequest: Fido2CredentialAssertionRequest,
    ) : RootNavState()

    /**
     * App should retrieve the requested credential from the provided user's vault.
     */
    @Parcelize
    data class VaultUnlockedForPasswordGet(
        val activeUserId: String,
        val providerGetPasswordCredentialRequest: ProviderGetPasswordCredentialRequest,
    ) : RootNavState()

    /**
     * App should unlock the user's vault and retrieve credentials matching the given request.
     */
    @Parcelize
    data class VaultUnlockedForProviderGetCredentials(
        val activeUserId: String,
        val getCredentialsRequest: GetCredentialsRequest,
    ) : RootNavState()

    /**
     * App should show the new verification codes listing screen for an unlocked user.
     */
    @Parcelize
    data class VaultUnlockedForNewTotp(
        val activeUserId: String,
    ) : RootNavState()

    /**
     * App should show the new send screen for an unlocked user.
     */
    @Parcelize
    data class VaultUnlockedForNewSend(
        val sendType: SendItemType,
    ) : RootNavState()

    /**
     * App should show the screen to complete an ongoing registration process.
     */
    @Parcelize
    data class CompleteOngoingRegistration(
        val email: String,
        val verificationToken: String,
        val fromEmail: Boolean,
        val timestamp: Long,
    ) : RootNavState()

    /**
     * App should show the auth confirmation screen for an unlocked user.
     */
    @Parcelize
    data object VaultUnlockedForAuthRequest : RootNavState()

    /**
     * App should show the expired registration link screen.
     */
    @Parcelize
    data object ExpiredRegistrationLink : RootNavState()

    /**
     * App should show the set up account lock onboarding screen.
     */
    @Parcelize
    data object OnboardingAccountLockSetup : RootNavState()

    /**
     * App should show the set up autofill onboarding screen.
     */
    @Parcelize
    data object OnboardingAutoFillSetup : RootNavState()

    /**
     * App should show the onboarding steps complete screen.
     */
    @Parcelize
    data object OnboardingStepsComplete : RootNavState()
}

/**
 * Models root level navigation actions.
 */
sealed class RootNavAction {

    /**
     * Internal ViewModel actions.
     */
    sealed class Internal {

        /**
         * User state in the repository layer changed.
         */
        data class UserStateUpdateReceive(
            val authState: AuthState,
            val userState: UserState?,
            val specialCircumstance: SpecialCircumstance?,
        ) : RootNavAction()
    }
}
