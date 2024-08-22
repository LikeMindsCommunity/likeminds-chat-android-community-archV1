package com.likeminds.chatsampleapp

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.likeminds.chatmm.*
import com.likeminds.chatmm.theme.model.LMChatTheme
import com.likeminds.chatmm.theme.model.LMFonts
import com.likeminds.chatmm.utils.observer.ChatEvent
import com.likeminds.chatmm.widget.model.WidgetViewData
import com.likeminds.chatsampleapp.auth.util.AuthPreferences
import kotlinx.coroutines.runBlocking
import org.json.JSONObject

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
            domain = "",
            enablePushNotifications = true,
            deviceId = ""
        )
    }

    override fun login() {
        super.login()
        // override this function to trigger login.
    }

    //get widget data and modify the data and return updated data in ChatEvent
    override fun getWidgetCallback(widgetData: HashMap<String?, WidgetViewData?>) {
        val updatedWidgetData = HashMap<String?, WidgetViewData?>()

        widgetData.forEach {
            val conversationId = it.key
            var widgetViewData = it.value
            val metadata = JSONObject(widgetViewData?.metadata.toString())
            metadata.put("timestamp", System.currentTimeMillis())
            widgetViewData = widgetViewData?.toBuilder()?.metadata(metadata.toString())?.build()
            updatedWidgetData[conversationId] = widgetViewData
        }
        Handler(Looper.getMainLooper()).postDelayed({
            ChatEvent.getPublisher().notify(updatedWidgetData)
        }, 3000)
    }

    override fun onAccessTokenExpiredAndRefreshed(accessToken: String, refreshToken: String) {
        Log.d(
            SDKApplication.LOG_TAG, """
                    Example Layer -> onAccessTokenExpiredAndRefreshed
                    accessToken: $accessToken
                    refreshToken: $refreshToken
                """.trimIndent()
        )
    }

    override fun onRefreshTokenExpired(): Pair<String?, String?> {
        return runBlocking {
            Log.d(
                SDKApplication.LOG_TAG, """
                Example Layer Callback -> onRefreshTokenExpired
            """.trimIndent()
            )

            val task = GetTokensTask()
            val tokens = task.getTokens(applicationContext, false)
            Log.d(SDKApplication.LOG_TAG, "tokens: $tokens")
            tokens
        }
    }
}