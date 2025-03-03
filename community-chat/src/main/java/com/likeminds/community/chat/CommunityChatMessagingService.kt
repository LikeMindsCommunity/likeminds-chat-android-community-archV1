package com.likeminds.community.chat

import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.likeminds.chatmm.SDKApplication.Companion.LOG_TAG
import com.likeminds.chatmm.pushnotification.util.LMChatNotificationHandler

class CommunityChatMessagingService : FirebaseMessagingService() {

    private lateinit var mNotificationHandler: LMChatNotificationHandler

    override fun onCreate() {
        super.onCreate()

        val launcherIntent = Intent(this, CommunityChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        mNotificationHandler = LMChatNotificationHandler.getInstance()
        mNotificationHandler.create(this.application, launcherIntent)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(LOG_TAG, "token generated: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(
            LOG_TAG, """
            notification received: ${message.notification?.title} ${message.notification?.body} 
        """.trimIndent()
        )
        Log.d(
            LOG_TAG,
            "message generated: ${message.data} timestamp: ${System.currentTimeMillis()}"
        )
        mNotificationHandler.handleNotification(message.data)
    }
}