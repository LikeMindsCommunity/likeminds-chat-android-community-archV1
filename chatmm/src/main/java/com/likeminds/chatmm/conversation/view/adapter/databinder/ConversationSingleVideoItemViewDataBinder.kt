package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.net.Uri
import android.view.*
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemConversationSingleVideoBinding
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.view.MediaActivity
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.reactions.util.ReactionUtil
import com.likeminds.chatmm.reactions.util.ReactionsPreferences
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.chatmm.utils.ProgressHelper
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.databinding.ImageBindingUtil
import com.likeminds.chatmm.utils.model.*
import javax.inject.Inject

internal class ConversationSingleVideoItemViewDataBinder @Inject constructor(
    private val userPreferences: UserPreferences,
    private val reactionsPreferences: ReactionsPreferences,
    private val adapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationSingleVideoBinding, BaseViewType>() {

    override val viewType: Int
        get() = ITEM_CONVERSATION_SINGLE_VIDEO

    override fun createBinder(parent: ViewGroup): ItemConversationSingleVideoBinding {
        val inflater = LayoutInflater.from(parent.context)
        val itemConversationSingleVideoBinding =
            ItemConversationSingleVideoBinding.inflate(inflater, parent, false)
        initSingleVideoViewClick(itemConversationSingleVideoBinding)
        return itemConversationSingleVideoBinding
    }

    override fun bindData(
        binding: ItemConversationSingleVideoBinding,
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
                clImage.hide()
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleDeletedTextView(
                    tvConversation,
                    tvDeleteMessage,
                    userPreferences.getUUID(),
                    conversationViewData = data,
                    viewReply
                )
            } else {
                clImage.show()
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleTextView(
                    tvConversation,
                    data.answer,
                    position,
                    createdAt = data.createdAt,
                    conversation = data,
                    adapterListener = adapterListener,
                    tvDeleteMessage = tvDeleteMessage
                )
                initSingleVideoView(binding, data)
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

    private fun initSingleVideoViewClick(binding: ItemConversationSingleVideoBinding) {
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
                    val subTitle =
                        "${conversation.date ?: ""}, ${conversation.createdAt ?: ""}"
                    val attachment = conversation.attachments?.firstOrNull()
                    adapterListener.onScreenChanged()
                    val extras = MediaExtras.Builder()
                        .communityId(conversation.communityId?.toIntOrNull())
                        .conversationId(conversation.id)
                        .chatroomId(conversation.chatroomId)
                        .mediaScreenType(MEDIA_VIDEO_PLAY_SCREEN)
                        .medias(
                            listOf(
                                MediaSwipeViewData.Builder()
                                    .dynamicViewType(ITEM_VIDEO_SWIPE)
                                    .uri(attachment?.uri ?: Uri.EMPTY)
                                    .type("video")
                                    .thumbnail(attachment?.thumbnail)
                                    .title(
                                        conversation.memberViewData.name ?: ""
                                    )
                                    .subTitle(subTitle)
                                    .build()
                            )
                        )
                        .build()
                    MediaActivity.startActivity(
                        root.context,
                        extras
                    )
                }
            }

            ivAddReaction.setOnClickListener {
                val conversation =
                    conversation ?: return@setOnClickListener
                val itemPosition = itemPosition ?: return@setOnClickListener
                adapterListener.onLongPressConversation(
                    conversation,
                    itemPosition,
                    LMAnalytics.Source.MESSAGE_REACTIONS_FROM_REACTION_BUTTON
                )
            }
        }
    }

    private fun initSingleVideoView(
        binding: ItemConversationSingleVideoBinding,
        conversationViewData: ConversationViewData,
    ) {
        binding.apply {
            if (!conversationViewData.attachments.isNullOrEmpty()) {
                val attachmentViewData = conversationViewData.attachments[0]
                ProgressHelper.hideProgress(progressBar)
                ivSingleImage.visibility = View.VISIBLE
                val duration = attachmentViewData.meta?.duration
                if (duration != null) {
                    tvDuration.show()
                    tvDuration.text = DateUtil.formatSeconds(duration)
                } else {
                    tvDuration.hide()
                }

                if (!attachmentViewData.thumbnail.isNullOrEmpty()) {
                    ImageBindingUtil.loadImage(
                        ivSingleImage,
                        attachmentViewData.thumbnail,
                        placeholder = R.drawable.lm_chat_image_placeholder,
                        cornerRadius = 10
                    )
                } else {
                    ImageBindingUtil.loadImage(
                        ivSingleImage,
                        attachmentViewData.uri.toString(),
                        placeholder = R.drawable.lm_chat_image_placeholder,
                        cornerRadius = 10
                    )
                }

                val mediaUploadData =
                    ChatroomConversationItemViewDataBinderUtil.initUploadMediaAction(
                        viewMediaUploadingActions,
                        conversation = conversationViewData,
                        listener = adapterListener
                    )

                if (mediaUploadData.first != null) {
                    adapterListener.observeMediaUpload(
                        mediaUploadData.first!!, conversationViewData
                    )
                }
            } else {
                ivSingleImage.visibility = View.GONE
                ProgressHelper.hideProgress(binding.progressBar)
            }
        }
    }
}