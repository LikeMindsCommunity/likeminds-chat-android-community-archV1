package com.likeminds.chatmm.utils.sharedpreferences

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.likeminds.chatmm.SDKApplication

object LMChatMasterPrefUtils {
    fun clearAllPrefs(context: Context) {
        val masterPref: SharedPreferences =
            context.getSharedPreferences(
                BasePreferences.MASTER_PREF,
                Context.MODE_PRIVATE
            )
        val listOfPreferences =
            masterPref.getStringSet(BasePreferences.ALL_PREFS_SET, null)
                ?.toMutableList()
        if (!listOfPreferences.isNullOrEmpty()) {
            for (i in listOfPreferences.indices) {
                // clear each preference file
                val preference = listOfPreferences[i]
                Log.d(SDKApplication.LOG_TAG, "clearing prefs: $preference")
                context.getSharedPreferences(preference, Context.MODE_PRIVATE)
                    .edit().clear().apply()
            }
        }
    }
}