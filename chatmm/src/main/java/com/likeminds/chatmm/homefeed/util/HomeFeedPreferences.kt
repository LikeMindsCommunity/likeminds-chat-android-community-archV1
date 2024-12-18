package com.likeminds.chatmm.homefeed.util

import android.content.Context
import com.likeminds.chatmm.utils.sharedpreferences.BasePreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HomeFeedPreferences @Inject constructor(
    context: Context,
) : BasePreferences(HOME_FEED_PREFS, context) {
    companion object {
        const val HOME_FEED_PREFS = "home_feed_prefs"

        private const val SHOW_HOME_FEED_SHIMMER = "SHOW_HOME_FEED_SHIMMER"
        private const val SHOW_DM_FEED_SHIMMER = "SHOW_DM_FEED_SHIMMER"
    }

    fun setShowHomeFeedShimmer(setHomeFeedShimmer: Boolean) {
        putPreference(SHOW_HOME_FEED_SHIMMER, setHomeFeedShimmer)
    }

    fun getShowHomeFeedShimmer(): Boolean {
        return getPreference(SHOW_HOME_FEED_SHIMMER, false)
    }

    fun setShowDMFeedShimmer(setDMFeedShimmer: Boolean) {
        putPreference(SHOW_DM_FEED_SHIMMER, setDMFeedShimmer)
    }

    fun getShowDMFeedShimmer(): Boolean {
        return getPreference(SHOW_DM_FEED_SHIMMER, false)
    }
}