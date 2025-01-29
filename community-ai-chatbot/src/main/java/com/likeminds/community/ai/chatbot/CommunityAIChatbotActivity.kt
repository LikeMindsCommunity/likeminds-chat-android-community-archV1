package com.likeminds.community.ai.chatbot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.chatmm.aichatbot.LMChatAIButton
import com.likeminds.chatmm.aichatbot.model.LMChatAIButtonProps
import com.likeminds.community.ai.chatbot.auth.util.AuthPreferences

class CommunityAIChatbotActivity : AppCompatActivity() {

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_ai_chatbot)

        findViewById<LMChatAIButton>(R.id.btn_ai_chatbot).setChatAIButtonProps(
            LMChatAIButtonProps.Builder()
                .uuid(authPreferences.getUserId())
                .apiKey(authPreferences.getApiKey())
                .userName(authPreferences.getUserName())
                .build()
        )
    }
}