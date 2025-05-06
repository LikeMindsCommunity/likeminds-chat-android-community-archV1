package com.likeminds.chatmm.utils

import android.content.Context
import android.content.Intent
import com.likeminds.chatmm.R
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder

object ShareUtils {
    const val DOMAIN = "https://www.chatsampleapp.com"

    /**
     * Share chatroom with url using default sharing in Android OS
     * @param context - context
     * @param chatroomId - id of the shared chatroom
     * @param domain - domain required to create share link
     */
    fun shareChatroom(
        context: Context,
        chatroomId: String,
        domain: String
    ) {
        val shareLink = "$domain/chatroom_detail?chatroom_id=$chatroomId"
        val shareTitle = context.getString(R.string.lm_chat_share_chatroom)
        shareLink(context, shareLink, shareTitle)
    }

    /**
     * Share conversation with url using default sharing in Android OS
     * @param context - context
     * @param conversation - Object of [ConversationViewData] shared
     * @param domain - domain required to create share link
     */
    fun shareConversation(
        context: Context,
        conversation: ConversationViewData,
        domain: String
    ) {
        val shareLink =
            "${domain}/chatroom_detail?chatroom_id=${conversation.chatroomId}&conversation_id=${conversation.id}"
        val shareTitle = if (conversation.answer.isEmpty()) {
            "Message"
        } else {
            MemberTaggingDecoder.decode(conversation.answer)
        }

        shareLink(context, shareLink, shareTitle)
    }

    //create intent and open sharing options without link as text
    private fun shareLink(
        context: Context,
        shareLink: String,
        shareTitle: String
    ) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, shareTitle)
            putExtra(Intent.EXTRA_TEXT, shareLink)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }
}