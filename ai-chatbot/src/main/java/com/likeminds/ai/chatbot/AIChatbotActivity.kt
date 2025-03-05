package com.likeminds.ai.chatbot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.ai.chatbot.auth.util.AuthPreferences
import com.likeminds.chatmm.aichatbot.LMChatAIBotButton
import com.likeminds.chatmm.aichatbot.model.LMChatAIBotButtonProps
import com.likeminds.community.ai.chatbot.R

class AIChatbotActivity : AppCompatActivity() {

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Call before the DecorView is accessed in setContentView
        theme.applyStyle(com.likeminds.chatmm.R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)

        setContentView(R.layout.activity_ai_chatbot)

        findViewById<LMChatAIBotButton>(R.id.btn_ai_chatbot).setChatAIButtonProps(
            LMChatAIBotButtonProps.Builder()
                .uuid(authPreferences.getUserId())
                .apiKey(authPreferences.getApiKey())
                .userName(authPreferences.getUserName())
                .build()
        )
    }
}