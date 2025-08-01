@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.platform.feature.search.util

import androidx.annotation.DrawableRes
import com.bitwarden.core.data.repository.util.SpecialCharWithPrecedenceComparator
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.base.util.removeDiacritics
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.ui.platform.feature.search.SearchState
import com.x8bit.bitwarden.ui.platform.feature.search.SearchTypeData
import com.x8bit.bitwarden.ui.platform.feature.search.model.AutofillSelectionOption
import com.x8bit.bitwarden.ui.tools.feature.send.util.toLabelIcons
import com.x8bit.bitwarden.ui.tools.feature.send.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.util.toLabelIcons
import com.x8bit.bitwarden.ui.vault.feature.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import com.x8bit.bitwarden.ui.vault.util.toSdkCipherType
import java.time.Clock
import java.time.format.FormatStyle

/**
 * Updates a [SearchTypeData] with the given data if necessary.
 */
fun SearchTypeData.updateWithAdditionalDataIfNecessary(
    folderList: List<FolderView>,
    collectionList: List<CollectionView>,
): SearchTypeData =
    when (this) {
        is SearchTypeData.Vault.Collection -> copy(
            collectionName = collectionList
                .find { it.id == collectionId }
                ?.name
                .orEmpty(),
        )

        is SearchTypeData.Vault.Folder -> copy(
            folderName = folderList
                .find { it.id == folderId }
                ?.name
                .orEmpty(),
        )

        SearchTypeData.Sends.All -> this
        SearchTypeData.Sends.Files -> this
        SearchTypeData.Sends.Texts -> this
        SearchTypeData.Vault.All -> this
        SearchTypeData.Vault.Cards -> this
        SearchTypeData.Vault.Identities -> this
        SearchTypeData.Vault.Logins -> this
        SearchTypeData.Vault.NoFolder -> this
        SearchTypeData.Vault.SecureNotes -> this
        SearchTypeData.Vault.Trash -> this
        SearchTypeData.Vault.VerificationCodes -> this
        SearchTypeData.Vault.SshKeys -> this
    }

/**
 * The semantic test tag to use for the search item.
 */
val SearchTypeData.searchItemTestTag: String
    get() = when (this) {
        is SearchTypeData.Sends -> "SendCell"
        is SearchTypeData.Vault -> "CipherCell"
    }

/**
 * Filters out any [CipherView]s that do not adhere to the [searchTypeData] and [searchTerm] and
 * sorts the remaining items.
 */
fun List<CipherListView>.filterAndOrganize(
    searchTypeData: SearchTypeData.Vault,
    searchTerm: String,
): List<CipherListView> =
    if (searchTerm.isBlank()) {
        emptyList()
    } else {
        this
            .filter { it.filterBySearchType(searchTypeData) }
            .groupBy { it.matchedSearch(searchTerm) }
            .flatMap { (priority, sends) ->
                when (priority) {
                    SortPriority.HIGH -> sends.sortedBy { it.name }
                    SortPriority.LOW -> sends.sortedBy { it.name }
                    null -> emptyList()
                }
            }
    }

/**
 * Determines a predicate to filter a list of [SendView] based on the [SearchTypeData.Sends].
 */
private fun CipherListView.filterBySearchType(
    searchTypeData: SearchTypeData.Vault,
): Boolean =
    when (searchTypeData) {
        SearchTypeData.Vault.All -> deletedDate == null
        is SearchTypeData.Vault.Cards -> type is CipherListViewType.Card && deletedDate == null
        is SearchTypeData.Vault.Collection -> {
            searchTypeData.collectionId in this.collectionIds && deletedDate == null
        }

        is SearchTypeData.Vault.Folder -> folderId == searchTypeData.folderId && deletedDate == null
        SearchTypeData.Vault.NoFolder -> folderId == null && deletedDate == null
        is SearchTypeData.Vault.Identities -> {
            type is CipherListViewType.Identity && deletedDate == null
        }

        is SearchTypeData.Vault.Logins -> type is CipherListViewType.Login && deletedDate == null
        is SearchTypeData.Vault.SecureNotes -> {
            type is CipherListViewType.SecureNote && deletedDate == null
        }

        is SearchTypeData.Vault.SshKeys -> type is CipherListViewType.SshKey && deletedDate == null
        is SearchTypeData.Vault.VerificationCodes -> login?.totp != null && deletedDate == null
        is SearchTypeData.Vault.Trash -> deletedDate != null
    }

/**
 * Determines the priority of a given [CipherView] based on the [searchTerm]. Null indicates that
 * this item should be removed from the list.
 */
@Suppress("MagicNumber")
private fun CipherListView.matchedSearch(searchTerm: String): SortPriority? {
    val term = searchTerm.removeDiacritics()
    val cipherName = name.removeDiacritics()
    val cipherId = id?.takeIf { term.length > 8 }.orEmpty().removeDiacritics()
    val cipherSubtitle = subtitle.removeDiacritics()
    val cipherUris = (this.type as? CipherListViewType.Login)
        ?.v1
        ?.uris
        .orEmpty()
        .map { it.uri.orEmpty().removeDiacritics() }
    return when {
        cipherName.contains(other = term, ignoreCase = true) -> SortPriority.HIGH
        cipherId.contains(other = term, ignoreCase = true) -> SortPriority.LOW
        cipherSubtitle.contains(other = term, ignoreCase = true) -> SortPriority.LOW
        cipherUris.any { it.contains(other = term, ignoreCase = true) } -> SortPriority.LOW
        else -> null
    }
}

/**
 * Transforms a list of [CipherView] into [SearchState.ViewState].
 */
@Suppress("LongParameterList")
fun List<CipherListView>.toViewState(
    searchTerm: String,
    baseIconUrl: String,
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
    isPremiumUser: Boolean,
): SearchState.ViewState =
    when {
        searchTerm.isEmpty() -> SearchState.ViewState.Empty(message = null)
        isNotEmpty() -> {
            SearchState.ViewState.Content(
                displayItems = toDisplayItemList(
                    baseIconUrl = baseIconUrl,
                    hasMasterPassword = hasMasterPassword,
                    isIconLoadingDisabled = isIconLoadingDisabled,
                    isAutofill = isAutofill,
                    isPremiumUser = isPremiumUser,
                )
                    .sortAlphabetically(),
            )
        }

        else -> {
            SearchState.ViewState.Empty(
                message = BitwardenString.there_are_no_items_that_match_the_search.asText(),
            )
        }
    }

private fun List<CipherListView>.toDisplayItemList(
    baseIconUrl: String,
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
    isPremiumUser: Boolean,
): List<SearchState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseIconUrl = baseIconUrl,
            hasMasterPassword = hasMasterPassword,
            isIconLoadingDisabled = isIconLoadingDisabled,
            isAutofill = isAutofill,
            isPremiumUser = isPremiumUser,
        )
    }

private fun CipherListView.toDisplayItem(
    baseIconUrl: String,
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
    isPremiumUser: Boolean,
): SearchState.DisplayItem =
    SearchState.DisplayItem(
        id = id.orEmpty(),
        title = name,
        titleTestTag = "CipherNameLabel",
        subtitle = subtitle,
        subtitleTestTag = "CipherSubTitleLabel",
        iconData = this.toIconData(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
        ),
        extraIconList = toLabelIcons(),
        overflowOptions = toOverflowActions(
            hasMasterPassword = hasMasterPassword,
            isPremiumUser = isPremiumUser,
        ),
        overflowTestTag = "CipherOptionsButton",
        totpCode = login?.totp,
        autofillSelectionOptions = AutofillSelectionOption
            .entries
            // Only valid for autofill
            .filter { isAutofill }
            // Only Login types get the save option
            .filter {
                this.login != null || (it != AutofillSelectionOption.AUTOFILL_AND_SAVE)
            },
        shouldDisplayMasterPasswordReprompt = hasMasterPassword &&
            reprompt == CipherRepromptType.PASSWORD,
        itemType = SearchState.DisplayItem.ItemType.Vault(type = this.type.toSdkCipherType()),
    )

private fun CipherListView.toIconData(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
): IconData =
    when (val cipherType = this.type) {
        is CipherListViewType.Login -> {
            cipherType.v1.uris.toLoginIconData(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
                usePasskeyDefaultIcon = this.isActiveWithFido2Credentials,
            )
        }

        else -> IconData.Local(iconRes = this.type.iconRes)
    }

@get:DrawableRes
private val CipherListViewType.iconRes: Int
    get() = when (this) {
        is CipherListViewType.Login -> BitwardenDrawable.ic_globe
        CipherListViewType.SecureNote -> BitwardenDrawable.ic_note
        is CipherListViewType.Card -> BitwardenDrawable.ic_payment_card
        CipherListViewType.Identity -> BitwardenDrawable.ic_id_card
        CipherListViewType.SshKey -> BitwardenDrawable.ic_ssh_key
    }

/**
 * Filters out any [SendView]s that do not adhere to the [searchTypeData] and [searchTerm] and
 * sorts the remaining items.
 */
fun List<SendView>.filterAndOrganize(
    searchTypeData: SearchTypeData.Sends,
    searchTerm: String,
): List<SendView> =
    if (searchTerm.isBlank()) {
        emptyList()
    } else {
        this
            .filter { it.filterBySearchType(searchTypeData) }
            .groupBy { it.matchedSearch(searchTerm) }
            .flatMap { (priority, sends) ->
                when (priority) {
                    SortPriority.HIGH -> sends.sortedBy { it.name }
                    SortPriority.LOW -> sends.sortedBy { it.name }
                    null -> emptyList()
                }
            }
    }

/**
 * Determines a predicate to filter a list of [SendView] based on the [SearchTypeData.Sends].
 */
private fun SendView.filterBySearchType(
    searchTypeData: SearchTypeData.Sends,
): Boolean =
    when (searchTypeData) {
        SearchTypeData.Sends.All -> true
        is SearchTypeData.Sends.Files -> type == SendType.FILE
        is SearchTypeData.Sends.Texts -> type == SendType.TEXT
    }

/**
 * Determines the priority of a given [SendView] based on the [searchTerm]. Null indicates that
 * this item should be removed from the list.
 */
private fun SendView.matchedSearch(searchTerm: String): SortPriority? {
    val term = searchTerm.removeDiacritics()
    val sendName = name.removeDiacritics()
    val sendText = text?.text.orEmpty().removeDiacritics()
    val sendFileName = file?.fileName.orEmpty().removeDiacritics()
    return when {
        sendName.contains(other = term, ignoreCase = true) -> SortPriority.HIGH
        sendText.contains(other = term, ignoreCase = true) -> SortPriority.LOW
        sendFileName.contains(other = term, ignoreCase = true) -> SortPriority.LOW
        else -> null
    }
}

/**
 * Transforms a list of [SendView] into [SearchState.ViewState].
 */
fun List<SendView>.toViewState(
    searchTerm: String,
    baseWebSendUrl: String,
    clock: Clock,
): SearchState.ViewState =
    when {
        searchTerm.isEmpty() -> SearchState.ViewState.Empty(message = null)
        isNotEmpty() -> {
            SearchState.ViewState.Content(
                displayItems = toDisplayItemList(
                    baseWebSendUrl = baseWebSendUrl,
                    clock = clock,
                )
                    .sortAlphabetically(),
            )
        }

        else -> {
            SearchState.ViewState.Empty(
                message = BitwardenString.there_are_no_items_that_match_the_search.asText(),
            )
        }
    }

private fun List<SendView>.toDisplayItemList(
    baseWebSendUrl: String,
    clock: Clock,
): List<SearchState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseWebSendUrl = baseWebSendUrl,
            clock = clock,
        )
    }

private fun SendView.toDisplayItem(
    baseWebSendUrl: String,
    clock: Clock,
): SearchState.DisplayItem =
    SearchState.DisplayItem(
        id = id.orEmpty(),
        title = name,
        titleTestTag = "SendNameLabel",
        subtitle = deletionDate.toFormattedDateTimeStyle(
            dateStyle = FormatStyle.MEDIUM,
            timeStyle = FormatStyle.SHORT,
            clock = clock,
        ),
        subtitleTestTag = "SendDateLabel",
        iconData = IconData.Local(
            iconRes = when (type) {
                SendType.TEXT -> BitwardenDrawable.ic_file_text
                SendType.FILE -> BitwardenDrawable.ic_file
            },
        ),
        extraIconList = toLabelIcons(clock = clock),
        overflowOptions = toOverflowActions(baseWebSendUrl = baseWebSendUrl),
        overflowTestTag = "SendOptionsButton",
        totpCode = null,
        autofillSelectionOptions = emptyList(),
        shouldDisplayMasterPasswordReprompt = false,
        itemType = SearchState.DisplayItem.ItemType.Sends(type = this.type),
    )

/**
 * Sort a list of [SearchState.DisplayItem] by their titles alphabetically giving digits and
 * special characters higher precedence.
 */
private fun List<SearchState.DisplayItem>.sortAlphabetically(): List<SearchState.DisplayItem> {
    return this.sortedWith { item1, item2 ->
        SpecialCharWithPrecedenceComparator.compare(item1.title, item2.title)
    }
}

private enum class SortPriority {
    HIGH,
    LOW,
}
