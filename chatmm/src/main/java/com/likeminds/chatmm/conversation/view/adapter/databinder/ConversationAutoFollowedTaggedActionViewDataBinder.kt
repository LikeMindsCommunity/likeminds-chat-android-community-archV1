package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.databinding.ItemConversationAutoFollowedTaggedActionBinding
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_AUTO_FOLLOWED_TAGGED_CHAT_ROOM

internal class ConversationAutoFollowedTaggedActionViewDataBinder constructor(
    private val chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationAutoFollowedTaggedActionBinding, BaseViewType>() {

    override val viewType: Int
        get() = ITEM_CONVERSATION_AUTO_FOLLOWED_TAGGED_CHAT_ROOM

    override fun createBinder(parent: ViewGroup): ItemConversationAutoFollowedTaggedActionBinding {
        val inflater = LayoutInflater.from(parent.context)
        val binding =
            ItemConversationAutoFollowedTaggedActionBinding.inflate(inflater, parent, false)
        setButtonClick(binding)
        return binding
    }

    override fun bindData(
        binding: ItemConversationAutoFollowedTaggedActionBinding,
        data: BaseViewType,
        position: Int,
    ) {

    }

    private fun setButtonClick(binding: ItemConversationAutoFollowedTaggedActionBinding) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()

            btnKeepFollowing.setOnClickListener {
                chatroomDetailAdapterListener.keepFollowingChatRoomClicked()
            }

            btnUnfollow.setOnClickListener {
                chatroomDetailAdapterListener.unFollowChatRoomClicked()
            }
        }
    }
}