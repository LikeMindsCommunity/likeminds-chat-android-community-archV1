package com.likeminds.chatmm.chatroom.detail.util

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.text.*
import android.text.style.*
import android.text.util.Linkify
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.button.MaterialButton
import com.likeminds.chatmm.*
import com.likeminds.chatmm.chatroom.detail.model.ChatroomViewData
import com.likeminds.chatmm.chatroom.detail.model.TYPE_DIRECT_MESSAGE
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.*
import com.likeminds.chatmm.conversation.util.ChatReplyUtil
import com.likeminds.chatmm.databinding.*
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.util.MediaUtils
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.MemberImageUtil
import com.likeminds.chatmm.polls.model.*
import com.likeminds.chatmm.polls.view.PollViewListener
import com.likeminds.chatmm.reactions.model.ReactionsGridViewData
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.AndroidUtils
import com.likeminds.chatmm.utils.DateUtil
import com.likeminds.chatmm.utils.Route
import com.likeminds.chatmm.utils.ValueUtils.getValidTextForLinkify
import com.likeminds.chatmm.utils.ValueUtils.isValidYoutubeLink
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.chrometabs.CustomTabIntent
import com.likeminds.chatmm.utils.databinding.ImageBindingUtil
import com.likeminds.chatmm.utils.link.LMLinkMovementMethod
import com.likeminds.chatmm.utils.mediauploader.worker.UploadHelper
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.chatmm.utils.model.*
import java.util.UUID

object ChatroomConversationItemViewDataBinderUtil {

    fun initChatRoomBubbleView(
        clBubble: ConstraintLayout,
        memberImage: ImageView,
        tvConversationMemberName: TextView,
        tvCustomTitle: TextView,
        customTitleDot: View,
        memberViewData: MemberViewData,
        chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
        itemPosition: Int,
        chatRoom: ChatroomViewData,
    ) {
        setMessageSenderDetails(
            memberViewData,
            memberImage,
            tvConversationMemberName,
            tvCustomTitle,
            customTitleDot,
            itemPosition,
            chatroomDetailAdapterListener,
            chatRoom = chatRoom
        )
        clBubble.background = ContextCompat.getDrawable(
            clBubble.context,
            R.drawable.lm_chat_background_chat_other
        )
    }

    /**
     * @param viewList - The list of all the views whose click listener is implemented separately.
     * */
    fun initChatRoomSelection(
        rootView: View,
        viewList: List<View>,
        chatRoom: ChatroomViewData,
        itemPosition: Int,
        adapterListener: ChatroomDetailAdapterListener,
    ): Boolean {
        val isSelectionEnabled = adapterListener.isSelectionEnabled()
        val isSelected = if (isSelectionEnabled) {
            adapterListener.isChatRoomSelected(chatRoom.id)
        } else {
            false
        }

        viewList.forEach {
            it.setOnLongClickListener {
                adapterListener.onLongPressChatRoom(chatRoom, itemPosition)
                return@setOnLongClickListener true
            }
        }

        rootView.setOnClickListener {
            if (adapterListener.isSelectionEnabled()) {
                adapterListener.onLongPressChatRoom(chatRoom, itemPosition)
            }
        }
        return isSelected
    }

    /**
     * @param itemPosition - This is only needed if view is a conversation
     * @param conversationViewData - This is only needed if view is a conversation
     * */
    fun initConversationBubbleView(
        clRoot: ConstraintLayout,
        clConversationBubble: ConstraintLayout,
        memberImage: ImageView,
        tvConversationMemberName: TextView,
        tvCustomTitle: TextView,
        customTitleDot: View,
        memberViewData: MemberViewData,
        currentMemberId: String,
        chatroomAdapter: ChatroomDetailAdapterListener? = null,
        itemPosition: Int,
        conversationViewData: ConversationViewData? = null,
        imageViewStatus: ImageView? = null,
        imageViewFailed: ImageView? = null,
    ) {
        val context = clRoot.context
        val set = ConstraintSet()
        set.clone(clRoot)
        set.clear(clConversationBubble.id, ConstraintSet.RIGHT)
        set.clear(clConversationBubble.id, ConstraintSet.LEFT)
        val uuid = memberViewData.sdkClientInfo.uuid
        if (uuid == currentMemberId) {
            if (conversationViewData != null && conversationViewData.isFailed() && imageViewFailed != null) {
                set.connect(
                    clConversationBubble.id,
                    ConstraintSet.RIGHT,
                    imageViewFailed.id,
                    ConstraintSet.LEFT
                )
            } else {
                set.connect(
                    clConversationBubble.id,
                    ConstraintSet.RIGHT,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.RIGHT
                )
            }
            set.applyTo(clRoot)

            Log.d("PUI", "initConversationBubbleView1 $itemPosition")

            if (conversationViewData?.isFailed() == true) {
                imageViewStatus?.visibility = View.GONE
                imageViewFailed?.visibility = View.VISIBLE
                conversationViewData.workerUUID?.let { workerUUID ->
                    Log.d("PUI", "initConversationBubbleView: $workerUUID")
                    WorkManager.getInstance(context).cancelWorkById(UUID.fromString(workerUUID))
                }
            } else {
                imageViewFailed?.visibility = View.GONE
            }
            memberImage.visibility = View.GONE
            tvConversationMemberName.visibility = View.GONE
            tvCustomTitle.visibility = View.GONE
            customTitleDot.visibility = View.GONE
            clConversationBubble.background = ContextCompat.getDrawable(
                clRoot.context,
                R.drawable.lm_chat_chat_bubble_mine
            )
        } else {
            set.connect(
                clConversationBubble.id, ConstraintSet.LEFT, memberImage.id, ConstraintSet.RIGHT
            )
            set.applyTo(clRoot)
            memberImage.visibility = View.VISIBLE
            tvConversationMemberName.visibility = View.VISIBLE
            imageViewStatus?.visibility = View.GONE
            imageViewFailed?.visibility = View.GONE

            clConversationBubble.background =
                ContextCompat.getDrawable(clRoot.context, R.drawable.lm_chat_chat_bubble_other)
            setMessageSenderDetails(
                memberViewData,
                memberImage,
                tvConversationMemberName,
                tvCustomTitle,
                customTitleDot,
                itemPosition,
                chatroomAdapter = chatroomAdapter,
                conversationViewData = conversationViewData,
            )
        }
    }

    /**
     * Initialize the Member detail for each chat bubble
     */
    private fun setMessageSenderDetails(
        memberViewData: MemberViewData,
        memberImage: ImageView,
        tvConversationMemberName: TextView,
        tvCustomTitle: TextView,
        customTitleDot: View,
        itemPosition: Int,
        chatroomAdapter: ChatroomDetailAdapterListener? = null,
        chatRoom: ChatroomViewData? = null,
        conversationViewData: ConversationViewData? = null,
    ) {
        if (chatroomAdapter?.getChatRoomType() == TYPE_DIRECT_MESSAGE) {
            tvCustomTitle.visibility = View.GONE
            customTitleDot.visibility = View.GONE
            memberImage.visibility = View.GONE
            tvConversationMemberName.visibility = View.GONE
        } else {
            tvCustomTitle.visibility = View.VISIBLE
            customTitleDot.visibility = View.VISIBLE
            memberImage.visibility = View.VISIBLE
            tvConversationMemberName.visibility = View.VISIBLE

            val customTitle = memberViewData.customTitle
            if (customTitle.isNullOrEmpty()) {
                tvCustomTitle.visibility = View.GONE
                customTitleDot.visibility = View.GONE
            } else {
                tvCustomTitle.visibility = View.VISIBLE
                customTitleDot.visibility = View.VISIBLE
                tvCustomTitle.text = customTitle
            }
            tvConversationMemberName.text = memberViewData.name
            val colorCode = MemberImageUtil.setImage(
                memberViewData.imageUrl,
                memberViewData.name,
                memberViewData.sdkClientInfo.uuid,
                memberImage
            )
            tvConversationMemberName.setTextColor(colorCode)

            memberImage.setOnClickListener {
                if (chatroomAdapter != null) {
                    if (chatroomAdapter.isSelectionEnabled()) {
                        when {
                            conversationViewData != null ->
                                chatroomAdapter.onLongPressConversation(
                                    conversationViewData, itemPosition,
                                    LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
                                )

                            chatRoom != null -> {
                                chatroomAdapter.onLongPressChatRoom(chatRoom, itemPosition)
                            }

                            else -> {
                                chatroomAdapter.showMemberProfile(memberViewData)
                                chatroomAdapter.onScreenChanged()
                            }
                        }
                    } else {
                        chatroomAdapter.showMemberProfile(memberViewData)
                        chatroomAdapter.onScreenChanged()
                    }
                }
            }
        }
    }

    fun initConversationBubbleDeletedTextView(
        tvConversation: TextView,
        tvDeleteMessage: TextView,
        currentMemberId: String,
        conversationViewData: ConversationViewData,
        viewReply: LayoutReplyBinding? = null
    ) {
        tvConversation.visibility = View.GONE
        tvDeleteMessage.visibility = View.VISIBLE
        viewReply?.root?.hide()

        val sb = StringBuilder()
        sb.append(
            ChatroomUtil.getDeletedMessage(
                tvConversation.context,
                conversationViewData,
                currentMemberId
            )
        )
        sb.append(
            addExtraBlankSpace(
                createdAt = conversationViewData.createdAt,
                isDeleted = conversationViewData.isDeleted()
            )
        )

        tvDeleteMessage.text = sb.toString()
    }

    /**
     * @param conversation - This is only needed if view is a conversation
     * @param chatRoom - This is only needed if view is a chatRoom
     * @param tvDeleteMessage - This is only needed if view is a conversation
     * */
    //passed extra data to pass on chrome custom tab
    fun initConversationBubbleTextView(
        tvConversation: TextView,
        text: String,
        itemPosition: Int,
        createdAt: String? = null,
        conversation: ConversationViewData? = null,
        chatRoom: ChatroomViewData? = null,
        adapterListener: ChatroomDetailAdapterListener? = null,
        tvDeleteMessage: TextView? = null,
    ) {
        /**
         * Text is modified as Linkify doesn't accept texts with these specific unicode characters
         * @see #Linkify.containsUnsupportedCharacters(String)
         */
        val textForLinkify = text.getValidTextForLinkify()

        var alreadySeenFullConversation = conversation?.alreadySeenFullConversation == true

        var alreadySeenFullChatRoomTitle = chatRoom?.alreadySeenFullConversation == true

        tvDeleteMessage?.visibility = View.GONE
        if (textForLinkify.isEmpty()) {
            tvConversation.visibility = View.GONE
            return
        } else {
            tvConversation.visibility = View.VISIBLE
        }

        val trimmedText =
            if (!alreadySeenFullConversation && !conversation?.shortAnswer.isNullOrEmpty()) {
                conversation?.shortAnswer!!
            } else if (!alreadySeenFullChatRoomTitle && !chatRoom?.shortAnswer.isNullOrEmpty()) {
                chatRoom?.shortAnswer!!
            } else {
                textForLinkify
            }

        MemberTaggingDecoder.decode(
            tvConversation,
            trimmedText,
            true,
            LMChatAppearance.getTextLinkColor()
        ) {
            adapterListener?.onMemberTagClicked(it)
        }

        decodeBoldText(tvConversation)

        val readMoreColor =
            ContextCompat.getColor(tvConversation.context, R.color.lm_chat_caribbean_green)
        val readMore = SpannableStringBuilder(" Read More")
        readMore.setSpan(
            ForegroundColorSpan(readMoreColor),
            0,
            readMore.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val readMoreClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                alreadySeenFullConversation = true
                alreadySeenFullChatRoomTitle = true
                adapterListener?.updateSeenFullConversation(itemPosition, true)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
            }
        }
        val readMoreSpannableStringBuilder = SpannableStringBuilder()
        if ((!alreadySeenFullChatRoomTitle && !chatRoom?.shortAnswer.isNullOrEmpty())
            || !alreadySeenFullConversation && !conversation?.shortAnswer.isNullOrEmpty()
        ) {
            readMoreSpannableStringBuilder.append("...")
            readMoreSpannableStringBuilder.append(readMore)
            readMoreSpannableStringBuilder.setSpan(
                readMoreClickableSpan,
                3,
                readMore.length + 3,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val viewLessColor =
            ContextCompat.getColor(tvConversation.context, R.color.lm_chat_caribbean_green)
        val viewLess = SpannableStringBuilder(" View Less")
        viewLess.setSpan(
            ForegroundColorSpan(viewLessColor),
            0,
            viewLess.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val viewLessClickableSpan = object : ClickableSpan() {
            override fun onClick(view: View) {
                alreadySeenFullConversation = false
                alreadySeenFullChatRoomTitle = false
                adapterListener?.updateSeenFullConversation(itemPosition, false)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.isUnderlineText = false
            }
        }
        val viewLessSpannableStringBuilder = SpannableStringBuilder()
        if ((alreadySeenFullChatRoomTitle && !chatRoom?.shortAnswer.isNullOrEmpty())
            || alreadySeenFullConversation && !conversation?.shortAnswer.isNullOrEmpty()
        ) {
            viewLessSpannableStringBuilder.append(viewLess)
            viewLessSpannableStringBuilder.setSpan(
                viewLessClickableSpan,
                0,
                viewLess.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        tvConversation.text = TextUtils.concat(
            tvConversation.text,
            readMoreSpannableStringBuilder,
            viewLessSpannableStringBuilder
        )

        tvConversation.text = TextUtils.concat(
            tvConversation.text, addExtraBlankSpace(
                createdAt,
                conversation?.isEdited,
                conversation?.isDeleted()
            )
        )
        val linkifyLinks =
            (Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS)
        LinkifyCompat.addLinks(tvConversation, linkifyLinks)
        tvConversation.movementMethod = LMLinkMovementMethod { url ->
            if ((conversation != null || chatRoom != null) && adapterListener != null
                && adapterListener.isSelectionEnabled()
            ) {
                // Comment this code because Link movement click and textView click both calls at the same time.
                // adapterListener.onLongPressConversation(conversation, itemPosition)
            } else {
                adapterListener?.onLinkClicked(conversation?.id, url)
                val intent = Route.handleDeepLink(tvConversation.context, url)
                if (intent == null) {
                    val chatRoomId = chatRoom?.id ?: conversation?.chatroomId
                    val uuid =
                        chatRoom?.memberViewData?.sdkClientInfo?.uuid ?: ""
                    CustomTabIntent.open(
                        tvConversation.context, url,
                        ReportLinkExtras.Builder()
                            .chatroomId(chatRoomId!!)
                            .conversationId(conversation?.id)
                            .reportedMemberId(uuid)
                            .build()
                    )
                } else {
                    try {
                        ActivityCompat.startActivity(tvConversation.context, intent, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            true
        }

        tvConversation.setOnClickListener {
            if (adapterListener != null && adapterListener.isSelectionEnabled()) {
                if (conversation != null) {
                    adapterListener.onLongPressConversation(
                        conversation,
                        itemPosition,
                        LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
                    )
                } else if (chatRoom != null) {
                    adapterListener.onLongPressChatRoom(chatRoom, itemPosition)
                }
            }
        }
    }

    /**
     * Adds extra white spaces at the end to separate the conversation text and the date text as
     * both are on the same line and constraint cannot be added according to UI
     */
    private fun addExtraBlankSpace(
        createdAt: String? = null,
        isEdited: Boolean? = false,
        isDeleted: Boolean? = false,
    ): String {
        val sb = StringBuilder()
        if (isEdited == true) {
            sb.append("\n")
        } else if (!createdAt.isNullOrEmpty()) {
            var extraSpaceCount = if (createdAt.length > 5) 12 else 9
            if (isDeleted == true) {
                extraSpaceCount += 2
            }
            for (i in 0..createdAt.length + extraSpaceCount) {
                sb.append("\u00A0")
            }
        }
        return sb.toString()
    }

    fun initProgress(
        textView: TextView,
        conversation: ConversationViewData,
    ) {
        val progress = conversation.attachmentUploadProgress
        if (progress == null) {
            textView.hide()
        } else {
            textView.show()
            val current = MediaUtils.getFileSizeText(progress.first)
            val total = MediaUtils.getFileSizeText(progress.second)
            textView.apply {
                text = context.getString(
                    R.string.lm_chat_uploading_progress_placeholder,
                    current,
                    total
                )
            }
        }
    }

    //Return type is Triple and third will return state of worker
    fun initUploadMediaAction(
        binding: LayoutMediaUploadingActionsBinding,
        conversation: ConversationViewData? = null,
        chatroom: ChatroomViewData? = null,
        listener: ChatroomDetailAdapterListener? = null,
    ): Pair<UUID?, Boolean> {
        if (chatroom == null && conversation == null) {
            return Pair(null, false)
        }
        val attachmentCount = conversation?.attachmentCount ?: 0
        val workerUUID = conversation?.workerUUID
        val transferUtility by lazy { SDKApplication.getInstance().transferUtility }
        var uuid: UUID? = null
        var isInProgress = false
        var mediaActionsVisible = false

        binding.apply {
            ivCancel.setOnClickListener {
                uuid?.let {
                    UploadHelper.getAWSFileResponses(workerUUID).forEach { response ->
                        transferUtility.pause(response.transferObserver?.id ?: 0)
                    }
                    WorkManager.getInstance(root.context).cancelWorkById(it)
                }
            }

            tvRetry.setOnClickListener {
                if (conversation?.id != null) {
                    listener?.onRetryConversationMediaUpload(conversation.id, attachmentCount)
                }
            }

            when {
                //When attachments are not uploaded for this conversation
                (!workerUUID.isNullOrEmpty() && conversation.attachmentsUploaded != true) -> {
                    uuid = UUID.fromString(workerUUID)
                    val workInfo = WorkManager.getInstance(root.context)
                        .getWorkInfoById(uuid!!).get() ?: return Pair(
                        null,
                        mediaActionsVisible
                    )
                    when (workInfo.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED, WorkInfo.State.RUNNING -> {
                            isInProgress = true
                            groupMediaUploading.visibility = View.VISIBLE
                            tvRetry.visibility = View.GONE
                            mediaActionsVisible = true
                        }

                        WorkInfo.State.SUCCEEDED -> {
                            groupMediaUploading.visibility = View.GONE
                            tvRetry.visibility = View.GONE
                            mediaActionsVisible = false
                        }

                        else -> {
                            tvRetry.visibility = View.VISIBLE
                            groupMediaUploading.visibility = View.GONE
                            mediaActionsVisible = true
                        }
                    }
                }

                else -> {
                    groupMediaUploading.visibility = View.GONE
                    tvRetry.visibility = View.GONE
                    mediaActionsVisible = false
                }
            }
            actionsVisible = mediaActionsVisible
        }

        return if (isInProgress) {
            Pair(uuid, mediaActionsVisible)
        } else {
            Pair(null, mediaActionsVisible)
        }
    }

    fun initVoiceNoteView(
        binding: ItemConversationVoiceNoteBinding,
        attachment: AttachmentViewData,
    ) {
        when (attachment.mediaState) {
            MEDIA_ACTION_NONE -> {
                binding.voiceNoteView.apply {
                    ivPlayPause.apply {
                        setImageResource(R.drawable.lm_chat_ic_conversation_play_voice_note)
                        isClickable = true
                    }

                    if (attachment.meta != null) {
                        tvDuration.apply {
                            show()
                            text = if (attachment.currentDuration != "00:00") {
                                attachment.currentDuration
                            } else if (attachment.meta.duration == 0) {
                                DateUtil.formatSeconds(
                                    1
                                )
                            } else {
                                DateUtil.formatSeconds(
                                    attachment.meta.duration ?: 0
                                )
                            }
                        }
                    } else {
                        tvDuration.hide()
                    }

                    seekBar.isEnabled = true
                    seekBar.progress = attachment.progress ?: 0
                    bufferProgressBar.hide()
                }
            }

            MEDIA_ACTION_PLAY -> {
                binding.voiceNoteView.apply {
                    ivPlayPause.apply {
                        setImageResource(R.drawable.lm_chat_ic_conversation_pause_voice_note)
                        isClickable = true
                    }
                    tvDuration.text = attachment.currentDuration
                    bufferProgressBar.hide()
                    seekBar.apply {
                        isEnabled = true
                        isClickable = true
                        progress = attachment.progress ?: 0
                    }
                }
            }

            MEDIA_ACTION_PAUSE -> {
                binding.voiceNoteView.apply {
                    ivPlayPause.apply {
                        setImageResource(R.drawable.lm_chat_ic_conversation_play_voice_note)
                        isClickable = true
                    }
                    tvDuration.text = attachment.currentDuration
                    bufferProgressBar.hide()
                    seekBar.apply {
                        isEnabled = true
                        isClickable = true
                        progress = attachment.progress ?: 0
                    }
                }
            }
        }
    }

    /**
     * @param viewList - The list of all the views whose click listener is implemented separately.
     * */
    fun initConversationSelection(
        rootView: View,
        viewList: List<View>,
        conversation: ConversationViewData,
        itemPosition: Int,
        adapterListener: ChatroomDetailAdapterListener,
    ): Boolean {
        val isSelectionEnabled = adapterListener.isSelectionEnabled()
        val isSelected = if (isSelectionEnabled) {
            adapterListener.isConversationSelected(conversation.id)
        } else {
            false
        }

        viewList.forEach {
            it.setOnLongClickListener {
                if (conversation.isFailed()) {
                    adapterListener.onFailedConversationClick(conversation, itemPosition)
                } else {
                    adapterListener.onLongPressConversation(
                        conversation,
                        itemPosition,
                        LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
                    )
                }
                return@setOnLongClickListener true
            }
        }

        rootView.setOnClickListener {
            if (conversation.isFailed()) {
                adapterListener.onFailedConversationClick(conversation, itemPosition)
            } else if (adapterListener.isSelectionEnabled()) {
                adapterListener.onLongPressConversation(
                    conversation,
                    itemPosition,
                    LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
                )
            }
        }
        return isSelected
    }

    /**
     * Sets the time and the status of the conversation/chatroom object
     * @param tvTime The textview to set the time
     * @param time Time in String
     * @param isAnswerEmpty Is the conversation/chatroom text empty
     * @param imageViewStatus Pass the view only for conversation item
     * @param conversation The conversation object
     * @param hasDocumentMeta The conversation's attachment has meta object or not
     */
    fun initTimeAndStatus(
        tvTime: TextView,
        currentMemberId: String,
        time: String?,
        isAnswerEmpty: Boolean = false,
        imageViewStatus: ImageView? = null,
        conversation: ConversationViewData? = null,
        hasDocumentMeta: Boolean = false,
    ) {
        val context = tvTime.context
        tvTime.text = time
        val params = tvTime.layoutParams as ViewGroup.MarginLayoutParams
        if (isAnswerEmpty && !hasDocumentMeta) {
            tvTime.setTextColor(Color.WHITE)
            tvTime.setPadding(
                ViewUtils.dpToPx(5),
                ViewUtils.dpToPx(1),
                ViewUtils.dpToPx(5),
                ViewUtils.dpToPx(1)
            )
            params.setMargins(0, 0, ViewUtils.dpToPx(8), ViewUtils.dpToPx(6))
            tvTime.setBackgroundResource(R.drawable.lm_chat_background_conversation_timestamp)
        } else {
            tvTime.background = null
            tvTime.setPadding(0, 0, 0, 0)
            params.setMargins(0, 0, 0, 0)
            tvTime.setTextColor(ContextCompat.getColor(context, R.color.lm_chat_brown_grey))
        }
        tvTime.layoutParams = params

        if (
            conversation == null ||
            conversation.memberViewData.sdkClientInfo.uuid != currentMemberId ||
            conversation.isFailed()
        ) {
            tvTime.setCompoundDrawables(null, null, null, null)
            //Show the conversation status only in your own conversations and failed case is already handled
            return
        }
        when {
            isAnswerEmpty && !hasDocumentMeta -> {
                imageViewStatus?.visibility = View.GONE
                val statusDrawable = when {
                    conversation.isSending() -> {
                        ViewUtils.getDrawable(
                            context,
                            R.drawable.lm_chat_ic_sending,
                            12,
                            R.color.lm_chat_white
                        )
                    }

                    conversation.isSent() -> {
                        ViewUtils.getDrawable(
                            context,
                            R.drawable.lm_chat_ic_sent,
                            12,
                            R.color.lm_chat_white
                        )
                    }

                    else -> return
                } ?: return
                tvTime.compoundDrawablePadding = ViewUtils.dpToPx(4)
                tvTime.setCompoundDrawables(null, null, statusDrawable, null)
            }

            else -> {
                when {
                    conversation.isSending() -> {
                        imageViewStatus?.visibility = View.VISIBLE
                        imageViewStatus?.setImageResource(R.drawable.lm_chat_ic_sending)
                    }

                    conversation.isSent() -> {
                        imageViewStatus?.visibility = View.VISIBLE
                        imageViewStatus?.setImageResource(R.drawable.lm_chat_ic_sent)
                    }
                }
                tvTime.setCompoundDrawables(null, null, null, null)
            }
        }
    }

    fun initReplyView(
        binding: LayoutReplyBinding,
        currentMemberId: String,
        replyConversation: ConversationViewData?,
        replyChatRoomId: String?,
        listener: ChatroomDetailAdapterListener,
        itemPosition: Int,
        conversation: ConversationViewData,
    ) {
        binding.apply {
            var replyChatRoom: ChatroomViewData? = null
            if (replyChatRoomId != null) {
                val chatRoom = listener.getChatRoom()
                if (chatRoom?.id == replyChatRoomId) {
                    replyChatRoom = chatRoom
                }
            }

            if (replyChatRoom == null && replyConversation == null) {
                root.isVisible = false
            } else {
                root.isVisible = true
                val replyData = when {
                    replyConversation != null -> {
                        ChatReplyUtil.getConversationReplyData(
                            replyConversation,
                            currentMemberId
                        )
                    }

                    replyChatRoom != null -> {
                        ChatReplyUtil.getChatRoomReplyData(
                            replyChatRoom,
                            currentMemberId
                        )
                    }

                    else -> null
                }
                if (replyData != null) {
                    chatReplyData = replyData

                    val placeholder = if (replyData.attachmentType == AUDIO) {
                        R.drawable.lm_chat_placeholder_audio
                    } else {
                        R.drawable.lm_chat_image_placeholder
                    }

                    if (replyData.imageUrl.isNullOrEmpty()) {
                        ivAttachment.visibility = View.GONE
                    } else {
                        ivAttachment.visibility = View.VISIBLE
                        ImageBindingUtil.loadImage(
                            ivAttachment,
                            replyData.imageUrl,
                            placeholder
                        )
                    }

                    MemberTaggingDecoder.decode(
                        tvConversation,
                        replyData.conversationText,
                        false,
                        LMChatAppearance.getTextLinkColor()
                    )

                    if (replyData.drawable != null && binding.tvConversation.editableText != null) {
                        tvConversation.editableText.setSpan(
                            ImageSpan(binding.root.context, replyData.drawable),
                            0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }

                    root.setOnClickListener {
                        if (listener.isSelectionEnabled()) {
                            listener.onLongPressConversation(
                                conversation,
                                itemPosition,
                                LMAnalytics.Source.MESSAGE_REACTIONS_FROM_LONG_PRESS
                            )
                        } else {
                            if (replyConversation != null) {
                                listener.scrollToRepliedAnswer(conversation, replyConversation.id)
                            } else if (replyChatRoom != null) {
                                listener.scrollToRepliedChatroom(replyChatRoom.id)
                            }
                        }
                    }
                }
            }
        }
    }

    fun initSelectionAnimation(
        viewSelectionAnimation: View, position: Int,
        chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
    ) {
        if (chatroomDetailAdapterListener.isScrolledConversation(position)) {
            viewSelectionAnimation.alpha = 0.16f
            viewSelectionAnimation.animate().alpha(0.0f).duration = 3000
        }
    }

    fun initReportView(
        imageViewReport: ImageView,
        currentMemberId: String,
        chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
        conversationViewData: ConversationViewData,
    ) {
        if (chatroomDetailAdapterListener.isReportedConversation(conversationViewData.id)
            && conversationViewData.memberViewData.sdkClientInfo.uuid != currentMemberId
        ) {
            imageViewReport.visibility = View.VISIBLE
            imageViewReport.setOnClickListener {
                chatroomDetailAdapterListener.showActionDialogForReportedMessage()
            }
        } else {
            imageViewReport.visibility = View.GONE
        }
    }

    fun initAudioItemView(binding: ItemAudioBinding, attachment: AttachmentViewData) {
        binding.apply {
            when (attachment.mediaState) {
                MEDIA_ACTION_NONE -> {
                    ivPlayPause.setImageResource(R.drawable.lm_chat_ic_play_grey)
                    ivPlayPause.isClickable = true
                    if (attachment.meta != null) {
                        tvAudioDuration.text =
                            if (attachment.currentDuration != "00:00") {
                                attachment.currentDuration
                            } else {
                                DateUtil.formatSeconds(
                                    attachment.meta.duration ?: 0
                                )
                            }
                    }
                    seekBar.isEnabled = true
                    ivAudioLogo.show()
                    progressBarBuffer.hide()
                    waveAnim.hide()
                }

                MEDIA_ACTION_PLAY -> {
                    ivPlayPause.setImageResource(R.drawable.lm_chat_ic_pause_grey)
                    ivPlayPause.isClickable = true
                    tvAudioDuration.text = attachment.currentDuration
                    ivAudioLogo.hide()
                    waveAnim.show()
                    progressBarBuffer.hide()
                    seekBar.apply {
                        isEnabled = true
                        isClickable = true
                    }
                    if (!waveAnim.isAnimating) {
                        waveAnim.playAnimation()
                    }
                }

                MEDIA_ACTION_PAUSE -> {
                    ivPlayPause.setImageResource(R.drawable.lm_chat_ic_play_grey)
                    ivPlayPause.isClickable = true
                    tvAudioDuration.text = attachment.currentDuration
                    ivAudioLogo.hide()
                    waveAnim.show()
                    progressBarBuffer.hide()
                    seekBar.apply {
                        isEnabled = true
                        isClickable = true
                    }
                    waveAnim.pauseAnimation()
                }
            }
        }
    }

    fun initLinkView(binding: LayoutLinkViewBinding, data: LinkOGTagsViewData?) {
        if (data == null) return

        binding.apply {
            val isYoutubeLink = data.url?.isValidYoutubeLink() == true
            tvLinkTitle.text = if (data.title?.isNotBlank() == true) {
                data.title
            } else {
                root.context.getString(R.string.lm_chat_link)
            }
            tvLinkDescription.isVisible = !data.description.isNullOrEmpty()
            tvLinkDescription.text = data.description

            if (isYoutubeLink) {
                ivLink.visibility = View.GONE
                ivPlay.isVisible = !data.image.isNullOrEmpty()
                ivYoutubeLink.isVisible = !data.image.isNullOrEmpty()
                ivYoutubeLogo.isVisible = !data.image.isNullOrEmpty()
            } else {
                ivPlay.visibility = View.GONE
                ivYoutubeLink.visibility = View.GONE
                ivYoutubeLogo.visibility = View.GONE
                ivLink.isVisible = !data.image.isNullOrEmpty()
            }
            ImageBindingUtil.loadImage(
                if (isYoutubeLink) {
                    ivYoutubeLink
                } else {
                    ivLink
                },
                data.image,
                placeholder = R.drawable.lm_chat_ic_link_primary_40dp,
                cornerRadius = 8,
                isBlur = isYoutubeLink
            )

            tvLinkUrl.hide()
            root.visibility = View.VISIBLE
        }
    }

    fun initDocument(
        binding: ItemConversationSinglePdfBinding,
        conversation: ConversationViewData,
    ) {
        binding.apply {
            val attachment = conversation.attachments?.firstOrNull() ?: return
            tvMeta1.hide()
            viewMetaDot1.hide()
            tvMeta2.hide()
            viewMetaDot2.hide()
            tvMeta3.hide()
            if (attachment.meta != null && conversation.isNotDeleted()) {
                val noOfPage = attachment.meta.numberOfPage ?: 0
                val size = attachment.meta.size ?: 0
                val mediaType = attachment.type
                if (noOfPage > 0) {
                    tvMeta1.show()
                    tvMeta1.text = root.context.getString(
                        R.string.lm_chat_placeholder_pages, noOfPage
                    )
                }
                if (size > 0) {
                    tvMeta2.show()
                    tvMeta2.text = MediaUtils.getFileSizeText(size)
                    if (tvMeta1.isVisible) {
                        viewMetaDot1.show()
                    }
                }
                if (mediaType.isNotEmpty() && (tvMeta1.isVisible || tvMeta2.isVisible)) {
                    tvMeta3.show()
                    tvMeta3.text = mediaType
                    viewMetaDot2.show()
                }
            }
        }
    }

    fun createReportLinkExtras(data: BaseViewType?): ReportLinkExtras? {
        var reportLinkExtras: ReportLinkExtras? = null
        when (data) {
            is ConversationViewData -> {
                reportLinkExtras = ReportLinkExtras.Builder()
                    .chatroomId(data.chatroomId!!)
                    .conversationId(data.id)
                    .reportedMemberId(data.memberViewData.sdkClientInfo.uuid)
                    .build()
            }

            is ChatroomViewData -> {
                reportLinkExtras = ReportLinkExtras.Builder()
                    .chatroomId(data.id)
                    .conversationId(null)
                    .reportedMemberId(data.memberViewData.sdkClientInfo.uuid)
                    .build()
            }
        }
        return reportLinkExtras
    }

    /**
     * @param parentViewItemPosition - This is only needed if view called from chatRoom detail screen
     * @param parentChatRoom - This is only needed if view called from chatRoom detail screen
     * @param parentConversation - This is only needed if view called from chatRoom detail screen
     * */
    fun getAttachmentViewDataList(
        attachments: List<AttachmentViewData>?,
        parentViewItemPosition: Int? = null,
        parentChatRoom: ChatroomViewData? = null,
        parentConversation: ConversationViewData? = null,
    ): List<AttachmentViewData> {
        var attachmentList = ArrayList<AttachmentViewData>()
        if (!attachments.isNullOrEmpty()) {
            for (item in attachments) {
                attachmentList.add(item.toBuilder().attachments(attachments).build())
            }
        }

        // Filter only image, video and pdf type attachments
        attachmentList = attachmentList.filter {
            it.type == IMAGE || it.type == GIF || it.type == PDF || it.type == VIDEO
        } as ArrayList<AttachmentViewData>

        var attachmentViewDataList = attachmentList.sortedBy { it.index }
        val attachmentCount = attachmentViewDataList.size
        val attachmentLeft = attachmentCount - 3
        when {
            attachmentCount == 3 -> {
                attachmentViewDataList =
                    attachmentViewDataList.subList(0, 2)
                        .mapIndexed { index, attachmentViewData ->
                            val viewType = when (attachmentViewData.type) {
                                VIDEO -> ITEM_VIDEO
                                PDF -> ITEM_PDF
                                else -> ITEM_IMAGE
                            }
                            if (index == 1) {
                                return@mapIndexed attachmentViewData.toBuilder()
                                    .mediaLeft(2)
                                    .dynamicType(viewType)
                                    .attachments(attachmentList)
                                    .parentConversation(parentConversation)
                                    .parentChatroom(parentChatRoom)
                                    .parentViewItemPosition(parentViewItemPosition)
                                    .build()
                            } else {
                                return@mapIndexed attachmentViewData.toBuilder()
                                    .dynamicType(viewType)
                                    .parentConversation(parentConversation)
                                    .parentChatroom(parentChatRoom)
                                    .parentViewItemPosition(parentViewItemPosition)
                                    .build()
                            }
                        }
            }

            attachmentLeft > 1 -> {
                attachmentViewDataList =
                    attachmentViewDataList.subList(0, 4)
                        .mapIndexed { index, attachmentViewData ->
                            val viewType = when (attachmentViewData.type) {
                                VIDEO -> ITEM_VIDEO
                                PDF -> ITEM_PDF
                                else -> ITEM_IMAGE
                            }
                            if (index == 3) {
                                return@mapIndexed attachmentViewData.toBuilder()
                                    .mediaLeft(attachmentLeft)
                                    .dynamicType(viewType)
                                    .attachments(attachmentList)
                                    .parentConversation(parentConversation)
                                    .parentChatroom(parentChatRoom)
                                    .parentViewItemPosition(parentViewItemPosition)
                                    .build()
                            } else {
                                return@mapIndexed attachmentViewData.toBuilder()
                                    .dynamicType(viewType)
                                    .parentConversation(parentConversation)
                                    .parentChatroom(parentChatRoom)
                                    .parentViewItemPosition(parentViewItemPosition)
                                    .build()
                            }
                        }
            }

            else -> {
                attachmentViewDataList =
                    attachmentViewDataList.map { attachmentViewData ->
                        val viewType = when (attachmentViewData.type) {
                            VIDEO -> ITEM_VIDEO
                            PDF -> ITEM_PDF
                            else -> ITEM_IMAGE
                        }
                        return@map attachmentViewData.toBuilder()
                            .dynamicType(viewType)
                            .parentConversation(parentConversation)
                            .parentChatroom(parentChatRoom)
                            .parentViewItemPosition(parentViewItemPosition)
                            .build()

                    }
            }
        }
        return attachmentViewDataList
    }

    fun initImageAspectRatio(
        clImage: ConstraintLayout,
        image: ImageView,
        attachmentViewData: AttachmentViewData?,
    ) {
        val set = ConstraintSet()
        set.clone(clImage)
        if (attachmentViewData != null && (attachmentViewData.width
                ?: 0) > 0 && (attachmentViewData.height ?: 0) > 0
        ) {
            if (attachmentViewData.width!! > attachmentViewData.height!!) {
                val widthRatio =
                    attachmentViewData.width.toDouble() / AndroidUtils.dp(
                        260F, clImage.context
                    )
                val heightRatio =
                    attachmentViewData.height.toDouble() / AndroidUtils.dp(
                        260F, clImage.context
                    )
                set.setDimensionRatio(image.id, "$widthRatio:$heightRatio")
            } else {
                set.setDimensionRatio(image.id, "1:1")
            }
        } else {
            set.setDimensionRatio(image.id, "1:0.55")
        }
        set.applyTo(clImage)
    }

    /**
     * Poll Binder Utility functions
     * */

    fun PollInfoData?.isMultipleItemPoll(): Boolean {
        return this?.multipleSelectState != null
    }

    fun PollInfoData?.isAddNewOptionEnabled(): Boolean {
        return this?.allowAddOption == true
    }

    fun PollInfoData?.hasPollEnded(): Boolean {
        return hasPollOrEventEnded(this?.expiryTime)
    }

    fun PollInfoData?.isDeferredPoll(): Boolean {
        return this?.pollType == POLL_TYPE_DEFERRED
    }

    fun PollInfoData?.isInstantPoll(): Boolean {
        return this?.pollType == POLL_TYPE_INSTANT
    }

    private fun hasPollOrEventEnded(dateTime: Long?): Boolean {
        if (dateTime != null) {
            val dateTimeLeft = dateTime - System.currentTimeMillis()
            if (dateTimeLeft <= 0) {
                return true
            }
        }
        return false
    }

    fun initPollSelectText(tvPollSelectText: TextView, pollInfoData: PollInfoData) {
        tvPollSelectText.apply {
            val multiSelect = pollInfoData.multipleSelectState != null
            if (multiSelect) {
                val multiSelectNum = pollInfoData.multipleSelectNum
                text =
                    when (pollInfoData.multipleSelectState) {
                        POLL_MULTIPLE_STATE_EXACTLY -> {
                            context.getString(
                                R.string.lm_chat_select_exact_options_text,
                                multiSelectNum.toString()
                            )
                        }

                        POLL_MULTIPLE_STATE_MAX -> {
                            context.getString(
                                R.string.lm_chat_select_at_most_text,
                                multiSelectNum.toString()
                            )
                        }

                        POLL_MULTIPLE_STATE_LEAST -> {
                            context.getString(
                                R.string.lm_chat_select_at_least_text,
                                multiSelectNum.toString()
                            )
                        }

                        else -> ""
                    }
                visibility = View.VISIBLE
            } else {
                visibility = View.GONE
            }
        }
    }

    fun initPollEndDateTimeView(
        tvDateTimeLeft: TextView,
        expiryTime: Long?,
        listener: ChatroomDetailAdapterListener,
    ) {
        val dayTimeLeft = listener.getPollRemainingTime(expiryTime)
        updateEndDateTimeView(tvDateTimeLeft, dayTimeLeft)
    }

    private fun updateEndDateTimeView(tvDateTimeLeft: TextView, dayTimeLeft: String?) {
        tvDateTimeLeft.apply {
            if (dayTimeLeft != null) {
                visibility = View.VISIBLE
                text = dayTimeLeft

                if (dayTimeLeft == context.getString(R.string.lm_chat_poll_ended)
                    || dayTimeLeft == context.getString(R.string.lm_chat_event_ended_regular)
                ) {
                    background = ContextCompat.getDrawable(
                        context,
                        R.drawable.lm_chat_background_scarlet_12
                    )
                } else {
                    backgroundTintList =
                        ColorStateList.valueOf(LMChatAppearance.getButtonsColor())
                }
            }
        }
    }

    fun updatePollButton(buttonSubmitVote: MaterialButton, pollInfoData: PollInfoData) {
        val pollViewDataList = pollInfoData.pollViewDataList ?: emptyList()
        if (pollViewDataList.isNotEmpty()) {
            val count = pollViewDataList.filter { it.isSelected == true }.size
            val enableButton = when (pollInfoData.multipleSelectState) {
                POLL_MULTIPLE_STATE_EXACTLY -> {
                    count == (pollInfoData.multipleSelectNum ?: 0)
                }

                POLL_MULTIPLE_STATE_MAX -> {
                    count > 0
                }

                POLL_MULTIPLE_STATE_LEAST -> {
                    count >= (pollInfoData.multipleSelectNum ?: 0)
                }

                else -> false
            }
            if (enableButton) {
                enablePollButton(buttonSubmitVote)
            } else {
                disablePollButton(buttonSubmitVote)
            }
        }
    }

    fun enablePollButton(btnSubmitVote: MaterialButton?) {
        if (btnSubmitVote == null) {
            return
        }
        btnSubmitVote.apply {
            iconTint = ColorStateList.valueOf(LMChatAppearance.getButtonsColor())
            setTextColor(LMChatAppearance.getButtonsColor())
            strokeColor = ColorStateList.valueOf(LMChatAppearance.getButtonsColor())
            tag = "POLL_CLICK_ENABLED"
            isEnabled = true
        }
    }

    fun disablePollButton(btnSubmitVote: MaterialButton?) {
        if (btnSubmitVote == null) {
            return
        }
        btnSubmitVote.apply {
            val context = context
            setIconTintResource(R.color.lm_chat_grey)
            setTextColor(ContextCompat.getColor(context, R.color.lm_chat_grey))
            setStrokeColorResource(R.color.lm_chat_black_20)
            tag = "POLL_CLICK_DISABLED"
            isEnabled = false
        }
    }

    fun showAnonymousPollDialog(context: Context) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(context)
        builder.setTitle(context.getString(R.string.lm_chat_anonymous_poll))
            .setMessage(context.getString(R.string.lm_chat_anonymous_poll_message))
            .setPositiveButton(context.getString(R.string.lm_chat_okay)) { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    fun multipleItemSelectPollExactly(
        pollViewData: PollViewData,
        optionCount: Int,
        pollViewDataList: List<PollViewData>,
        listener: ChatroomDetailAdapterListener,
        buttonSubmitVote: MaterialButton?,
        pollViewListener: PollViewListener?,
        conversationId: String?,
        onComplete: (list: List<PollViewData>) -> Unit,
    ) {
        var selectedItemsCount = pollViewDataList.filter { it.isSelected == true }.size

        if (selectedItemsCount == optionCount) {
            if (pollViewData.isSelected == false) {
                listener.showToastMessage("You must select $optionCount options. Unselect an option or submit your vote now")
                return
            }
        }

        val data = updatePollItemsForInstantPollWithMultipleSelection(
            pollViewDataList, pollViewData, selectedItemsCount
        )
        onComplete(data.first)
        selectedItemsCount = data.second

        if (selectedItemsCount == optionCount) {
            listener.showToastMessage("$optionCount options selected. Submit your vote now")
        } else if (selectedItemsCount < optionCount) {
            val optionDifference = optionCount - selectedItemsCount
            if (optionDifference > 1) {
                listener.showToastMessage("Select $optionDifference more options to submit your vote")
            } else {
                listener.showToastMessage("Select $optionDifference more option to submit your vote")
            }
        }

        if (selectedItemsCount == 0) {
            listener.dismissToastMessage()
        }

        //enable poll button when option count items are selected, otherwise disable the poll button
        if (selectedItemsCount == optionCount) {
            enablePollButton(buttonSubmitVote)
            pollViewListener?.updatePollButton(conversationId, true)
        } else {
            disablePollButton(buttonSubmitVote)
            pollViewListener?.updatePollButton(conversationId, false)
        }
    }

    fun multipleItemSelectPollAtMax(
        pollViewData: PollViewData,
        optionCount: Int,
        pollViewDataList: List<PollViewData>,
        listener: ChatroomDetailAdapterListener,
        buttonSubmitVote: MaterialButton?,
        pollViewListener: PollViewListener?,
        conversationId: String?,
        onComplete: (list: List<PollViewData>) -> Unit,
    ) {
        var selectedItemsCount = pollViewDataList.filter { it.isSelected == true }.size

        //on selecting a new options when already option count items are selected, show a toast that he can't select more than option count
        if (selectedItemsCount == optionCount) {
            if (pollViewData.isSelected == false) {
                listener.showToastMessage("You can select max. $optionCount options. Unselect an option or submit your vote now")
                return
            }
        }

        val data = updatePollItemsForInstantPollWithMultipleSelection(
            pollViewDataList, pollViewData, selectedItemsCount
        )
        onComplete(data.first)
        selectedItemsCount = data.second

        if (selectedItemsCount == optionCount) {
            listener.showToastMessage("$selectedItemsCount options selected. Submit your vote now")
        } else if (selectedItemsCount < optionCount) {
            listener.showToastMessage("Select more options or submit your vote now")
        }

        if (selectedItemsCount == 0) {
            listener.dismissToastMessage()
        }

        //disable poll button when no options are selected
        if (selectedItemsCount == 0) {
            disablePollButton(buttonSubmitVote)
            pollViewListener?.updatePollButton(conversationId, false)
        } else {
            enablePollButton(buttonSubmitVote)
            pollViewListener?.updatePollButton(conversationId, true)
        }
    }

    fun multipleItemSelectPollAtLeast(
        pollViewData: PollViewData,
        optionCount: Int,
        pollViewDataList: List<PollViewData>,
        listener: ChatroomDetailAdapterListener,
        buttonSubmitVote: MaterialButton?,
        pollViewListener: PollViewListener?,
        conversationId: String?,
        onComplete: (list: List<PollViewData>) -> Unit,
    ) {
        var selectedItemsCount = pollViewDataList.filter { it.isSelected == true }.size

        val data = updatePollItemsForInstantPollWithMultipleSelection(
            pollViewDataList, pollViewData, selectedItemsCount
        )
        onComplete(data.first)
        selectedItemsCount = data.second

        if (selectedItemsCount < optionCount) {
            val optionDifference = optionCount - selectedItemsCount
            if (optionDifference > 1) {
                listener.showToastMessage("Select at least $optionDifference more options to submit your vote")
            } else {
                listener.showToastMessage("Select at least $optionDifference more option to submit your vote")
            }
        } else if (selectedItemsCount >= optionCount && selectedItemsCount < pollViewDataList.size) {
            listener.showToastMessage("$selectedItemsCount options selected. Select more options or submit your vote now")
        } else if (selectedItemsCount == pollViewDataList.size) {
            listener.showToastMessage("$selectedItemsCount options selected. Submit your vote now")
        }

        if (selectedItemsCount == 0) {
            listener.dismissToastMessage()
        }

        if (selectedItemsCount >= optionCount) {
            enablePollButton(buttonSubmitVote)
            pollViewListener?.updatePollButton(conversationId, true)
        } else {
            disablePollButton(buttonSubmitVote)
            pollViewListener?.updatePollButton(conversationId, false)
        }
    }

    fun singlePollItemSelected(
        pollViewData: PollViewData,
        pollViewDataList: List<PollViewData>,
        onComplete: (list: List<PollViewData>) -> Unit,
    ) {
        var oldValueSelected = false
        var totalVoteCount = 0.0
        pollViewDataList.forEach {
            totalVoteCount += it.noVotes ?: 0
        }

        var updatedPollViewList = pollViewDataList.map {
            val isPreviouslySelected = it.isSelected ?: false
            val previousNoOfVotes = it.noVotes ?: 0

            val updatedNoOfVotes: Int
            if (it.id == pollViewData.id) {
                if (isPreviouslySelected) {
                    oldValueSelected = true
                    updatedNoOfVotes = previousNoOfVotes
                } else {
                    updatedNoOfVotes = previousNoOfVotes + 1
                    totalVoteCount += 1
                }
                it.toBuilder().isSelected(true).noVotes(updatedNoOfVotes).build()
            } else {
                if (isPreviouslySelected) {
                    updatedNoOfVotes = previousNoOfVotes - 1
                    totalVoteCount -= 1
                } else {
                    updatedNoOfVotes = previousNoOfVotes
                }
                it.toBuilder().isSelected(false).noVotes(updatedNoOfVotes)
                    .build()
            }
        }

        updatedPollViewList = updatedPollViewList.map {
            val updatedPercentage = (((it.noVotes ?: 0) / totalVoteCount) * 100).toInt()
            it.toBuilder().percentage(updatedPercentage).build()
        }

        // Check if user selects old value
        if (!oldValueSelected) {
            onComplete(updatedPollViewList)
        }
    }

    private fun updatePollItemsForInstantPollWithMultipleSelection(
        pollViewDataList: List<PollViewData>,
        pollViewData: PollViewData,
        oldSelectedItemsCount: Int,
    ): Pair<List<PollViewData>, Int> {
        var selectedItemsCount = oldSelectedItemsCount
        var totalVoteCount = 0.0
        pollViewDataList.forEach {
            totalVoteCount += it.noVotes ?: 0
        }

        var updatedPollViewList = pollViewDataList.map {
            val isPreviouslySelected = it.isSelected ?: false
            val previousNoOfVotes = it.noVotes ?: 0

            val updatedNoOfVotes: Int
            val updatedIsSelected: Boolean
            if (it.id == pollViewData.id) {
                if (isPreviouslySelected) {
                    updatedNoOfVotes = previousNoOfVotes - 1
                    totalVoteCount -= 1
                    updatedIsSelected = false
                    selectedItemsCount--
                } else {
                    updatedNoOfVotes = previousNoOfVotes + 1
                    totalVoteCount += 1
                    updatedIsSelected = true
                    selectedItemsCount++
                }
                it.toBuilder().isSelected(updatedIsSelected)
                    .noVotes(updatedNoOfVotes).build()
            } else {
                it
            }
        }
        updatedPollViewList = updatedPollViewList.map {
            val updatedPercentage = (((it.noVotes ?: 0) / totalVoteCount) * 100).toInt()
            it.toBuilder().percentage(updatedPercentage).build()
        }

        return Pair(updatedPollViewList, selectedItemsCount)
    }

    fun initChatroomReactionGridView(
        viewData: ReactionsGridViewData?,
        clBubbleRoot: ConstraintLayout,
        bubbleLayout: ConstraintLayout,
        binding: GridMessageReactionsBinding,
        currentMemberId: String,
        chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
        chatroomViewData: ChatroomViewData,
    ) {
        binding.apply {
            if (viewData == null || chatroomViewData.deletedBy != null) {
                clRoot.visibility = View.GONE
            } else {
                if (!viewData.mostRecentReaction.isNullOrEmpty()
                    && viewData.mostRecentReactionCount != null
                ) {
                    firstReaction.text = viewData.mostRecentReaction
                    firstReactionCount.text =
                        viewData.mostRecentReactionCount.toString()
                    clRoot.show()
                    showReactionView(firstReactionCount, firstReaction, layoutReaction1)
                } else {
                    hideReactionView(firstReactionCount, firstReaction, layoutReaction1)
                }

                if (viewData.secondMostRecentReaction != null
                    && viewData.secondMostRecentReactionCount != null
                ) {
                    secondReaction.text = viewData.secondMostRecentReaction
                    secondReactionCount.text =
                        viewData.secondMostRecentReactionCount.toString()
                    showReactionView(secondReactionCount, secondReaction, layoutReaction2)
                } else {
                    hideReactionView(secondReactionCount, secondReaction, layoutReaction2)
                }

                if (viewData.moreThanTwoReactionsPresent == true) {
                    ivDots.show()
                    showMoreReactionView(ivDots)
                } else {
                    hideMoreReactionView(ivDots)
                }

                val gridRoot = clRoot
                val set = ConstraintSet()
                set.clone(clBubbleRoot)
                set.clear(gridRoot.id, ConstraintSet.END)
                set.clear(gridRoot.id, ConstraintSet.START)

                val uuid = chatroomViewData.memberViewData.sdkClientInfo.uuid
                if (uuid == currentMemberId) {
                    set.connect(
                        gridRoot.id,
                        ConstraintSet.END,
                        bubbleLayout.id,
                        ConstraintSet.END
                    )
                } else {
                    set.connect(
                        gridRoot.id,
                        ConstraintSet.START,
                        bubbleLayout.id,
                        ConstraintSet.START
                    )
                }
                set.applyTo(clBubbleRoot)

                initChatroomGridClickListeners(
                    binding,
                    chatroomDetailAdapterListener,
                    chatroomViewData
                )
            }

        }
    }

    private fun initChatroomGridClickListeners(
        binding: GridMessageReactionsBinding,
        chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
        chatroomViewData: ChatroomViewData,
    ) {
        binding.layoutReaction1.setOnClickListener {
            chatroomDetailAdapterListener.chatroomEmoticonGridClicked(
                chatroomViewData,
                binding.firstReaction.text.toString(),
                1
            )
        }
        binding.layoutReaction2.setOnClickListener {
            chatroomDetailAdapterListener.chatroomEmoticonGridClicked(
                chatroomViewData,
                binding.secondReaction.text.toString(), 2
            )
        }
        binding.ivDots.setOnClickListener {
            chatroomDetailAdapterListener.chatroomEmoticonGridClicked(
                chatroomViewData,
                null, 3
            )
        }
    }

    private fun showReactionView(reactionCount: TextView, reaction: TextView, background: View) {
        reactionCount.show()
        reaction.show()
        background.show()
    }

    private fun hideReactionView(reactionCount: TextView, reaction: TextView, background: View) {
        reactionCount.hide()
        reaction.hide()
        background.hide()
    }

    private fun showMoreReactionView(ivDots: ImageView) {
        ivDots.show()
    }

    private fun hideMoreReactionView(ivDots: ImageView) {
        ivDots.hide()
    }

    fun isReactionHintViewShown(
        lastItem: Boolean?,
        hasUserReactedOnce: Boolean,
        noOfTimesHintShown: Int,
        totalNoOfHintsAllowed: Int,
        tvDoubleTap: TextView,
        memberViewData: MemberViewData,
        currentMemberId: String,
        clBubbleRoot: ConstraintLayout,
        bubbleLayout: ConstraintLayout,
    ): Boolean {
        val set = ConstraintSet()
        set.clone(clBubbleRoot)
        set.clear(tvDoubleTap.id, ConstraintSet.END)
        set.clear(tvDoubleTap.id, ConstraintSet.START)

        val uuid = memberViewData.sdkClientInfo.uuid
        if (uuid == currentMemberId) {
            set.connect(
                tvDoubleTap.id,
                ConstraintSet.END,
                bubbleLayout.id,
                ConstraintSet.END
            )
        } else {
            set.connect(
                tvDoubleTap.id,
                ConstraintSet.START,
                bubbleLayout.id,
                ConstraintSet.START
            )
        }
        set.applyTo(clBubbleRoot)
        if (hasUserReactedOnce || (noOfTimesHintShown > totalNoOfHintsAllowed) || lastItem == false) {
            tvDoubleTap.visibility = View.GONE
        } else if (lastItem == true) {
            tvDoubleTap.visibility = View.VISIBLE
        } else {
            tvDoubleTap.visibility = View.GONE
        }
        return tvDoubleTap.visibility == View.VISIBLE
    }

    fun initMessageReactionGridView(
        viewData: ReactionsGridViewData?,
        clBubbleRoot: ConstraintLayout,
        bubbleLayout: ConstraintLayout,
        binding: GridMessageReactionsBinding,
        currentMemberId: String,
        chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
        conversation: ConversationViewData,
    ) {
        binding.apply {
            if (viewData == null || conversation.deletedBy != null) {
                clRoot.visibility = View.GONE
            } else {
                if (!viewData.mostRecentReaction.isNullOrEmpty()
                    && viewData.mostRecentReactionCount != null
                ) {
                    firstReaction.text = viewData.mostRecentReaction
                    firstReactionCount.text =
                        viewData.mostRecentReactionCount.toString()
                    clRoot.visibility = View.VISIBLE
                    showReactionView(
                        firstReactionCount,
                        firstReaction,
                        layoutReaction1
                    )
                } else {
                    hideReactionView(
                        firstReactionCount,
                        firstReaction,
                        layoutReaction1
                    )
                }

                if (viewData.secondMostRecentReaction != null
                    && viewData.secondMostRecentReactionCount != null
                ) {
                    secondReaction.text = viewData.secondMostRecentReaction
                    secondReactionCount.text =
                        viewData.secondMostRecentReactionCount.toString()
                    showReactionView(
                        secondReactionCount,
                        secondReaction,
                        layoutReaction2
                    )
                } else {
                    hideReactionView(
                        secondReactionCount,
                        secondReaction,
                        layoutReaction2
                    )
                }

                if (viewData.moreThanTwoReactionsPresent == true) {
                    ivDots.visibility = View.VISIBLE
                    showMoreReactionView(ivDots)
                } else {
                    hideMoreReactionView(ivDots)
                }

                val gridRoot = clRoot
                val set = ConstraintSet()
                set.clone(clBubbleRoot)
                set.clear(gridRoot.id, ConstraintSet.END)
                set.clear(gridRoot.id, ConstraintSet.START)

                val uuid = conversation.memberViewData.sdkClientInfo.uuid
                if (uuid == currentMemberId) {
                    set.connect(
                        gridRoot.id,
                        ConstraintSet.END,
                        bubbleLayout.id,
                        ConstraintSet.END
                    )
                } else {
                    set.connect(
                        gridRoot.id,
                        ConstraintSet.START,
                        bubbleLayout.id,
                        ConstraintSet.START
                    )
                }
                set.applyTo(clBubbleRoot)

                initGridClickListeners(
                    binding,
                    chatroomDetailAdapterListener,
                    conversation
                )
            }
        }
    }

    private fun initGridClickListeners(
        binding: GridMessageReactionsBinding,
        chatroomDetailAdapterListener: ChatroomDetailAdapterListener,
        conversation: ConversationViewData,
    ) {
        binding.apply {
            layoutReaction1.setOnClickListener {
                chatroomDetailAdapterListener.emoticonGridClicked(
                    conversation,
                    firstReaction.text.toString(),
                    1
                )
            }
            layoutReaction2.setOnClickListener {
                chatroomDetailAdapterListener.emoticonGridClicked(
                    conversation,
                    secondReaction.text.toString(), 2
                )
            }
            ivDots.setOnClickListener {
                chatroomDetailAdapterListener.emoticonGridClicked(
                    conversation,
                    null, 3
                )
            }
        }
    }

    fun initReactionButton(
        ivReaction: ImageView,
        conversation: ConversationViewData,
        currentMemberId: String,
    ) {
        if (conversation.isDeleted()) {
            ivReaction.hide()
        } else {
            val uuid = conversation.memberViewData.sdkClientInfo.uuid
            if (uuid == currentMemberId) {
                ivReaction.hide()
            } else {
                ivReaction.show()
            }
        }
    }

    /***
     * This function helps to finds the text between **<text>** to make it as bold using [SpannableString]
     */
    private fun decodeBoldText(tvConversation: TextView) {
        val text = tvConversation.text
        val spannableStringBuilder = SpannableStringBuilder()
        val regex =
            "\\*\\*(.*?)\\*\\*".toRegex() // Regular expression to find text between double asterisks

        var lastIndex = 0
        val matches = regex.findAll(text)

        for (match in matches) {
            val matchStart = match.range.first
            val matchEnd = match.range.last

            // Append the text before the match
            spannableStringBuilder.append(text.subSequence(lastIndex, matchStart))

            // Get the bold text without the asterisks
            val boldText = match.groupValues[1]

            // Create a SpannableStringBuilder for the bold text
            val boldSpannable = SpannableStringBuilder(boldText)
            boldSpannable.setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                boldText.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Append the bold text to the final output
            spannableStringBuilder.append(boldSpannable)

            // Update the last index to after the current match
            lastIndex = matchEnd + 1
        }

        // Append any remaining text after the last match
        spannableStringBuilder.append(text.subSequence(lastIndex, text.length))

        tvConversation.text = spannableStringBuilder
    }
}