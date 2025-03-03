package com.likeminds.community.hybrid.chat

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.likeminds.chatmm.*
import com.likeminds.chatmm.theme.model.LMChatAppearanceRequest
import com.likeminds.chatmm.theme.model.LMFonts
import com.likeminds.community.hybrid.chat.auth.util.AuthPreferences
import com.likeminds.likemindschat.conversation.model.ConversationState

class CommunityHybridChatApplication : Application(), LMChatCoreCallback {

    private lateinit var authPreferences: AuthPreferences

    companion object {
        const val EXTRA_LOGIN = "extra of login"
        const val LM_CHAT_EXAMPLE_TAG = "ExampleTag"
    }

    override fun onCreate() {
        super.onCreate()

        authPreferences = AuthPreferences(this)

        val lmChatAppearanceRequest = LMChatAppearanceRequest.Builder()
            .headerColor(authPreferences.getHeaderColor())
            .buttonsColor(authPreferences.getButtonColor())
            .textLinkColor(authPreferences.getTextLinkColor())
            .fonts(
                LMFonts.Builder()
                    .bold(com.likeminds.chatmm.R.font.lm_chat_roboto_bold)
                    .medium(com.likeminds.chatmm.R.font.lm_chat_roboto_regular)
                    .regular(com.likeminds.chatmm.R.font.lm_chat_roboto_regular)
                    .build()
            )
            .build()

        LMChatCore.setup(
            application = this,
            theme = LMChatTheme.COMMUNITY_HYBRID_CHAT,
            lmChatCoreCallback = this,
            lmChatAppearanceRequest = lmChatAppearanceRequest,
            domain = deviceId(),
            enablePushNotifications = true,
            deviceId = deviceId(),
            shareLogsWithLM = true,
            excludeConversationStates = listOf(
                ConversationState.MEMBER_LEFT_OPEN_CHATROOM,
                ConversationState.MEMBER_JOINED_OPEN_CHATROOM,
                ConversationState.MEMBER_ADDED_TO_CHATROOM
            )
        )
    }

    @SuppressLint("HardwareIds")
    fun deviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: ""
    }
}