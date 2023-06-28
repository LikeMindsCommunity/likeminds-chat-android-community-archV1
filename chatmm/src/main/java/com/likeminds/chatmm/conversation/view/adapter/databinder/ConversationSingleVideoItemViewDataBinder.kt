package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.content.res.ColorStateList
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemConversationSingleVideoBinding
import com.likeminds.chatmm.media.model.MEDIA_VIDEO_PLAY_SCREEN
import com.likeminds.chatmm.media.model.MediaExtras
import com.likeminds.chatmm.media.model.MediaSwipeViewData
import com.likeminds.chatmm.media.view.MediaActivity
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.chatmm.utils.ProgressHelper
import com.likeminds.chatmm.utils.SDKPreferences
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.databinding.ImageBindingUtil
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_SINGLE_VIDEO
import com.likeminds.chatmm.utils.model.ITEM_VIDEO_SWIPE
import javax.inject.Inject

internal class ConversationSingleVideoItemViewDataBinder @Inject constructor(
    private val sdkPreferences: SDKPreferences,
//    private val messageReactionsPreferences: MessageReactionsPreferences,
    private val adapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationSingleVideoBinding, BaseViewType>() {

    override val viewType: Int
        get() = ITEM_CONVERSATION_SINGLE_VIDEO

    override fun drawPrimaryColor(binding: ItemConversationSingleVideoBinding, color: Int) {
        super.drawPrimaryColor(binding, color)
        binding.viewMediaUploadingActions.progressBarLoading.progressTintList =
            ColorStateList.valueOf(color)
        binding.viewReply.viewVerticalLine.backgroundTintList = ColorStateList.valueOf(color)
        binding.viewReply.tvConversationMemberName.setTextColor(color)
        binding.viewSelection.backgroundTintList = ColorStateList.valueOf(color)
        binding.viewSelectionAnimation.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun drawAdvancedColor(
        binding: ItemConversationSingleVideoBinding,
        headerColor: Int,
        buttonsIconsColor: Int,
        textLinksColor: Int,
    ) {
        super.drawAdvancedColor(binding, headerColor, buttonsIconsColor, textLinksColor)
        binding.viewMediaUploadingActions.progressBarLoading.progressTintList =
            ColorStateList.valueOf(buttonsIconsColor)
        binding.viewReply.viewVerticalLine.backgroundTintList =
            ColorStateList.valueOf(buttonsIconsColor)
        binding.viewReply.tvConversationMemberName.setTextColor(buttonsIconsColor)
        binding.viewSelection.backgroundTintList = ColorStateList.valueOf(buttonsIconsColor)
        binding.viewSelectionAnimation.backgroundTintList =
            ColorStateList.valueOf(buttonsIconsColor)
    }

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
                sdkPreferences.getMemberId(),
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
                    sdkPreferences.getMemberId(),
                    conversationViewData = data
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

//            ChatroomConversationItemViewDataBinderUtil.initReactionButton(
//                ivAddReaction,
//                data,
//                sdkPreferences.getMemberId()
//            )

            ChatroomConversationItemViewDataBinderUtil.initProgress(binding.tvProgress, data)

            ChatroomConversationItemViewDataBinderUtil.initTimeAndStatus(
                tvTime,
                sdkPreferences.getMemberId(),
                data.createdAt,
                data.answer.isEmpty() && data.deletedBy == null,
                imageViewStatus = ivConversationStatus,
                conversation = data
            )

            ChatroomConversationItemViewDataBinderUtil.initReplyView(
                viewReply,
                sdkPreferences.getMemberId(),
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
                sdkPreferences.getMemberId(),
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

//            val messageReactionsGridViewData = ChatroomUtil.getMessageReactionsGrid(data)
//
//            ChatroomConversationItemViewDataBinderUtil.initMessageReactionGridView(
//                messageReactionsGridViewData,
//                clConversationRoot,
//                clConversationBubble,
//                messageReactionsGridLayout,
//                sdkPreferences.getMemberId(),
//                adapterListener,
//                data
//            )
//            val isReactionHintShown =
//                ChatroomConversationItemViewDataBinderUtil.isReactionHintViewShown(
//                    data.isLastItem,
//                    messageReactionsPreferences.getHasUserReactedOnce(),
//                    messageReactionsPreferences.getNoOfTimesHintShown(),
//                    messageReactionsPreferences.getTotalNoOfHintsAllowed(),
//                    tvDoubleTap,
//                    data.memberViewData,
//                    sdkPreferences.getMemberId(),
//                    clConversationRoot,
//                    clConversationBubble
//                )
//            if (isReactionHintShown) {
//                adapterListener.messageReactionHintShown()
//            }
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
                        LMAnalytics.Source.SOURCE_MESSAGE_REACTIONS_FROM_LONG_PRESS
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
                    LMAnalytics.Source.SOURCE_MESSAGE_REACTIONS_FROM_REACTION_BUTTON
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
                        placeholder = R.drawable.image_placeholder,
                        cornerRadius = 10
                    )
                } else {
                    ImageBindingUtil.loadImage(
                        ivSingleImage,
                        attachmentViewData.uri.toString(),
                        placeholder = R.drawable.image_placeholder,
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