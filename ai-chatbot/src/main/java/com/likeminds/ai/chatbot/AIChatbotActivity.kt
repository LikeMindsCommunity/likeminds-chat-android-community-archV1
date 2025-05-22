package com.likeminds.ai.chatbot

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

        setContentView(R.layout.activity_ai_chatbot)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById<ConstraintLayout>(R.id.main)) { view, windowInsets ->
            val innerPadding = windowInsets.getInsets(
                // Notice we're using systemBars, not statusBar
                WindowInsetsCompat.Type.systemBars()
                        // Notice we're also accounting for the display cutouts
                        or WindowInsetsCompat.Type.displayCutout()
                        // If using EditText, also add
                        or WindowInsetsCompat.Type.ime()
            )
            // Apply the insets as padding to the view. Here, set all the dimensions
            // as appropriate to your layout. You can also update the view's margin if
            // more appropriate.
            view.setPadding(0, innerPadding.top, 0, innerPadding.bottom)

            // Return CONSUMED if you don't want the window insets to keep passing down
            // to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        findViewById<LMChatAIBotButton>(R.id.btn_ai_chatbot).setChatAIButtonProps(
            LMChatAIBotButtonProps.Builder()
                .uuid(authPreferences.getUserId())
                .apiKey(authPreferences.getApiKey())
                .userName(authPreferences.getUserName())
                .build()
        )
    }
}