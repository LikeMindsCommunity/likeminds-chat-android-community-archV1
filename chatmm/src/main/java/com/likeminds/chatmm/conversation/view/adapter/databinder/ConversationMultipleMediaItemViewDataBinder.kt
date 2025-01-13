package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.view.*
import androidx.recyclerview.widget.GridLayoutManager
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.create.view.adapter.ChatroomItemAdapter
import com.likeminds.chatmm.chatroom.create.view.adapter.ChatroomItemAdapterListener
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemConversationMultipleMediaBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.reactions.util.ReactionUtil
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_MULTIPLE_MEDIA
import javax.inject.Inject

internal class ConversationMultipleMediaItemViewDataBinder @Inject constructor(
    private val userPreferences: UserPreferences,
    private val adapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationMultipleMediaBinding, BaseViewType>(),
    ChatroomItemAdapterListener {

    private var mediaActionVisible: Boolean = false

    override val viewType: Int
        get() = ITEM_CONVERSATION_MULTIPLE_MEDIA

    override fun createBinder(parent: ViewGroup): ItemConversationMultipleMediaBinding {
        val inflater = LayoutInflater.from(parent.context)
        return ItemConversationMultipleMediaBinding.inflate(inflater, parent, false)
    }

    override fun bindData(
        binding: ItemConversationMultipleMediaBinding,
        data: BaseViewType,
        position: Int,
    ) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
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
                adapterListener,
                position,
                conversationViewData = data,
                imageViewStatus = ivConversationStatus,
                imageViewFailed = ivConversationFailed
            )

            if (data.deletedBy != null) {
                rvDocuments.visibility = View.GONE
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
                    adapterListener = adapterListener,
                    tvDeleteMessage = tvDeleteMessage
                )
                initDocumentsListView(binding, data, position)
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
                adapterListener,
                itemPosition = position,
                conversation = data
            )

            ChatroomConversationItemViewDataBinderUtil.initSelectionAnimation(
                viewSelectionAnimation,
                position,
                adapterListener
            )

            ChatroomConversationItemViewDataBinderUtil.initReportView(
                ivReport,
                userPreferences.getUUID(),
                adapterListener,
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
                    adapterListener
                )

            val reactionsGridViewData = ReactionUtil.getReactionsGrid(data)

            ChatroomConversationItemViewDataBinderUtil.initMessageReactionGridView(
                reactionsGridViewData,
                clConversationRoot,
                clConversationBubble,
                messageReactionsGridLayout,
                userPreferences.getUUID(),
                adapterListener,
                data
            )
        }
    }

    private fun initDocumentsListView(
        binding: ItemConversationMultipleMediaBinding,
        conversationViewData: ConversationViewData,
        position: Int,
    ) {
        binding.apply {
            val gridLayoutManager = GridLayoutManager(root.context, 2)
            rvDocuments.layoutManager = gridLayoutManager
            val documentsItemAdapter = ChatroomItemAdapter(
                userPreferences,
                chatroomItemAdapterListener = this@ConversationMultipleMediaItemViewDataBinder
            )
            rvDocuments.adapter = documentsItemAdapter
            documentsItemAdapter.replace(
                ChatroomConversationItemViewDataBinderUtil.getAttachmentViewDataList(
                    conversationViewData.attachments,
                    parentViewItemPosition = position,
                    parentConversation = conversationViewData
                )
            )

            val mediaUploadData = ChatroomConversationItemViewDataBinderUtil.initUploadMediaAction(
                viewMediaUploadingActions,
                conversation = conversationViewData,
                listener = adapterListener
            )

            mediaActionVisible = mediaUploadData.second

            if (mediaUploadData.first != null) {
                adapterListener.observeMediaUpload(
                    mediaUploadData.first!!, conversationViewData
                )
            }

            ivAddReaction.setOnClickListener {
                adapterListener.onLongPressConversation(
                    conversationViewData,
                    position,
                    LMAnalytics.Source.MESSAGE_REACTIONS_FROM_REACTION_BUTTON
                )
            }
        }
    }

    override fun onLongPressConversation(
        conversation: ConversationViewData,
        itemPosition: Int,
    ) {
        adapterListener.onLongPressConversation(
            conversation,
            itemPosition,
            LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
        )
    }

    override fun isSelectionEnabled(): Boolean {
        return adapterListener.isSelectionEnabled()
    }

    override fun isMediaActionVisible(): Boolean {
        return mediaActionVisible
    }

    override fun onScreenChanged() {
        adapterListener.onScreenChanged()
    }
}