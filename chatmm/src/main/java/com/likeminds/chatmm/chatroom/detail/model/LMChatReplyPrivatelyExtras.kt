package com.likeminds.chatmm.chatroom.detail.model

import android.os.Parcelable
import com.likeminds.chatmm.conversation.model.ConversationViewData
import kotlinx.parcelize.Parcelize

@Parcelize
class LMChatReplyPrivatelyExtras private constructor(
    val sourceChatroomName: String,
    val sourceChatroomId: String,
    val sourceConversation: ConversationViewData
) : Parcelable {

    class Builder {
        private var sourceChatroomName: String = ""
        private var sourceChatroomId: String = ""
        private var sourceConversation: ConversationViewData =
            ConversationViewData.Builder().build()

        fun sourceChatroomName(sourceChatroomName: String) = apply {
            this.sourceChatroomName = sourceChatroomName
        }

        fun sourceChatroomId(sourceChatroomId: String) = apply {
            this.sourceChatroomId = sourceChatroomId
        }

        fun sourceConversation(sourceConversation: ConversationViewData) = apply {
            this.sourceConversation = sourceConversation
        }

        fun build() = LMChatReplyPrivatelyExtras(
            sourceChatroomName,
            sourceChatroomId,
            sourceConversation
        )
    }

    fun toBuilder(): Builder {
        return Builder().sourceChatroomName(sourceChatroomName)
            .sourceChatroomId(sourceChatroomId)
            .sourceConversation(sourceConversation)
    }
}