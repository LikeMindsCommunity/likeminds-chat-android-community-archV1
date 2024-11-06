package com.likeminds.chatmm.chatroom.detail.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ChatroomDetailExtras private constructor(
    val chatroomId: String,
    val fromNotification: Boolean,
    val communityId: String?,
    val communityName: String?,
    val source: String?,
    val reportedConversationId: String?,
    val conversationId: String?,
    val openedFromLink: Boolean?,
    val isFromSearchMessage: Boolean?,
    val isFromSearchChatroom: Boolean?,
    val scrollToExtremeTopForHighlightingTitle: Boolean?,
    val sourceChatroomId: String?,
    val sourceCommunityId: String?,
    val loadFromTop: Boolean?,
    val loadingAfterSync: Boolean,
    val searchKey: String?,
    val sourceLinkOrRoute: String?,
    val cohortId: String?,
    val notificationId: Int?
) : Parcelable {
    class Builder {
        private var chatroomId: String = ""
        private var fromNotification: Boolean = false
        private var communityId: String? = null
        private var communityName: String? = null
        private var source: String? = null
        private var reportedConversationId: String? = null
        private var conversationId: String? = null
        private var openedFromLink: Boolean? = null
        private var isFromSearchMessage: Boolean? = null
        private var isFromSearchChatroom: Boolean? = null
        private var scrollToExtremeTopForHighlightingTitle: Boolean? = null
        private var sourceChatroomId: String? = null
        private var sourceCommunityId: String? = null
        private var loadFromTop: Boolean? = false
        private var loadingAfterSync: Boolean = false
        private var searchKey: String? = null
        private var sourceLinkOrRoute: String? = null
        private var cohortId: String? = null
        private var notificationId: Int? = null

        fun chatroomId(chatroomId: String) = apply { this.chatroomId = chatroomId }
        fun fromNotification(fromNotification: Boolean) =
            apply { this.fromNotification = fromNotification }

        fun communityId(communityId: String?) = apply { this.communityId = communityId }
        fun communityName(communityName: String?) = apply { this.communityName = communityName }

        fun source(source: String?) = apply { this.source = source }
        fun reportedConversationId(reportedConversationId: String?) =
            apply { this.reportedConversationId = reportedConversationId }

        fun conversationId(conversationId: String?) = apply { this.conversationId = conversationId }
        fun openedFromLink(openedFromLink: Boolean?) =
            apply { this.openedFromLink = openedFromLink }

        fun isFromSearchMessage(isFromSearchMessage: Boolean?) =
            apply { this.isFromSearchMessage = isFromSearchMessage }

        fun isFromSearchChatroom(isFromSearchChatroom: Boolean?) =
            apply { this.isFromSearchChatroom = isFromSearchChatroom }

        fun scrollToExtremeTopForHighlightingTitle(scrollToExtremeTopForHighlightingTitle: Boolean?) =
            apply {
                this.scrollToExtremeTopForHighlightingTitle = scrollToExtremeTopForHighlightingTitle
            }

        fun sourceChatroomId(sourceChatroomId: String?) =
            apply { this.sourceChatroomId = sourceChatroomId }

        fun sourceCommunityId(sourceCommunityId: String?) =
            apply { this.sourceCommunityId = sourceCommunityId }

        fun loadFromTop(loadFromTop: Boolean?) = apply { this.loadFromTop = loadFromTop }
        fun loadingAfterSync(loadingAfterSync: Boolean) =
            apply { this.loadingAfterSync = loadingAfterSync }

        fun searchKey(searchKey: String?) = apply { this.searchKey = searchKey }

        fun sourceLinkOrRoute(sourceLinkOrRoute: String?) =
            apply { this.sourceLinkOrRoute = sourceLinkOrRoute }

        fun cohortId(cohortId: String?) = apply { this.cohortId = cohortId }
        fun notificationId(notificationId: Int?) = apply { this.notificationId = notificationId }

        fun build() = ChatroomDetailExtras(
            chatroomId,
            fromNotification,
            communityId,
            communityName,
            source,
            reportedConversationId,
            conversationId,
            openedFromLink,
            isFromSearchMessage,
            isFromSearchChatroom,
            scrollToExtremeTopForHighlightingTitle,
            sourceChatroomId,
            sourceCommunityId,
            loadFromTop,
            loadingAfterSync,
            searchKey,
            sourceLinkOrRoute,
            cohortId,
            notificationId
        )
    }

    fun toBuilder(): Builder {
        return Builder().chatroomId(chatroomId)
            .fromNotification(fromNotification)
            .communityId(communityId)
            .communityName(communityName)
            .source(source)
            .reportedConversationId(reportedConversationId)
            .conversationId(conversationId)
            .openedFromLink(openedFromLink)
            .isFromSearchMessage(isFromSearchMessage)
            .isFromSearchChatroom(isFromSearchChatroom)
            .scrollToExtremeTopForHighlightingTitle(scrollToExtremeTopForHighlightingTitle)
            .sourceChatroomId(sourceChatroomId)
            .sourceCommunityId(sourceCommunityId)
            .loadFromTop(loadFromTop)
            .loadingAfterSync(loadingAfterSync)
            .searchKey(searchKey)
            .sourceLinkOrRoute(sourceLinkOrRoute)
            .cohortId(cohortId)
            .notificationId(notificationId)
    }
}