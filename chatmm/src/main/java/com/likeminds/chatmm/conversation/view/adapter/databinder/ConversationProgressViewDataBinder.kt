package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.chatmm.chatroom.detail.model.LMChatConversationProgressViewData
import com.likeminds.chatmm.databinding.LmChatItemConversationProgressBinding
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_PROGRESS

class ConversationProgressViewDataBinder :
    ViewDataBinder<LmChatItemConversationProgressBinding, LMChatConversationProgressViewData>() {
    override val viewType: Int
        get() = ITEM_CONVERSATION_PROGRESS

    override fun createBinder(parent: ViewGroup): LmChatItemConversationProgressBinding {
        return LmChatItemConversationProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bindData(
        binding: LmChatItemConversationProgressBinding,
        data: LMChatConversationProgressViewData,
        position: Int
    ) {
    }
}