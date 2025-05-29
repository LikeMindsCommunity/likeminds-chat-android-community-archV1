package com.likeminds.chatmm.chatroom.detail.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.*
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.view.WindowInsets
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.model.ChatroomViewData
import com.likeminds.chatmm.chatroom.detail.model.TYPE_ANNOUNCEMENT
import com.likeminds.chatmm.chatroom.detail.view.YouTubeVideoPlayerPopup
import com.likeminds.chatmm.conversation.model.AttachmentViewData
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.ValueUtils.containsUrl
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.chatmm.utils.model.ITEM_CHAT_ROOM
import com.likeminds.chatmm.utils.model.ITEM_CHAT_ROOM_ANNOUNCEMENT
import com.likeminds.likemindschat.chatroom.model.Chatroom
import com.likeminds.likemindschat.conversation.model.ConversationState

object ChatroomUtil {

    /**
     * Get chatroom view type based on the chatroom data
     */
    fun getChatroomViewType(chatroom: Chatroom?): Int {
        return if (chatroom?.type == TYPE_ANNOUNCEMENT) {
            ITEM_CHAT_ROOM_ANNOUNCEMENT
        } else {
            ITEM_CHAT_ROOM
        }
    }

    fun getMediaCount(mediaType: String, attachments: List<AttachmentViewData>?): Int {
        return attachments?.count { it.type == mediaType } ?: 0
    }

    /**
     * Get conversation type based on the conversation data
     */
    fun getConversationType(conversation: ConversationViewData?): String {
        if (conversation == null) return ""
        val imageCount = getMediaCount(IMAGE, conversation.attachments)
        val gifCount = getMediaCount(GIF, conversation.attachments)
        val videoCount = getMediaCount(VIDEO, conversation.attachments)
        val pdfCount = getMediaCount(PDF, conversation.attachments)
        val audioCount = getMediaCount(AUDIO, conversation.attachments)
        val voiceNoteCount = getMediaCount(VOICE_NOTE, conversation.attachments)
        return when {
            imageCount > 0 && videoCount > 0 -> "image, video"
            imageCount > 0 -> "image"
            gifCount > 0 -> "gif"
            videoCount > 0 -> "video"
            pdfCount > 0 -> "doc"
            audioCount > 0 -> "audio"
            voiceNoteCount > 0 -> "voice note"
            conversation.ogTags?.url != null -> "link"
            else -> "text"
        }
    }

    /**
     * @return a Triple : First -> SpannableStringBuilder | Second -> FirstMediaType | Third -> drawable list
     **/
    fun getHomeScreenAttachmentData(
        context: Context,
        conversation: ConversationViewData?
    ): Triple<SpannableStringBuilder, String, ArrayList<Pair<Int, Int>>> {
        val drawableList = ArrayList<Pair<Int, Int>>()
        val firstMediaType = getFirstMediaType(conversation?.attachments)

        val videoDrawable = getVideoDrawable(conversation)
        val otherDrawable = getAttachmentTypeDrawable(conversation)
        if (firstMediaType == VIDEO) {
            if (videoDrawable != null) drawableList.add(videoDrawable)
            if (otherDrawable != null) drawableList.add(otherDrawable)
        } else {
            if (otherDrawable != null) drawableList.add(otherDrawable)
            if (videoDrawable != null) drawableList.add(videoDrawable)
        }

        val spannableStringBuilder = SpannableStringBuilder()
        if (drawableList.size > 0) {
            drawableList.forEach { pair ->
                if (pair.second > 1 || drawableList.size > 1) {
                    val spannableString = SpannableString("${pair.second} #  ")
                    spannableString.setSpan(
                        ImageSpan(context, pair.first),
                        pair.second.toString().length + 1,
                        pair.second.toString().length + 2,
                        Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    spannableStringBuilder.append(spannableString)
                } else {
                    val spannableString = SpannableString("#  ")
                    spannableString.setSpan(
                        ImageSpan(context, pair.first),
                        0, 1, Spannable.SPAN_INCLUSIVE_INCLUSIVE
                    )
                    spannableStringBuilder.append(spannableString)
                }
            }
        }
        return Triple(spannableStringBuilder, firstMediaType, drawableList)
    }

    fun hasOriginalConversationId(conversation: ConversationViewData?): Boolean {
        return !conversation?.id.isNullOrEmpty()
                && conversation?.id?.startsWith("-") == false
    }

    fun getFirstMediaType(attachmentList: List<AttachmentViewData>?): String {
        return when (val viewData = attachmentList?.minByOrNull { it.index ?: 0 }) {
            is AttachmentViewData -> viewData.type
            else -> ""
        }
    }

    private fun getVideoDrawable(conversation: ConversationViewData?): Pair<Int, Int>? {
        val videosCount = conversation?.attachments?.filter { it.type == VIDEO }?.size ?: 0
        return when {
            (videosCount > 0) -> Pair(R.drawable.lm_chat_ic_video_header, videosCount)
            else -> null
        }
    }

    fun getTopicMediaData(
        context: Context,
        conversation: ConversationViewData?
    ): SpannableStringBuilder {
        val tripleOfSpannableAndMedia = getHomeScreenAttachmentData(context, conversation)
        val firstMediaType = tripleOfSpannableAndMedia.second
        val hasAnswer = !conversation?.answer.isNullOrEmpty()
        val hasLink = conversation?.ogTags != null
        val hasPDFs = conversation?.attachments?.any { it.type == PDF }

        val spannableStringBuilder = tripleOfSpannableAndMedia.first
        val drawableList = tripleOfSpannableAndMedia.third
        if (drawableList.size > 0) {
            drawableList.forEach { pair ->
                if (pair.second > 1 || drawableList.size > 1) {
                    when {
                        hasAnswer -> {
                            val decodeString = MemberTaggingDecoder.decode(conversation?.answer)
                            spannableStringBuilder.append(decodeString)
                        }

                        firstMediaType == IMAGE -> spannableStringBuilder.append("Photos")
                        firstMediaType == VIDEO -> spannableStringBuilder.append("Videos")
                        firstMediaType == GIF -> spannableStringBuilder.append("GIFs")
                        firstMediaType == AUDIO -> spannableStringBuilder.append("Audios")
                        firstMediaType == VOICE_NOTE -> spannableStringBuilder.append("Voice Notes")
                        hasLink -> {
                            spannableStringBuilder.append(conversation?.answer)
                        }

                        hasPDFs == true -> {
                            spannableStringBuilder.append("Documents")
                        }
                    }
                } else {
                    when {
                        hasAnswer -> {
                            val decodeString = MemberTaggingDecoder.decode(conversation?.answer)
                            spannableStringBuilder.append(decodeString)
                        }

                        firstMediaType == IMAGE -> spannableStringBuilder.append("Photo")
                        firstMediaType == VIDEO -> spannableStringBuilder.append("Video")
                        firstMediaType == GIF -> spannableStringBuilder.append("GIF")
                        firstMediaType == AUDIO -> spannableStringBuilder.append("Audio")
                        firstMediaType == VOICE_NOTE -> spannableStringBuilder.append("Voice Note")
                        hasLink -> {
                            spannableStringBuilder.append(conversation?.answer)
                        }

                        hasPDFs == true -> {
                            spannableStringBuilder.append("Document")
                        }
                    }
                }
            }
        }
        return spannableStringBuilder
    }

    private fun getAttachmentTypeDrawable(conversation: ConversationViewData?): Pair<Int, Int>? {
        val imagesCount = conversation?.attachments?.filter { it.type == IMAGE }?.size ?: 0
        val gifsCount = conversation?.attachments?.filter { it.type == GIF }?.size ?: 0
        val pdfsCount = conversation?.attachments?.filter { it.type == PDF }?.size ?: 0
        val audiosCount = conversation?.attachments?.filter { it.type == AUDIO }?.size ?: 0
        val voiceNoteCount =
            conversation?.attachments?.filter { it.type == VOICE_NOTE }?.size ?: 0

        return when {
            imagesCount > 0 -> {
                Pair(R.drawable.lm_chat_ic_photo_header, imagesCount)
            }

            gifsCount > 0 -> {
                Pair(R.drawable.lm_chat_ic_gif_header, gifsCount)
            }

            pdfsCount > 0 -> {
                Pair(R.drawable.lm_chat_ic_document_header, pdfsCount)
            }

            audiosCount > 0 -> {
                Pair(R.drawable.lm_chat_ic_audio_header_grey, audiosCount)
            }

            voiceNoteCount > 0 -> {
                Pair(R.drawable.lm_chat_ic_voice_note_header_grey, voiceNoteCount)
            }

            conversation?.ogTags != null || conversation?.answer
                .containsUrl() -> {
                Pair(R.drawable.lm_chat_ic_link_header, 1)
            }

            conversation?.state == ConversationState.POLL.value -> {
                Pair(R.drawable.lm_chat_ic_micro_poll, 1)
            }

            else -> null
        }
    }

    fun getDeletedMessage(
        context: Context,
        conversation: ConversationViewData,
        currentMemberId: String,
    ): String {
        val conversationCreator = conversation.memberViewData.sdkClientInfo.uuid
        return if (conversationCreator == currentMemberId) {
            val deletedByUUID = conversation.deletedByMember?.sdkClientInfo?.uuid
            if (deletedByUUID == currentMemberId) {
                context.getString(R.string.lm_chat_you_deleted_this_message)
            } else {
                context.getString(R.string.lm_chat_your_message_was_deleted_by_cm)
            }
        } else {
            val deletedByUUID = conversation.deletedByMember?.sdkClientInfo?.uuid
            if (conversationCreator == deletedByUUID) {
                context.getString(R.string.lm_chat_this_message_was_deleted)
            } else {
                context.getString(R.string.lm_chat_this_message_was_deleted_by_cm)
            }
        }
    }

    fun getTypeDrawableId(type: Int?): Int? {
        return when (type) {
            TYPE_ANNOUNCEMENT -> {
                R.drawable.lm_chat_ic_announcement_room
            }

            else -> {
                return null
            }
        }
    }

    fun getTypeDrawable(context: Context, type: Int?): Drawable? {
        val drawableId = getTypeDrawableId(type)
        return getDrawable(context, drawableId)
    }

    fun getDrawable(context: Context, drawableId: Int?): Drawable? {
        return if (drawableId != null) ContextCompat.getDrawable(context, drawableId)
        else null
    }

    fun getLastConversationTextForHome(lastConversation: ConversationViewData?): String {
        var lastConversationAnswer = lastConversation?.answer
        //If it's just a link with no answer show the title of link
        if (lastConversationAnswer.isNullOrBlank()) {
            val linkTitle = lastConversation?.ogTags?.title
            if (!linkTitle.isNullOrBlank()) {
                lastConversationAnswer = linkTitle
            }
        }

        //If it's just a media with no answer
        if (lastConversationAnswer.isNullOrBlank()) {
            val imagesCount =
                lastConversation?.attachments?.filter { it.type == IMAGE }?.size ?: 0
            val gifsCount =
                lastConversation?.attachments?.filter { it.type == GIF }?.size ?: 0
            val videoCount =
                lastConversation?.attachments?.filter { it.type == VIDEO }?.size ?: 0
            val audioCount =
                lastConversation?.attachments?.filter { it.type == AUDIO }?.size ?: 0
            val pdfCount = lastConversation?.attachments?.filter { it.type == PDF }?.size ?: 0
            val voiceNoteCount =
                lastConversation?.attachments?.filter { it.type == VOICE_NOTE }?.size ?: 0
            val firstMediaType = getFirstMediaType(lastConversation?.attachments)
            when {
                imagesCount > 0 && videoCount > 0 -> {
                    lastConversationAnswer = ""
                }

                firstMediaType == IMAGE -> {
                    lastConversationAnswer = if (imagesCount > 1) "Photos" else "Photo"
                }

                firstMediaType == GIF -> {
                    lastConversationAnswer = if (gifsCount > 1) "Gifs" else "GIF"
                }

                firstMediaType == VIDEO -> {
                    lastConversationAnswer = if (videoCount > 1) "Videos" else "Video"
                }

                firstMediaType == PDF -> {
                    lastConversationAnswer = if (pdfCount > 1) "Documents" else "Document"
                }

                firstMediaType == AUDIO -> {
                    lastConversationAnswer = if (audioCount > 1) "Audios" else "Audio"
                }

                firstMediaType == VOICE_NOTE -> {
                    lastConversationAnswer = if (voiceNoteCount > 1) "Voice Notes" else "Voice Note"
                }
            }
        }
        return lastConversationAnswer ?: ""
    }

    fun ChatroomViewData.getTypeName(): String {
        return when (type) {
            0 -> "normal"
            1 -> "intro"
            2 -> "event"
            3 -> "poll"
            6 -> "public_event"
            7 -> "announcement"
            9 -> "introduction_rooms"
            10 -> "direct messages"
            else -> "normal"
        }
    }

    fun setVidePlayerDimensions(
        activity: Activity,
        inAppYouTubePlayer: YouTubeVideoPlayerPopup?,
        isFullScreen: Boolean,
    ) {
        val fullWidth: Int
        val fullHeight: Int
        fullWidth = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = activity.windowManager.currentWindowMetrics
            val insets =
                windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            fullHeight = windowMetrics.bounds.height() - insets.top - insets.bottom
            windowMetrics.bounds.width()
        } else {
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            fullHeight = displayMetrics.heightPixels
            displayMetrics.widthPixels
        }
        if (isFullScreen) {
            inAppYouTubePlayer?.update(
                fullWidth, fullHeight
            )
        } else {
            val height = ViewUtils.dpToPx(208)
            val margin = ViewUtils.dpToPx(16)
            inAppYouTubePlayer?.update(
                fullWidth - margin, height
            )
        }
    }

    fun removeTemporaryText(answer: String?): String {
        if (answer.isNullOrEmpty()) {
            return ""
        }
        val videoText = "\n * This is a video message. Please update your app *"
        val gifText = "\n * This is a gif message. Please update your app *"
        val audioText = "\n * This is an audio message. Please update your app *"
        return answer.replace(videoText, "").replace(gifText, "").replace(audioText, "")
    }

    @SuppressLint("InlinedApi")
    @Suppress("Deprecation")
    fun setStatusBarColor(activity: Activity) {
        val window = activity.window
        val statusBarColor = LMChatAppearance.getHeaderColor()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            windowInsetsController.isAppearanceLightStatusBars = true
            window.statusBarColor = statusBarColor
        } else {
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = true

            window.decorView.setBackgroundColor(statusBarColor)
            window.insetsController?.hide(WindowInsets.Type.statusBars())
            window.insetsController?.show(WindowInsets.Type.statusBars())
        }
    }
}