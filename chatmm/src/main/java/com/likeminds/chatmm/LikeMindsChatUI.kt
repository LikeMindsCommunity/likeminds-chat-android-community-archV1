package com.likeminds.chatmm

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.chatmm.theme.model.LMChatTheme
import com.likeminds.chatmm.chat.model.SDKInitiateSource
import com.likeminds.chatmm.chat.model.LMChatExtras
import com.likeminds.chatmm.chat.view.LMChatFragment
import com.likeminds.chatmm.homefeed.view.HomeFeedFragment

object LikeMindsChatUI {
    /**
     * Call this function to configure SDK in client's app
     *
     * @param application: application instance of client's app
     * @param chatTheme: [LMChatTheme] object from client
     **/
    fun initiateChatUI(
        application: Application,
        lmUICallback: LMUICallback? = null,
        chatTheme: LMChatTheme? = null
    ) {
        Log.d(SDKApplication.LOG_TAG, "initiate LikeMindsChatUI called")

        //create object of SDKApplication
        val sdk = SDKApplication.getInstance()

        //call initSDKApplication to initialise sdk
        sdk.initSDKApplication(
            application,
            lmUICallback,
            chatTheme
        )
    }

    fun initiateHomeFeed(
        activity: AppCompatActivity,
        containerViewId: Int,
        apiKey: String,
        userName: String,
        userId: String? = null,
        isGuest: Boolean? = false,
    ) {
        Log.d(SDKApplication.LOG_TAG, "initiate group chat called")

        val extra = LMChatExtras.Builder()
            .userName(userName)
            .userId(userId)
            .isGuest(isGuest)
            .apiKey(apiKey)
            .sdkInitiateSource(SDKInitiateSource.HOME_FEED)
            .build()

        val fragment = HomeFeedFragment.getInstance(extra)

        val transaction = activity.supportFragmentManager.beginTransaction()
        transaction.replace(containerViewId, fragment, containerViewId.toString())
        transaction.setReorderingAllowed(true)
        Log.d(SDKApplication.LOG_TAG, "showing group chat")
        transaction.commitNowAllowingStateLoss()
    }

    fun initiateChatFragment(
        activity: AppCompatActivity,
        containerViewId: Int,
        apiKey: String,
        userName: String,
        userId: String? = null,
        isGuest: Boolean? = false,
    ) {
        Log.d(SDKApplication.LOG_TAG, "initiate chat fragment called")

        val extra = LMChatExtras.Builder()
            .userName(userName)
            .userId(userId)
            .isGuest(isGuest)
            .apiKey(apiKey)
            .sdkInitiateSource(SDKInitiateSource.CHAT_FRAGMENT)
            .build()

        val fragment = LMChatFragment.getInstance(extra)

        val transaction = activity.supportFragmentManager.beginTransaction()
        transaction.replace(containerViewId, fragment, containerViewId.toString())
        transaction.setReorderingAllowed(true)
        Log.d(SDKApplication.LOG_TAG, "showing chat fragment")
        transaction.commitNowAllowingStateLoss()
    }

    fun setTheme(chatTheme: LMChatTheme) {
        val sdk = SDKApplication.getInstance()
        sdk.setupTheme(chatTheme)
    }
}