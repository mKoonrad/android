package com.x8bit.bitwarden.ui.platform.manager.nfc

import android.app.Activity
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Build
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.x8bit.bitwarden.AuthCallbackActivity
import com.x8bit.bitwarden.data.autofill.util.toPendingIntentMutabilityFlag
import kotlin.random.Random

/**
 * The default implementation of the [NfcManager].
 */
@OmitFromCoverage
class NfcManagerImpl(
    private val activity: Activity,
) : NfcManager {
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(activity)

    private val supportsNfc: Boolean get() = nfcAdapter?.isEnabled == true

    override fun start() {
        if (!supportsNfc) return
        val options = ActivityOptions.makeBasic()
        if (isBuildVersionAtLeast(version = Build.VERSION_CODES.BAKLAVA)) {
            options.setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE,
            )
        } else if (isBuildVersionAtLeast(version = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)) {
            @Suppress("DEPRECATION")
            options.setPendingIntentCreatorBackgroundActivityStartMode(
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED,
            )
        }
        nfcAdapter?.enableForegroundDispatch(
            activity,
            PendingIntent.getActivity(
                activity,
                Random.nextInt(),
                Intent(activity, AuthCallbackActivity::class.java).addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
                PendingIntent.FLAG_UPDATE_CURRENT.toPendingIntentMutabilityFlag(),
                options.toBundle(),
            ),
            arrayOf(
                IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
                    // Register for all NDEF tags starting with http or https
                    addDataScheme("http")
                    addDataScheme("https")
                },
            ),
            null,
        )
    }

    override fun stop() {
        if (!supportsNfc) return
        nfcAdapter?.disableForegroundDispatch(activity)
    }
}
