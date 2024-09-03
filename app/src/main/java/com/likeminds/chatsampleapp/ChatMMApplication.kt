package com.likeminds.chatsampleapp

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.LMChatCoreCallback
import com.likeminds.chatmm.theme.model.LMChatTheme
import com.likeminds.chatmm.theme.model.LMFonts
import com.likeminds.chatsampleapp.auth.util.AuthPreferences

class ChatMMApplication : Application(), LMChatCoreCallback {

    private lateinit var authPreferences: AuthPreferences

    companion object {
        const val EXTRA_LOGIN = "extra of login"
        const val LM_CHAT_EXAMPLE_TAG = "ExampleTag"
    }

    override fun onCreate() {
        super.onCreate()

        authPreferences = AuthPreferences(this)

        val chatTheme = LMChatTheme.Builder()
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
            lmChatCoreCallback = this,
            theme = chatTheme,
            domain = deviceId(),
            enablePushNotifications = true,
            deviceId = deviceId()
        )
    }

    @SuppressLint("HardwareIds")
    fun deviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: ""
    }
}