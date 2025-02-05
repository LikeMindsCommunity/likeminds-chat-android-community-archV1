package com.likeminds.chatmm.aichatbot.util

import android.content.Context
import com.likeminds.chatmm.utils.sharedpreferences.BasePreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LMChatAIChatBotPreferences @Inject constructor(
    context: Context
) : BasePreferences(AI_CHATBOT_PREFS, context) {

    companion object {
        const val AI_CHATBOT_PREFS = "ai_chatbot_prefs"
        const val CHATROOM_ID_WITH_AI_CHATBOT = "chatroom_id_with_ai_chatbot"
    }

    fun getChatroomIDWithAIChatbot(): String {
        return getPreference(CHATROOM_ID_WITH_AI_CHATBOT, "") ?: ""
    }

    fun setChatroomIDWithAIChatbot(chatroomIdWithAIChatbot: String) {
        putPreference(CHATROOM_ID_WITH_AI_CHATBOT, chatroomIdWithAIChatbot)
    }
}