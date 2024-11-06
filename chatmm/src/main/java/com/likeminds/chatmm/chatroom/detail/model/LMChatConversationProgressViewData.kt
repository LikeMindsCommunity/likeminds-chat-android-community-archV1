package com.likeminds.chatmm.chatroom.detail.model

import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_PROGRESS

class LMChatConversationProgressViewData private constructor() : BaseViewType {
    override val viewType: Int
        get() = ITEM_CONVERSATION_PROGRESS

    class Builder {
        fun build() = LMChatConversationProgressViewData()
    }

    fun toBuilder(): Builder {
        return Builder()
    }
}