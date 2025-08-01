package com.x8bit.bitwarden.ui.platform.manager.review

import android.app.Activity
import android.widget.Toast
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.resource.BitwardenString
import com.google.android.play.core.review.ReviewManagerFactory
import com.x8bit.bitwarden.BuildConfig
import timber.log.Timber

/**
 * Default implementation of [AppReviewManager].
 */
@OmitFromCoverage
class AppReviewManagerImpl(
    private val activity: Activity,
) : AppReviewManager {
    override fun promptForReview() {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                manager.launchReviewFlow(activity, reviewInfo)
            } else {
                Timber.e(task.exception, "Failed to launch review flow.")
            }
        }
        if (BuildConfig.DEBUG) {
            Toast
                .makeText(
                    activity,
                    activity.getString(BitwardenString.review_flow_launched),
                    Toast.LENGTH_SHORT,
                )
                .show()
        }
    }
}
