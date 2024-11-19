package com.likeminds.chatmm.conversation.util

import android.content.Context
import androidx.annotation.DrawableRes
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.model.ChatReplyViewData
import com.likeminds.chatmm.chatroom.detail.model.ChatroomViewData
import com.likeminds.chatmm.chatroom.detail.util.ChatroomUtil
import com.likeminds.chatmm.conversation.model.*
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.member.model.MemberState
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.MemberUtil

object ChatReplyUtil {

    fun getChatRoomReplyData(
        chatRoom: ChatroomViewData,
        currentMemberId: String,
        context: Context,
        checkForDeletedChatroom: Boolean = true,
        type: String? = null
    ): ChatReplyViewData {
        val memberViewData = chatRoom.memberViewData
        return if (checkForDeletedChatroom && chatRoom.deletedBy != null) {
            ChatReplyViewData.Builder()
                .memberName(MemberUtil.getMemberNameForDisplay(memberViewData, currentMemberId))
                .isMessageDeleted(true)
                .deleteMessage(
                    ChatroomUtil.getDeletedMessage(context, chatRoom, currentMemberId)
                )
                .attachmentType("")
                .build()
        } else {
            getReplyData(
                chatRoom.title,
                memberViewData,
                currentMemberId,
                type = type
            )
        }
    }

    fun getConversationReplyData(
        conversation: ConversationViewData,
        currentMemberId: String,
        context: Context,
        checkForDeletedConversation: Boolean = true,
        type: String? = null
    ): ChatReplyViewData {
        return if (checkForDeletedConversation && conversation.deletedBy != null) {
            ChatReplyViewData.Builder()
                .memberName(
                    MemberUtil.getMemberNameForDisplay(
                        conversation.memberViewData,
                        currentMemberId
                    )
                )
                .isMessageDeleted(true)
                .deleteMessage(
                    ChatroomUtil.getDeletedMessage(
                        context,
                        conversation,
                        currentMemberId
                    )
                )
                .attachmentType("")
                .build()
        } else {
            getReplyData(
                conversation.answer,
                conversation.memberViewData,
                currentMemberId,
                conversation.attachments,
                conversation.ogTags,
                conversationState = conversation.state,
                type = type
            )
        }
    }

    private fun getReplyData(
        answer: String,
        memberViewData: MemberViewData,
        currentMemberId: String,
        attachments: MutableList<AttachmentViewData>? = null,
        linkOgTags: LinkOGTagsViewData? = null,
        @ConversationState conversationState: Int? = null,
        type: String?
    ): ChatReplyViewData {
        val memberName = MemberUtil.getMemberNameForDisplay(memberViewData, currentMemberId)
        val memberState = MemberState.getMemberState(memberViewData.state)
        val uuid = memberViewData.sdkClientInfo.uuid

        val sortedAttachments = attachments?.sortedBy { it.index }.orEmpty()

        val imagesCount = sortedAttachments.filter { it.type == IMAGE }.size
        val gifsCount = sortedAttachments.filter { it.type == GIF }.size
        val pdfsCount = sortedAttachments.filter { it.type == PDF }.size
        val videosCount = sortedAttachments.filter { it.type == VIDEO }.size
        val audiosCount = sortedAttachments.filter { it.type == AUDIO }.size
        val voiceNoteCount = sortedAttachments.filter { it.type == VOICE_NOTE }.size

        val firstMediaType = ChatroomUtil.getFirstMediaType(sortedAttachments)
        val imageUrl = getImageUrlForReplyView(
            sortedAttachments,
            linkOgTags,
            firstMediaType,
            imagesCount,
            gifsCount,
            videosCount,
            audiosCount
        )

        val messageBuilder = StringBuilder()

        // Add image at start of message
        val drawable = getImageDrawableForReplyView(
            linkOgTags,
            firstMediaType,
            imagesCount,
            gifsCount,
            videosCount,
            pdfsCount,
            conversationState,
            audiosCount,
            voiceNoteCount
        )
        if (drawable != null) {
            messageBuilder.append("  ")
        }

        // Add text message
        messageBuilder.append(
            getDisplayTextForReplyView(
                answer, linkOgTags, firstMediaType,
                imagesCount, gifsCount, videosCount, pdfsCount, audiosCount, voiceNoteCount
            )
        )

        // Add additional medias count
        messageBuilder.append(
            getAdditionalMediaCountForReplyView(
                imagesCount,
                gifsCount,
                videosCount,
                pdfsCount,
                audiosCount,
                voiceNoteCount
            )
        )

        return ChatReplyViewData.Builder().memberName(memberName)
            .conversationText(messageBuilder.toString())
            .drawable(drawable)
            .imageUrl(imageUrl)
            .attachmentType(firstMediaType)
            .repliedMemberId(uuid)
            .repliedMemberState(memberState)
            .type(type)
            .build()
    }

    private fun getImageUrlForReplyView(
        attachments: List<AttachmentViewData>,
        linkViewData: LinkOGTagsViewData?,
        firstMediaType: String,
        imagesCount: Int,
        gifsCount: Int,
        videosCount: Int,
        audiosCount: Int,
    ): String {
        val hasLink = linkViewData != null

        return if ((firstMediaType == IMAGE && imagesCount > 0)) {
            attachments.firstOrNull()?.uri.toString()
        } else if ((firstMediaType == VIDEO && videosCount > 0)
            || (firstMediaType == GIF && gifsCount > 0)
            || (firstMediaType == AUDIO && audiosCount > 0)
        ) {
            attachments.firstOrNull()?.thumbnail ?: attachments.firstOrNull()?.uri.toString()
        } else if (hasLink && !linkViewData?.image.isNullOrEmpty()) {
            linkViewData?.image ?: ""
        } else {
            ""
        }
    }

    @DrawableRes
    private fun getImageDrawableForReplyView(
        linkViewData: LinkOGTagsViewData?,
        firstMediaType: String,
        imagesCount: Int,
        gifsCount: Int,
        videosCount: Int,
        pdfsCount: Int,
        @ConversationState conversationState: Int?,
        audiosCount: Int,
        voiceNoteCount: Int,
    ): Int? {
        val hasLink = linkViewData != null
        return when {
            conversationState == STATE_POLL -> {
                R.drawable.lm_chat_ic_poll_room_header
            }

            firstMediaType == IMAGE && imagesCount > 0 -> {
                R.drawable.lm_chat_ic_photo_header
            }

            firstMediaType == GIF && gifsCount > 0 -> {
                R.drawable.lm_chat_ic_gif_header
            }

            firstMediaType == VIDEO && videosCount > 0 -> {
                R.drawable.lm_chat_ic_video_header
            }

            firstMediaType == PDF && pdfsCount > 0 -> {
                R.drawable.lm_chat_ic_document_header
            }

            firstMediaType == AUDIO && audiosCount > 0 -> {
                R.drawable.lm_chat_ic_audio_header_grey
            }

            firstMediaType == VOICE_NOTE && voiceNoteCount > 0 -> {
                R.drawable.lm_chat_ic_voice_note_header_grey
            }

            hasLink -> {
                R.drawable.lm_chat_ic_link_header
            }

            else -> null
        }
    }

    private fun getDisplayTextForReplyView(
        answer: String?,
        linkViewData: LinkOGTagsViewData?,
        firstMediaType: String,
        imagesCount: Int,
        gifsCount: Int,
        videosCount: Int,
        pdfsCount: Int,
        audiosCount: Int,
        voiceNoteCount: Int,
    ): String {
        val hasLink = linkViewData != null
        return when {
            !answer.isNullOrEmpty() -> answer
            firstMediaType == IMAGE && imagesCount > 0 -> "Photo"
            firstMediaType == GIF && gifsCount > 0 -> "GIF"
            firstMediaType == VIDEO && videosCount > 0 -> "Video"
            firstMediaType == PDF && pdfsCount > 0 -> "Document"
            firstMediaType == AUDIO && audiosCount > 0 -> "Audio"
            firstMediaType == VOICE_NOTE && voiceNoteCount > 0 -> "Voice Note"
            hasLink -> linkViewData?.url ?: ""
            else -> ""
        }
    }

    private fun getAdditionalMediaCountForReplyView(
        imagesCount: Int,
        gifsCount: Int,
        videosCount: Int,
        pdfsCount: Int,
        audiosCount: Int,
        voiceNoteCount: Int,
    ): String {
        val allMediaCount =
            imagesCount + videosCount + pdfsCount + gifsCount + audiosCount + voiceNoteCount
        if (allMediaCount > 1) {
            return String.format(" (+%s more)", allMediaCount - 1)
        }
        return ""
    }

    fun getEditChatRoomData(chatRoom: ChatroomViewData): ChatReplyViewData {
        return ChatReplyViewData.Builder()
            .conversationText(chatRoom.title)
            .isEditMessage(true)
            .attachmentType("")
            .build()
    }

    fun getEditConversationData(conversation: ConversationViewData): ChatReplyViewData {
        return ChatReplyViewData.Builder()
            .conversationText(conversation.answer)
            .isEditMessage(true)
            .attachmentType("")
            .build()
    }
}