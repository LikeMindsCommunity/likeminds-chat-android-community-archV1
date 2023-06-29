package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.SeekBar
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.AttachmentViewData
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemConversationVoiceNoteBinding
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.chatmm.utils.SDKPreferences
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_VOICE_NOTE

internal class ConversationVoiceNoteItemViewDataBinder constructor(
    private val sdkPreferences: SDKPreferences,
    private val adapterListener: ChatroomDetailAdapterListener,
) : ViewDataBinder<ItemConversationVoiceNoteBinding, BaseViewType>() {

    //Variable for long click and single click on seekbar
    var xAtDown = 0f
    var yAtDown = 0f
    var handler: Handler? = null

    override val viewType: Int
        get() = ITEM_CONVERSATION_VOICE_NOTE

    override fun drawPrimaryColor(binding: ItemConversationVoiceNoteBinding, color: Int) {
        super.drawPrimaryColor(binding, color)
        binding.viewMediaUploadingActions.progressBarLoading.progressTintList =
            ColorStateList.valueOf(color)
        binding.viewReply.viewVerticalLine.backgroundTintList = ColorStateList.valueOf(color)
        binding.viewReply.tvConversationMemberName.setTextColor(color)
        binding.viewSelection.backgroundTintList = ColorStateList.valueOf(color)
        binding.viewSelectionAnimation.backgroundTintList = ColorStateList.valueOf(color)
    }

    override fun drawAdvancedColor(
        binding: ItemConversationVoiceNoteBinding,
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

    override fun createBinder(parent: ViewGroup): ItemConversationVoiceNoteBinding {
        return ItemConversationVoiceNoteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
    }

    override fun bindData(
        binding: ItemConversationVoiceNoteBinding,
        data: BaseViewType,
        position: Int,
    ) {
        binding.apply {
            conversation = data as ConversationViewData
            itemPosition = position
            attachment = data.attachments?.get(0) ?: return
            ChatroomConversationItemViewDataBinderUtil.initConversationBubbleView(
                clConversationRoot,
                clConversationBubble,
                memberImage,
                tvMemberName,
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
                voiceNoteView.clLayout.hide()
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleDeletedTextView(
                    tvConversation,
                    tvDeleteMessage,
                    sdkPreferences.getMemberId(),
                    conversationViewData = data
                )
            } else {
                voiceNoteView.clLayout.show()
                ChatroomConversationItemViewDataBinderUtil.initConversationBubbleTextView(
                    tvConversation,
                    data.answer,
                    position,
                    createdAt = data.createdAt,
                    conversation = data,
                    adapterListener = adapterListener,
                    tvDeleteMessage = binding.tvDeleteMessage
                )
                initVoiceNoteView(this, data)
            }

//            ChatroomConversationItemViewDataBinderUtil.initReactionButton(
//                ivAddReaction,
//                data,
//                sdkPreferences.getMemberId()
//            )

            ChatroomConversationItemViewDataBinderUtil.initProgress(tvProgress, data)

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
                viewSelectionAnimation, position, adapterListener
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
                voiceNoteView.root,
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

//            val messageReactionGridViewData = ChatroomUtil.getMessageReactionsGrid(data)

//            ChatroomConversationItemViewDataBinderUtil.initMessageReactionGridView(
//                messageReactionGridViewData,
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
//                    messageReactionsPreferences.getNoOfTimesHintShown(),
//                    binding.tvDoubleTap,
//                    data.memberViewData(),
//                    sdkPreferences.getMemberId(),
//                    binding.clConversationRoot,
//                    binding.clConversationBubble
//                )

//            if (isReactionHintShown) {
//                adapterListener.messageReactionHintShown()
//            }
        }
    }

    private fun initVoiceNoteView(
        binding: ItemConversationVoiceNoteBinding,
        conversation: ConversationViewData,
    ) {
        val mediaUploadData = ChatroomConversationItemViewDataBinderUtil.initUploadMediaAction(
            binding.viewMediaUploadingActions,
            conversation = conversation,
            listener = adapterListener
        )

        val mediaActionVisible = if (mediaUploadData.third == "sending") {
            true
        } else {
            mediaUploadData.second
        }

        val mediaUploadFailed = conversation.isFailed()

        if (mediaUploadData.first != null) {
            adapterListener.observeMediaUpload(mediaUploadData.first!!, conversation)
        }

        val attachment = binding.attachment ?: return

        binding.voiceNoteView.seekBar.max = attachment.meta?.duration ?: 100
        binding.voiceNoteView.ivPlayPause.isClickable = !(mediaActionVisible || mediaUploadFailed)

        setListeners(binding, attachment)

        ChatroomConversationItemViewDataBinderUtil.initVoiceNoteView(binding, attachment)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setListeners(
        binding: ItemConversationVoiceNoteBinding,
        attachment: AttachmentViewData,
    ) {
        binding.apply {
            root.setOnClickListener {
                checkForSelection(this, checkForSelectionEnabled = true)
            }

            root.setOnLongClickListener {
                checkForSelection(this)
                true
            }

            voiceNoteView.ivPlayPause.setOnClickListener {
                if (!checkForSelection(this, checkForSelectionEnabled = true)) {
                    val id = conversation?.id
                    if (id != null) {
                        adapterListener.onAudioConversationActionClicked(
                            attachment,
                            id,
                            childPosition = 0,
                            voiceNoteView.seekBar.progress
                        )
                    }
                }
            }

            voiceNoteView.ivPlayPause.setOnLongClickListener {
                checkForSelection(this)
                true
            }

            voiceNoteView.seekBar.setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        if (!checkForSelection(this, checkForSelectionEnabled = true)) {
                            isProgressBarFocused = true
                            xAtDown = event.x
                            yAtDown = event.y
                            handler = Handler(Looper.getMainLooper())
                            handler?.postDelayed({
                                handler?.removeCallbacksAndMessages(null)
                                handler = null
                                checkForSelection(this)
                            }, ViewConfiguration.getLongPressTimeout().toLong())
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        isProgressBarFocused = false
                        handler?.removeCallbacksAndMessages(null)
                        handler = null
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (event.x - xAtDown >= ViewUtils.dpToPx(5) || event.y - yAtDown >= ViewUtils.dpToPx(
                                5
                            )
                        ) {
                            handler?.removeCallbacksAndMessages(null)
                            handler = null
                        }
                    }
                }
                return@setOnTouchListener false
            }

            ivAddReaction.setOnClickListener {
                val conversation = conversation ?: return@setOnClickListener
                val position = itemPosition ?: return@setOnClickListener
                adapterListener.onLongPressConversation(
                    conversation,
                    position,
                    LMAnalytics.Source.SOURCE_MESSAGE_REACTIONS_FROM_REACTION_BUTTON
                )
            }

            voiceNoteView.seekBar.setOnSeekBarChangeListener(object :
                SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean
                ) {
                    val id = binding.conversation?.id
                    if (fromUser && id != null) {
                        voiceNoteView.tvDuration.text = DateUtil.formatSeconds(progress)
                        adapterListener.onConversationSeekbarChanged(
                            progress,
                            attachment,
                            id,
                            0
                        )
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            })
        }
    }

    private fun checkForSelection(
        binding: ItemConversationVoiceNoteBinding,
        checkForSelectionEnabled: Boolean = false,
    ): Boolean {
        val conversation = binding.conversation ?: return false
        val position = binding.itemPosition ?: return false

        if (checkForSelectionEnabled) {
            if (adapterListener.isSelectionEnabled()) {
                adapterListener.onLongPressConversation(
                    conversation,
                    position,
                    LMAnalytics.Source.SOURCE_MESSAGE_REACTIONS_FROM_LONG_PRESS
                )
                return true
            }
        } else {
            adapterListener.onLongPressConversation(
                conversation,
                position,
                LMAnalytics.Source.SOURCE_MESSAGE_REACTIONS_FROM_LONG_PRESS
            )
            return true
        }
        return false
    }
}