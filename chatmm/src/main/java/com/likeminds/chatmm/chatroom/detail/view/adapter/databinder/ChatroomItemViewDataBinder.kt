package com.likeminds.chatmm.chatroom.detail.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.detail.model.ChatroomViewData
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.databinding.ItemChatroomBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.reactions.util.ReactionUtil
import com.likeminds.chatmm.reactions.util.ReactionsPreferences
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CHAT_ROOM

internal class ChatroomItemViewDataBinder constructor(
    private val userPreferences: UserPreferences,
    private val reactionsPreferences: ReactionsPreferences,
    private val chatroomDetailAdapterListener: ChatroomDetailAdapterListener
) : ViewDataBinder<ItemChatroomBinding, BaseViewType>() {

    override val viewType: Int
        get() = ITEM_CHAT_ROOM

    override fun createBinder(parent: ViewGroup): ItemChatroomBinding {
        val inflater = LayoutInflater.from(parent.context)
        return ItemChatroomBinding.inflate(inflater, parent, false)
    }

    override fun bindData(
        binding: ItemChatroomBinding,
        data: BaseViewType,
        position: Int,
    ) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
            chatroomViewData = data as ChatroomViewData
            ChatroomConversationItemViewDataBinderUtil.initChatRoomBubbleView(
                clBubble,
                memberImage,
                tvMemberName,
                tvCustomTitle,
                tvCustomTitleDot,
                data.memberViewData,
                chatroomDetailAdapterListener,
                position,
                chatRoom = data
            )
            ChatroomConversationItemViewDataBinderUtil.initConversationBubbleTextView(
                tvTitle,
                data.title,
                itemPosition = position,
                chatRoom = data,
                adapterListener = chatroomDetailAdapterListener
            )
            val time = DateUtil.createDateFormat("hh:mm", data.createdAt)
            ChatroomConversationItemViewDataBinderUtil.initTimeAndStatus(
                tvTime,
                userPreferences.getUUID(),
                time
            )

            ChatroomConversationItemViewDataBinderUtil.initSelectionAnimation(
                viewSelectionAnimation,
                position,
                chatroomDetailAdapterListener
            )

            val viewList = listOf(
                root,
                memberImage,
                tvTitle
            )
            isSelected = ChatroomConversationItemViewDataBinderUtil.initChatRoomSelection(
                root,
                viewList,
                data,
                position,
                chatroomDetailAdapterListener
            )

            val reactionsGridViewData = ReactionUtil.getChatroomReactionsGrid(data)

            ChatroomConversationItemViewDataBinderUtil.initChatroomReactionGridView(
                reactionsGridViewData,
                clBubble,
                clMain,
                messageReactionsGridLayout,
                userPreferences.getUUID(),
                chatroomDetailAdapterListener,
                data
            )

            val isReactionHintShown =
                ChatroomConversationItemViewDataBinderUtil.isReactionHintViewShown(
                    data.totalResponseCount == 0,
                    reactionsPreferences.getHasUserReactedOnce(),
                    reactionsPreferences.getNoOfTimesHintShown(),
                    reactionsPreferences.getTotalNoOfHintsAllowed(),
                    tvDoubleTap,
                    data.memberViewData,
                    userPreferences.getUUID(),
                    clBubble,
                    clMain
                )
            if (isReactionHintShown) {
                chatroomDetailAdapterListener.reactionHintShown()
            }
        }
    }
}