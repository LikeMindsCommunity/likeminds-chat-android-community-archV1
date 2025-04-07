package com.likeminds.chatmm.widget.model

import android.os.Parcelable
import com.likeminds.chatmm.conversation.model.ConversationViewData
import kotlinx.parcelize.Parcelize

@Parcelize
class LMMetaViewData private constructor(
    val sourceChatroomId: String?,
    val sourceChatroomName: String?,
    val sourceConversation: ConversationViewData?,
    val type: String?
) : Parcelable {
    class Builder {
        private var sourceChatroomId: String? = null
        private var sourceChatroomName: String? = null
        private var sourceConversation: ConversationViewData? = null
        private var type: String? = null

        fun sourceChatroomId(sourceChatroomId: String?) = apply {
            this.sourceChatroomId = sourceChatroomId
        }

        fun sourceChatroomName(sourceChatroomName: String?) = apply {
            this.sourceChatroomName = sourceChatroomName
        }

        fun sourceConversation(sourceConversation: ConversationViewData?) = apply {
            this.sourceConversation = sourceConversation
        }

        fun type(type: String?) = apply {
            this.type = type
        }

        fun build() = LMMetaViewData(
            sourceChatroomId,
            sourceChatroomName,
            sourceConversation,
            type
        )
    }

    fun toBuilder(): Builder {
        return Builder()
            .sourceChatroomId(sourceChatroomId)
            .sourceChatroomName(sourceChatroomName)
            .sourceConversation(sourceConversation)
            .type(type)
    }

    override fun toString(): String {
        return "LMMetaViewData(" +
                "sourceChatroomId=$sourceChatroomId, " +
                "sourceChatroomName=$sourceChatroomName, " +
                "sourceConversation=$sourceConversation, " +
                "type=$type" +
                ")"
    }
}