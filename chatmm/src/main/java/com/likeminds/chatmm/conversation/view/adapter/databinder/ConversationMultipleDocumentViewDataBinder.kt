package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.annotation.SuppressLint
import android.view.*
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.chatroom.create.view.adapter.ChatroomItemAdapter
import com.likeminds.chatmm.chatroom.create.view.adapter.ChatroomItemAdapterListener
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemConversationMultipleDocumentBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.reactions.util.ReactionUtil
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.*

internal class ConversationMultipleDocumentViewDataBinder(
    private val userPreferences: UserPreferences,
    private val chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationMultipleDocumentBinding, BaseViewType>(),
    ChatroomItemAdapterListener {

    companion object {
        const val SHOW_MORE_COUNT = 2
    }

    private var mediaActionVisible: Boolean = false

    override val viewType: Int
        get() = ITEM_CONVERSATION_MULTIPLE_DOCUMENT

    override fun createBinder(parent: ViewGroup): ItemConversationMultipleDocumentBinding {
        val inflater = LayoutInflater.from(parent.context)
        return ItemConversationMultipleDocumentBinding.inflate(inflater, parent, false)
    }

    override fun bindData(
        binding: ItemConversationMultipleDocumentBinding,
        data: BaseViewType,
        position: Int,
    ) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
            textLinkColor = LMChatAppearance.getTextLinkColor()
            viewReply.buttonColor = LMChatAppearance.getButtonsColor()
            conversation = data as ConversationViewData
            itemPosition = position

            ChatroomConversationItemViewDataBinderUtil.initConversationBubbleView(
                clConversationRoot,
                clConversationBubble,
                memberImage,
                tvMemberName,
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
                rvDocuments.visibility = View.GONE
                tvShowMore.hide()
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleDeletedTextView(
                    tvConversation,
                    tvDeleteMessage,
                    userPreferences.getUUID(),
                    conversationViewData = data,
                    viewReply
                )
            } else {
                rvDocuments.visibility = View.VISIBLE
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleTextView(
                    tvConversation,
                    data.answer,
                    position,
                    createdAt = data.createdAt,
                    conversation = data,
                    adapterListener = chatroomDetailAdapterListener,
                    tvDeleteMessage = binding.tvDeleteMessage
                )
                initDocumentsListView(this, data, position)
            }

            ChatroomConversationItemViewDataBinderUtil.initReactionButton(
                ivAddReaction,
                data,
                userPreferences.getUUID()
            )

            ChatroomConversationItemViewDataBinderUtil.initProgress(binding.tvProgress, data)

            ChatroomConversationItemViewDataBinderUtil.initTimeAndStatus(
                tvTime,
                userPreferences.getUUID(),
                data.createdAt,
                data.answer.isEmpty() && data.deletedBy == null,
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
                ivReport
            )
            isSelected =
                ChatroomConversationItemViewDataBinderUtil.initConversationSelection(
                    root,
                    viewList,
                    data,
                    position,
                    chatroomDetailAdapterListener
                )

            val reactionsGridViewData = ReactionUtil.getReactionsGrid(data)

            ChatroomConversationItemViewDataBinderUtil.initMessageReactionGridView(
                reactionsGridViewData,
                clConversationRoot,
                clConversationBubble,
                messageReactionsGridLayout,
                userPreferences.getUUID(),
                chatroomDetailAdapterListener,
                data
            )
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initDocumentsListView(
        binding: ItemConversationMultipleDocumentBinding,
        conversation: ConversationViewData,
        position: Int,
    ) {
        binding.apply {
            val documentsItemAdapter = ChatroomItemAdapter(
                userPreferences,
                chatroomItemAdapterListener = this@ConversationMultipleDocumentViewDataBinder
            )
            rvDocuments.adapter = documentsItemAdapter

            val attachments = conversation.attachments.orEmpty().map {
                it.toBuilder()
                    .parentViewItemPosition(position)
                    .parentConversation(conversation)
                    .dynamicType(ITEM_DOCUMENT)
                    .build()
            }

            val tvConversationLP =
                tvConversation.layoutParams as ViewGroup.MarginLayoutParams
            if (conversation.isExpanded || attachments.size <= SHOW_MORE_COUNT) {
                tvShowMore.hide()
                tvConversationLP.topMargin = ViewUtils.dpToPx(4)
                documentsItemAdapter.replace(attachments)
            } else {
                tvShowMore.show()
                tvShowMore.text = "+${attachments.size - SHOW_MORE_COUNT} more"
                tvConversationLP.topMargin = ViewUtils.dpToPx(15)
                documentsItemAdapter.replace(attachments.take(SHOW_MORE_COUNT))
            }
            tvConversation.layoutParams = tvConversationLP

            val mediaUploadData = ChatroomConversationItemViewDataBinderUtil.initUploadMediaAction(
                viewMediaUploadingActions,
                conversation = conversation,
                listener = chatroomDetailAdapterListener
            )

            mediaActionVisible = mediaUploadData.second

            binding.tvShowMore.setOnClickListener {
                chatroomDetailAdapterListener.onMultipleItemsExpanded(conversation, position)
            }

            binding.ivAddReaction.setOnClickListener {
                chatroomDetailAdapterListener.onLongPressConversation(
                    conversation, position,
                    LMAnalytics.Source.MESSAGE_REACTIONS_FROM_REACTION_BUTTON
                )
            }
        }
    }

    override fun onLongPressConversation(
        conversation: ConversationViewData,
        itemPosition: Int,
    ) {
        chatroomDetailAdapterListener.onLongPressConversation(
            conversation,
            itemPosition,
            LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
        )
    }

    override fun isSelectionEnabled(): Boolean {
        return chatroomDetailAdapterListener.isSelectionEnabled()
    }

    override fun isMediaActionVisible(): Boolean {
        return mediaActionVisible
    }

    override fun onScreenChanged() {
        chatroomDetailAdapterListener.onScreenChanged()
    }
}