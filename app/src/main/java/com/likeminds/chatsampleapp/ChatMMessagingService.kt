package com.likeminds.chatsampleapp

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.likeminds.chatmm.SDKApplication.Companion.LOG_TAG
import com.likeminds.chatmm.pushnotification.util.LMChatNotificationHandler

class ChatMMessagingService : FirebaseMessagingService() {

    private lateinit var mNotificationHandler: LMChatNotificationHandler

    override fun onCreate() {
        super.onCreate()
        mNotificationHandler = LMChatNotificationHandler.getInstance()
        mNotificationHandler.create(this.application)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(LOG_TAG, "token generated: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(
            LOG_TAG,
            "message generated: ${message.data} timestamp: ${System.currentTimeMillis()}"
        )
        mNotificationHandler.handleNotification(message.data)
    }
}