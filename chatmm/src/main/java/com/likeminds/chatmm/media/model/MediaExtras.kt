package com.likeminds.chatmm.media.model

import android.os.Parcelable
import com.giphy.sdk.core.models.Media
import com.likeminds.chatmm.conversation.model.AttachmentViewData
import kotlinx.parcelize.Parcelize

@Parcelize
class MediaExtras private constructor(
    val isExternallyShared: Boolean,
    val mediaScreenType: Int,
    val title: String?,
    val subtitle: String?,
    val attachments: List<AttachmentViewData>?,
    val singleUriData: SingleUriData?,
    val mediaUris: ArrayList<SingleUriData>?,
    val conversation: String?,
    val text: String?,
    val position: Int?,
    val medias: List<MediaSwipeViewData>?,
    val chatroomId: String?,
    val chatroomName: String?,
    val communityId: Int?,
    val cropSquare: Boolean?,
    val downloadableContentTypes: List<String>?,
    val chatroomType: String?,
    val communityName: String?,
    val searchKey: String?,
    val giphyMedia: Media?,
    val conversationId: String?,
    val isSecretChatroom: Boolean?,
    val isTaggingEnabled: Boolean,
    val allowMultipleSelect: Boolean
) : Parcelable {

    class Builder {
        private var isExternallyShared: Boolean = false
        private var mediaScreenType: Int = -1
        private var title: String? = null
        private var subtitle: String? = null
        private var attachments: List<AttachmentViewData>? = null
        private var singleUriData: SingleUriData? = null
        private var mediaUris: ArrayList<SingleUriData>? = null
        private var conversation: String? = null
        private var text: String? = null
        private var position: Int? = null
        private var medias: List<MediaSwipeViewData>? = null
        private var chatroomId: String? = null
        private var chatroomName: String? = null
        private var communityId: Int? = null
        private var cropSquare: Boolean? = null
        private var downloadableContentTypes: List<String>? = null
        private var chatroomType: String? = null
        private var communityName: String? = null
        private var searchKey: String? = null
        private var giphyMedia: Media? = null
        private var conversationId: String? = null
        private var isSecretChatroom: Boolean? = null
        private var isTaggingEnabled: Boolean = true
        private var allowMultipleSelect: Boolean = true

        fun isExternallyShared(isExternallyShared: Boolean) =
            apply { this.isExternallyShared = isExternallyShared }

        fun mediaScreenType(mediaScreenType: Int) = apply { this.mediaScreenType = mediaScreenType }
        fun title(title: String?) = apply { this.title = title }
        fun subtitle(subtitle: String?) = apply { this.subtitle = subtitle }
        fun attachments(attachments: List<AttachmentViewData>?) =
            apply { this.attachments = attachments }

        fun singleUriData(singleUriData: SingleUriData?) =
            apply { this.singleUriData = singleUriData }

        fun mediaUris(mediaUris: ArrayList<SingleUriData>?) = apply { this.mediaUris = mediaUris }

        fun conversation(conversation: String?) = apply { this.conversation = conversation }
        fun text(text: String?) = apply { this.text = text }
        fun position(position: Int?) = apply { this.position = position }
        fun medias(medias: List<MediaSwipeViewData>?) = apply { this.medias = medias }
        fun chatroomId(chatroomId: String?) = apply { this.chatroomId = chatroomId }
        fun chatroomName(chatroomName: String?) = apply { this.chatroomName = chatroomName }
        fun communityId(communityId: Int?) = apply { this.communityId = communityId }
        fun cropSquare(cropSquare: Boolean?) = apply { this.cropSquare = cropSquare }
        fun downloadableContentTypes(downloadableContentTypes: List<String>?) =
            apply { this.downloadableContentTypes = downloadableContentTypes }

        fun chatroomType(chatroomType: String?) = apply { this.chatroomType = chatroomType }
        fun communityName(communityName: String?) = apply { this.communityName = communityName }
        fun searchKey(searchKey: String?) = apply { this.searchKey = searchKey }
        fun giphyMedia(giphyMedia: Media?) = apply { this.giphyMedia = giphyMedia }
        fun conversationId(conversationId: String?) = apply { this.conversationId = conversationId }
        fun isSecretChatroom(isSecretChatroom: Boolean?) =
            apply { this.isSecretChatroom = isSecretChatroom }

        fun isTaggingEnabled(isTaggingEnabled: Boolean) = apply {
            this.isTaggingEnabled = isTaggingEnabled
        }

        fun allowMultipleSelect(allowMultipleSelect: Boolean) = apply {
            this.allowMultipleSelect = allowMultipleSelect
        }

        fun build() = MediaExtras(
            isExternallyShared,
            mediaScreenType,
            title,
            subtitle,
            attachments,
            singleUriData,
            mediaUris,
            conversation,
            text,
            position,
            medias,
            chatroomId,
            chatroomName,
            communityId,
            cropSquare,
            downloadableContentTypes,
            chatroomType,
            communityName,
            searchKey,
            giphyMedia,
            conversationId,
            isSecretChatroom,
            isTaggingEnabled,
            allowMultipleSelect
        )
    }

    fun toBuilder(): Builder {
        return Builder().isExternallyShared(isExternallyShared)
            .mediaScreenType(mediaScreenType)
            .title(title)
            .subtitle(subtitle)
            .attachments(attachments)
            .singleUriData(singleUriData)
            .mediaUris(mediaUris)
            .conversation(conversation)
            .text(text)
            .position(position)
            .medias(medias)
            .chatroomId(chatroomId)
            .chatroomName(chatroomName)
            .communityId(communityId)
            .cropSquare(cropSquare)
            .downloadableContentTypes(downloadableContentTypes)
            .chatroomType(chatroomType)
            .communityName(communityName)
            .searchKey(searchKey)
            .giphyMedia(giphyMedia)
            .conversationId(conversationId)
            .isSecretChatroom(isSecretChatroom)
            .isTaggingEnabled(isTaggingEnabled)
            .allowMultipleSelect(allowMultipleSelect)
    }
}