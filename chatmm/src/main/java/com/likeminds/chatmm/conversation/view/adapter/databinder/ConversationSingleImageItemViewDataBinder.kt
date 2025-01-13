package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.net.Uri
import android.view.*
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemConversationSingleImageBinding
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.view.MediaActivity
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.reactions.util.ReactionUtil
import com.likeminds.chatmm.reactions.util.ReactionsPreferences
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.ProgressHelper
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.databinding.ImageBindingUtil
import com.likeminds.chatmm.utils.model.*

internal class ConversationSingleImageItemViewDataBinder(
    private val userPreferences: UserPreferences,
    private val reactionsPreferences: ReactionsPreferences,
    private val adapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationSingleImageBinding, BaseViewType>() {

    override val viewType: Int
        get() = ITEM_CONVERSATION_SINGLE_IMAGE

    override fun createBinder(parent: ViewGroup): ItemConversationSingleImageBinding {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemConversationSingleImageBinding.inflate(inflater, parent, false)
        initSingleImageViewClick(binding)
        return binding
    }

    override fun bindData(
        binding: ItemConversationSingleImageBinding,
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
                tvConversationMemberName,
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
                clImage.visibility = View.GONE
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleDeletedTextView(
                    tvConversation,
                    tvDeleteMessage,
                    userPreferences.getUUID(),
                    conversationViewData = data,
                    viewReply
                )
            } else {
                clImage.visibility = View.VISIBLE
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleTextView(
                    tvConversation,
                    data.answer,
                    position,
                    createdAt = data.createdAt,
                    conversation = data,
                    adapterListener = adapterListener,
                    tvDeleteMessage = tvDeleteMessage
                )
                initSingleImageView(this, data)
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
                ivSingleImage,
                viewReply.root,
                ivReport
            )
            isSelected = ChatroomConversationItemViewDataBinderUtil.initConversationSelection(
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
            val isReactionHintShown =
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
            if (isReactionHintShown) {
                adapterListener.reactionHintShown()
            }
        }
    }

    private fun initSingleImageViewClick(binding: ItemConversationSingleImageBinding) {
        binding.apply {
            ivSingleImage.setOnClickListener {
                val actionsVisible = viewMediaUploadingActions.actionsVisible
                if (actionsVisible == true) return@setOnClickListener

                val conversation = conversation ?: return@setOnClickListener
                val itemPosition = itemPosition
                if (itemPosition != null && adapterListener.isSelectionEnabled()) {
                    adapterListener.onLongPressConversation(
                        conversation,
                        itemPosition,
                        LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
                    )
                } else {
                    val imageUri = conversation.attachments?.get(0)?.uri
                    val subTitle = "${conversation.date ?: ""}, ${conversation.createdAt ?: ""}"
                    adapterListener.onScreenChanged()
                    val extra = MediaExtras.Builder()
                        .conversationId(conversation.id)
                        .chatroomId(conversation.chatroomId)
                        .communityId(conversation.communityId?.toIntOrNull())
                        .mediaScreenType(MEDIA_HORIZONTAL_LIST_SCREEN)
                        .medias(
                            listOf(
                                MediaSwipeViewData.Builder()
                                    .dynamicViewType(ITEM_IMAGE_SWIPE)
                                    .uri(imageUri ?: Uri.EMPTY)
                                    .title(conversation.memberViewData.name ?: "")
                                    .subTitle(subTitle).build()
                            )
                        )
                        .build()
                    MediaActivity.startActivity(
                        root.context,
                        extra
                    )
                }
            }

            ivAddReaction.setOnClickListener {
                val conversation = conversation ?: return@setOnClickListener
                val itemPosition = itemPosition ?: return@setOnClickListener
                adapterListener.onLongPressConversation(
                    conversation,
                    itemPosition,
                    LMAnalytics.Source.MESSAGE_REACTIONS_FROM_REACTION_BUTTON
                )
            }
        }
    }

    private fun initSingleImageView(
        binding: ItemConversationSingleImageBinding,
        conversationViewData: ConversationViewData,
    ) {
        binding.apply {
            if (!conversationViewData.attachments.isNullOrEmpty()) {
                val attachmentViewData = conversationViewData.attachments.firstOrNull()
                if (attachmentViewData != null) {
                    ProgressHelper.hideProgress(progressBar)
                    ivSingleImage.visibility = View.VISIBLE
                    ImageBindingUtil.loadImage(
                        ivSingleImage,
                        attachmentViewData.uri,
                        placeholder = R.drawable.lm_chat_image_placeholder,
                        cornerRadius = 10
                    )

                    ChatroomConversationItemViewDataBinderUtil.initImageAspectRatio(
                        clImage,
                        ivSingleImage,
                        attachmentViewData
                    )

                    val uploadData =
                        ChatroomConversationItemViewDataBinderUtil.initUploadMediaAction(
                            viewMediaUploadingActions,
                            conversation = conversationViewData,
                            listener = adapterListener
                        )

                    if (uploadData.first != null) {
                        adapterListener.observeMediaUpload(
                            uploadData.first!!, conversationViewData
                        )
                    }
                } else {
                    ivSingleImage.visibility = View.GONE
                    ProgressHelper.showProgress(progressBar, false)
                }
            } else {
                ivSingleImage.visibility = View.GONE
                ProgressHelper.hideProgress(progressBar)
            }
        }
    }

}