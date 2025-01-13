package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.view.*
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemConversationLinkBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.reactions.util.ReactionUtil
import com.likeminds.chatmm.reactions.util.ReactionsPreferences
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_LINK

internal class ConversationLinkItemViewDataBinder constructor(
    private val userPreferences: UserPreferences,
    private val reactionsPreferences: ReactionsPreferences,
    private val chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationLinkBinding, ConversationViewData>() {

    override val viewType: Int
        get() = ITEM_CONVERSATION_LINK

    override fun createBinder(parent: ViewGroup): ItemConversationLinkBinding {
        return ItemConversationLinkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bindData(
        binding: ItemConversationLinkBinding,
        data: ConversationViewData,
        position: Int
    ) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
            viewReply.buttonColor = LMChatAppearance.getButtonsColor()
            conversation = data
            ChatroomConversationItemViewDataBinderUtil.initConversationBubbleView(
                clConversationRoot,
                clConversationBubble,
                memberImage,
                tvConversationMemberName,
                tvCustomTitle,
                tvCustomTitleDot,
                data.memberViewData,
                userPreferences.getUUID(),
                chatroomDetailAdapterListener,
                position,
                conversationViewData = data,
                imageViewStatus = ivConversationStatus,
                imageViewFailed = ivConversationFailed
            )

            if (data.deletedBy != null) {
                layoutLinkView.root.visibility = View.GONE
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleDeletedTextView(
                    tvConversation,
                    tvDeleteMessage,
                    userPreferences.getUUID(),
                    conversationViewData = data,
                    viewReply
                )
            } else {
                layoutLinkView.root.visibility = View.VISIBLE
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleTextView(
                    tvConversation,
                    data.answer,
                    position,
                    conversation = data,
                    adapterListener = chatroomDetailAdapterListener,
                    tvDeleteMessage = binding.tvDeleteMessage
                )

                ChatroomConversationItemViewDataBinderUtil.initLinkView(
                    layoutLinkView,
                    data.ogTags
                )
            }

            ChatroomConversationItemViewDataBinderUtil.initReactionButton(
                ivAddReaction,
                data,
                userPreferences.getUUID()
            )

            ChatroomConversationItemViewDataBinderUtil.initTimeAndStatus(
                tvTime,
                userPreferences.getUUID(),
                data.createdAt,
                imageViewStatus = ivConversationStatus,
                conversation = data
            )

            ChatroomConversationItemViewDataBinderUtil.initReplyView(
                viewReply,
                userPreferences.getUUID(),
                data.replyConversation,
                data.replyChatroomId,
                chatroomDetailAdapterListener,
                itemPosition = position,
                conversation = data
            )

            binding.layoutLinkView.root.setOnClickListener {
                if (chatroomDetailAdapterListener.isSelectionEnabled()) {
                    chatroomDetailAdapterListener.onLongPressConversation(
                        data,
                        position,
                        LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
                    )
                } else {
                    val url = data.ogTags?.url ?: return@setOnClickListener
                    val conversationId = data.id
                    val reportLinkExtras =
                        ChatroomConversationItemViewDataBinderUtil.createReportLinkExtras(data)
                    chatroomDetailAdapterListener.onScreenChanged()
                    chatroomDetailAdapterListener.externalLinkClicked(
                        conversationId,
                        url,
                        reportLinkExtras
                    )
                }
            }

            ivAddReaction.setOnClickListener {
                chatroomDetailAdapterListener.onLongPressConversation(
                    data,
                    position,
                    LMAnalytics.Source.MESSAGE_REACTIONS_FROM_REACTION_BUTTON
                )
            }

            ChatroomConversationItemViewDataBinderUtil.initSelectionAnimation(
                viewSelectionAnimation,
                position,
                chatroomDetailAdapterListener
            )

            ChatroomConversationItemViewDataBinderUtil.initReportView(
                ivReport,
                userPreferences.getUUID(),
                chatroomDetailAdapterListener,
                conversationViewData = data
            )

            val viewList = listOf(
                root,
                memberImage,
                tvConversation,
                viewReply.root,
                layoutLinkView.root,
                ivReport
            )
            isSelected = ChatroomConversationItemViewDataBinderUtil.initConversationSelection(
                root,
                viewList,
                data,
                position,
                chatroomDetailAdapterListener
            )

            val reactionsGridViewData = ReactionUtil.getReactionsGrid(data)

            ChatroomConversationItemViewDataBinderUtil.initMessageReactionGridView(
                reactionsGridViewData,
                binding.clConversationRoot,
                binding.clConversationBubble,
                binding.messageReactionsGridLayout,
                userPreferences.getUUID(),
                chatroomDetailAdapterListener,
                data
            )

            ChatroomConversationItemViewDataBinderUtil.isReactionHintViewShown(
                data.isLastItem,
                reactionsPreferences.getHasUserReactedOnce(),
                reactionsPreferences.getNoOfTimesHintShown(),
                reactionsPreferences.getTotalNoOfHintsAllowed(),
                tvDoubleTap,
                data.memberViewData,
                userPreferences.getUUID(),
                clConversationRoot,
                clConversationBubble
            )
        }
    }
}