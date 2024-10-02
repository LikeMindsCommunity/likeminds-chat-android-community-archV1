package com.likeminds.chatmm.conversation.model

import android.net.Uri
import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import com.likeminds.chatmm.chatroom.detail.model.ChatroomViewData
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.utils.model.*
import kotlinx.parcelize.Parcelize

@Parcelize
class AttachmentViewData private constructor(
    @SerializedName("id")
    val id: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("url")
    val url: String?,
    val uri: Uri,
    @SerializedName("type")
    val type: String,
    val index: Int?,
    @SerializedName("width")
    val width: Int?,
    @SerializedName("height")
    val height: Int?,
    val title: String?,
    val subTitle: String?,
    val attachments: List<AttachmentViewData>?,
    val parentConversation: ConversationViewData?,
    val parentChatroom: ChatroomViewData?,
    val parentViewItemPosition: Int?,
    val awsFolderPath: String?,
    val localFilePath: String?,
    val thumbnail: String?,
    val thumbnailAWSFolderPath: String?,
    val thumbnailLocalFilePath: String?,
    @SerializedName("meta")
    val meta: AttachmentMetaViewData?,
    val progress: Int?,
    val currentDuration: String,
    val mediaState: String,
    val communityId: Int?,
    val createdAt: Long?,
    val updatedAt: Long?,
    val mediaLeft: Int?,
    val dynamicType: Int?
) : BaseViewType, Parcelable {
    override val viewType: Int
        get() = dynamicType ?: when (type) {
            VIDEO -> {
                ITEM_CHATROOM_VIDEO
            }
            PDF -> {
                ITEM_CHATROOM_PDF
            }
            AUDIO -> {
                ITEM_CREATE_CHATROOM_AUDIO
            }
            else -> {
                ITEM_CHATROOM_IMAGE
            }
        }

    class Builder {
        private var id: String? = null
        private var name: String? = null
        private var url: String? = null
        private var uri: Uri = Uri.parse("")
        private var type: String = ""
        private var index: Int? = null
        private var width: Int? = null
        private var height: Int? = null
        private var title: String? = null
        private var subTitle: String? = null
        private var attachments: List<AttachmentViewData>? = null
        private var parentConversation: ConversationViewData? = null
        private var parentChatroom: ChatroomViewData? = null
        private var parentViewItemPosition: Int? = null
        private var awsFolderPath: String? = null
        private var localFilePath: String? = null
        private var thumbnail: String? = null
        private var thumbnailAWSFolderPath: String? = null
        private var thumbnailLocalFilePath: String? = null
        private var meta: AttachmentMetaViewData? = null
        private var progress: Int? = null
        private var currentDuration: String = "00:00"
        private var mediaState: String = MEDIA_ACTION_NONE
        private var communityId: Int? = null
        private var createdAt: Long? = null
        private var updatedAt: Long? = null
        private var mediaLeft: Int? = null
        private var dynamicType: Int? = null

        fun id(id: String?) = apply { this.id = id }
        fun name(name: String?) = apply { this.name = name }
        fun url(url: String?) = apply { this.url = url }
        fun uri(uri: Uri) = apply { this.uri = uri }
        fun type(type: String) = apply { this.type = type }
        fun index(index: Int?) = apply { this.index = index }
        fun width(width: Int?) = apply { this.width = width }
        fun height(height: Int?) = apply { this.height = height }
        fun title(title: String?) = apply { this.title = title }
        fun subTitle(subTitle: String?) = apply { this.subTitle = subTitle }
        fun attachments(attachments: List<AttachmentViewData>?) =
            apply { this.attachments = attachments }

        fun parentConversation(parentConversation: ConversationViewData?) =
            apply { this.parentConversation = parentConversation }

        fun parentChatroom(parentChatroom: ChatroomViewData?) =
            apply { this.parentChatroom = parentChatroom }

        fun parentViewItemPosition(parentViewItemPosition: Int?) =
            apply { this.parentViewItemPosition = parentViewItemPosition }

        fun awsFolderPath(awsFolderPath: String?) = apply { this.awsFolderPath = awsFolderPath }
        fun localFilePath(localFilePath: String?) = apply { this.localFilePath = localFilePath }
        fun thumbnail(thumbnail: String?) = apply { this.thumbnail = thumbnail }
        fun thumbnailAWSFolderPath(thumbnailAWSFolderPath: String?) =
            apply { this.thumbnailAWSFolderPath = thumbnailAWSFolderPath }

        fun thumbnailLocalFilePath(thumbnailLocalFilePath: String?) =
            apply { this.thumbnailLocalFilePath = thumbnailLocalFilePath }

        fun meta(meta: AttachmentMetaViewData?) = apply { this.meta = meta }
        fun progress(progress: Int?) = apply { this.progress = progress }
        fun currentDuration(currentDuration: String) =
            apply { this.currentDuration = currentDuration }

        fun mediaState(mediaState: String) = apply { this.mediaState = mediaState }
        fun communityId(communityId: Int?) = apply { this.communityId = communityId }
        fun createdAt(createdAt: Long?) = apply { this.createdAt = createdAt }
        fun updatedAt(updatedAt: Long?) = apply { this.updatedAt = updatedAt }
        fun mediaLeft(mediaLeft: Int?) = apply { this.mediaLeft = mediaLeft }
        fun dynamicType(dynamicType: Int?) = apply { this.dynamicType = dynamicType }

        fun build() = AttachmentViewData(
            id,
            name,
            url,
            uri,
            type,
            index,
            width,
            height,
            title,
            subTitle,
            attachments,
            parentConversation,
            parentChatroom,
            parentViewItemPosition,
            awsFolderPath,
            localFilePath,
            thumbnail,
            thumbnailAWSFolderPath,
            thumbnailLocalFilePath,
            meta,
            progress,
            currentDuration,
            mediaState,
            communityId,
            createdAt,
            updatedAt,
            mediaLeft,
            dynamicType
        )
    }

    fun toBuilder(): Builder {
        return Builder().id(id)
            .name(name)
            .url(url)
            .uri(uri)
            .type(type)
            .index(index)
            .width(width)
            .height(height)
            .title(title)
            .subTitle(subTitle)
            .attachments(attachments)
            .parentConversation(parentConversation)
            .parentChatroom(parentChatroom)
            .parentViewItemPosition(parentViewItemPosition)
            .awsFolderPath(awsFolderPath)
            .localFilePath(localFilePath)
            .thumbnail(thumbnail)
            .thumbnailAWSFolderPath(thumbnailAWSFolderPath)
            .thumbnailLocalFilePath(thumbnailLocalFilePath)
            .meta(meta)
            .progress(progress)
            .currentDuration(currentDuration)
            .mediaState(mediaState)
            .communityId(communityId)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .mediaLeft(mediaLeft)
            .dynamicType(dynamicType)
    }
}