package com.likeminds.chatmm.pushnotification.util

import android.app.NotificationManager
import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.likeminds.chatmm.pushnotification.util.LMChatNotificationHandler.Companion.NOTIFICATION_UNREAD_CONVERSATION_GROUP_ID
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

object NotificationUtils {

    private const val NOTIFICATION_TAG = "chatroom_followed_feed"

    fun removeConversationNotification(
        context: Context,
        communityId: String?,
        communityName: String?,
        chatroomId: String
    ) {
        val notificationId = chatroomId.toIntOrNull() ?: return
        NotificationManagerCompat.from(context).apply {
            cancel(
                generateRouteForChatroom(communityId, communityName),
                notificationId
            )
        }
        try {
            removeConversationGroupNotification(context)
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            Log.e("Error", "${e.stackTrace}")
        }
    }

    fun removeConversationGroupNotification(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

            val activeNotifications = manager.activeNotifications.filter {
                (!it.tag.isNullOrEmpty() && it.tag.contains(NOTIFICATION_TAG))
            }

            if (activeNotifications.size == 1) {
                val notification = activeNotifications.find {
                    it.id == NOTIFICATION_UNREAD_CONVERSATION_GROUP_ID
                } ?: return

                NotificationManagerCompat.from(context).apply {
                    cancel(notification.tag, NOTIFICATION_UNREAD_CONVERSATION_GROUP_ID)
                }
            }
        }
    }

    // generates the route for chatroom with provided communityName and communityId
    private fun generateRouteForChatroom(communityId: String?, communityName: String?): String {
        return "route://chatroom_followed_feed?community_id=${communityId}&community_name=${communityName}"
    }
}