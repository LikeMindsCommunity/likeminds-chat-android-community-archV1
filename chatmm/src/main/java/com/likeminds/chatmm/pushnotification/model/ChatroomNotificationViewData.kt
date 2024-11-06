package com.likeminds.chatmm.pushnotification.model

import com.google.gson.annotations.SerializedName
import com.likeminds.chatmm.conversation.model.AttachmentViewData
import com.likeminds.chatmm.member.model.MemberViewData

class ChatroomNotificationViewData private constructor(
    @SerializedName("community_name")
    val communityName: String,
    @SerializedName("chatroom_name")
    val chatroomName: String,
    @SerializedName("chatroom_title")
    val chatroomTitle: String,
    @SerializedName("chatroom_user_name")
    val chatroomUserName: String,
    @SerializedName("chatroom_user_image")
    val chatroomUserImage: String,
    @SerializedName("chatroom_id")
    val chatroomId: String,
    @SerializedName("community_image")
    val communityImage: String,
    @SerializedName("community_id")
    val communityId: Int,
    @SerializedName("route")
    val route: String,
    @SerializedName("chatroom_unread_conversation_count")
    val chatroomUnreadConversationCount: Int,
    @SerializedName("chatroom_last_conversation")
    val chatroomLastConversation: String?,
    @SerializedName("chatroom_last_conversation_id")
    val chatroomLastConversationId: String?,
    @SerializedName("chatroom_last_conversation_user_name")
    val chatroomLastConversationUserName: String?,
    @SerializedName("chatroom_last_conversation_user_image")
    val chatroomLastConversationUserImage: String?,
    @SerializedName("route_child")
    val routeChild: String,
    @SerializedName("chatroom_last_conversation_user_timestamp")
    val chatroomLastConversationUserTimestamp: Long?,
    @SerializedName("chatroom_last_conversation_timestamp")
    val chatroomLastConversationTimestamp: Long?,
    @SerializedName("attachments")
    val attachments: List<AttachmentViewData>?,
    @SerializedName("sort_key")
    val sortKey: String?,
    @SerializedName("chatroom_creator")
    val chatroomCreator: MemberViewData?,
    @SerializedName("chatroom_last_conversation_creator")
    val chatroomLastConversationCreator: MemberViewData?,
) {
    class Builder {
        private var communityName: String = ""
        private var chatroomName: String = ""
        private var chatroomTitle: String = ""
        private var chatroomUserName: String = ""
        private var chatroomUserImage: String = ""
        private var chatroomId: String = ""
        private var communityImage: String = ""
        private var communityId: Int = 0
        private var route: String = ""
        private var chatroomUnreadConversationCount: Int = 0
        private var chatroomLastConversation: String? = null
        private var chatroomLastConversationId: String? = null
        private var chatroomLastConversationUserName: String? = null
        private var chatroomLastConversationUserImage: String? = null
        private var routeChild: String = ""
        private var chatroomLastConversationUserTimestamp: Long? = null
        private var chatroomLastConversationTimestamp: Long? = null
        private var attachments: List<AttachmentViewData>? = null
        private var sortKey: String? = null
        private var chatroomCreator: MemberViewData? = null
        private var chatroomLastConversationCreator: MemberViewData? = null

        fun communityName(communityName: String) = apply {
            this.communityName = communityName
        }

        fun chatroomName(chatroomName: String) = apply {
            this.chatroomName = chatroomName
        }

        fun chatroomTitle(chatroomTitle: String) = apply {
            this.chatroomTitle = chatroomTitle
        }

        fun chatroomUserName(chatroomUserName: String) = apply {
            this.chatroomUserName = chatroomUserName
        }

        fun chatroomUserImage(chatroomUserImage: String) = apply {
            this.chatroomUserImage = chatroomUserImage
        }

        fun chatroomId(chatroomId: String) = apply {
            this.chatroomId = chatroomId
        }

        fun communityImage(communityImage: String) = apply {
            this.communityImage = communityImage
        }

        fun communityId(communityId: Int) = apply {
            this.communityId = communityId
        }

        fun route(route: String) = apply {
            this.route = route
        }

        fun chatroomUnreadConversationCount(chatroomUnreadConversationCount: Int) = apply {
            this.chatroomUnreadConversationCount = chatroomUnreadConversationCount
        }

        fun chatroomLastConversation(chatroomLastConversation: String?) = apply {
            this.chatroomLastConversation = chatroomLastConversation
        }

        fun chatroomLastConversationId(chatroomLastConversationId: String?) = apply {
            this.chatroomLastConversationId = chatroomLastConversationId
        }

        fun chatroomLastConversationUserName(chatroomLastConversationUserName: String?) = apply {
            this.chatroomLastConversationUserName = chatroomLastConversationUserName
        }

        fun chatroomLastConversationUserImage(chatroomLastConversationUserImage: String?) = apply {
            this.chatroomLastConversationUserImage = chatroomLastConversationUserImage
        }

        fun routeChild(routeChild: String) = apply {
            this.routeChild = routeChild
        }

        fun chatroomLastConversationUserTimestamp(chatroomLastConversationUserTimestamp: Long?) =
            apply {
                this.chatroomLastConversationUserTimestamp = chatroomLastConversationUserTimestamp
            }

        fun chatroomLastConversationTimestamp(chatroomLastConversationTimestamp: Long?) = apply {
            this.chatroomLastConversationTimestamp = chatroomLastConversationTimestamp
        }

        fun attachments(attachments: List<AttachmentViewData>?) = apply {
            this.attachments = attachments
        }

        fun sortKey(sortKey: String?) = apply {
            this.sortKey = sortKey
        }

        fun chatroomCreator(chatroomCreator: MemberViewData?) = apply {
            this.chatroomCreator = chatroomCreator
        }

        fun chatroomLastConversationCreator(chatroomLastConversationCreator: MemberViewData?) =
            apply {
                this.chatroomLastConversationCreator = chatroomLastConversationCreator
            }

        fun build() = ChatroomNotificationViewData(
            communityName,
            chatroomName,
            chatroomTitle,
            chatroomUserName,
            chatroomUserImage,
            chatroomId,
            communityImage,
            communityId,
            route,
            chatroomUnreadConversationCount,
            chatroomLastConversation,
            chatroomLastConversationId,
            chatroomLastConversationUserName,
            chatroomLastConversationUserImage,
            routeChild,
            chatroomLastConversationUserTimestamp,
            chatroomLastConversationTimestamp,
            attachments,
            sortKey,
            chatroomCreator,
            chatroomLastConversationCreator
        )
    }

    fun toBuilder(): Builder {
        return Builder().communityName(communityName)
            .chatroomName(chatroomName)
            .chatroomTitle(chatroomTitle)
            .chatroomUserName(chatroomUserName)
            .chatroomUserImage(chatroomUserImage)
            .chatroomId(chatroomId)
            .communityImage(communityImage)
            .communityId(communityId)
            .route(route)
            .chatroomUnreadConversationCount(chatroomUnreadConversationCount)
            .chatroomLastConversation(chatroomLastConversation)
            .chatroomLastConversationId(chatroomLastConversationId)
            .chatroomLastConversationUserName(chatroomLastConversationUserName)
            .chatroomLastConversationUserImage(chatroomLastConversationUserImage)
            .routeChild(routeChild)
            .chatroomLastConversationUserTimestamp(chatroomLastConversationUserTimestamp)
            .chatroomLastConversationTimestamp(chatroomLastConversationTimestamp)
            .attachments(attachments)
            .sortKey(sortKey)
            .chatroomCreator(chatroomCreator)
            .chatroomLastConversationCreator(chatroomLastConversationCreator)
    }
}