package com.likeminds.chatmm.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.facebook.common.util.UriUtil.HTTPS_SCHEME
import com.facebook.common.util.UriUtil.HTTP_SCHEME
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

object Route {
    private const val ROUTE_SCHEME = "route"
    const val ROUTE_CHATROOM = "collabcard"
    const val ROUTE_CHATROOM_DETAIL = "chatroom_detail"
    const val ROUTE_BROWSER = "browser"
    const val ROUTE_MAIL = "mail"
    const val ROUTE_MEMBER = "member"
    const val ROUTE_MEMBER_PROFILE = "member_profile"
    const val ROUTE_USER_PROFILE = "user_profile"
    const val ROUTE_COMMUNITY_FEED = "community_feed"
    const val ROUTE_POLL_CHATROOM = "poll_chatroom"
    const val ROUTE_SYNC = "sync"
    private const val ROUTE_DIRECT_MESSAGE = "direct_messages"

    private const val DEEP_LINK_CHATROOM = "collabcard"
    private const val DEEP_LINK_SCHEME = "likeminds"
    private const val DEEP_LINK_CHATROOM_DETAIL = "chatroom_detail"
    private const val DEEP_LINK_COMMUNITY_FEED = "community_feed"

    const val PARAM_CHATROOM_ID = "chatroom_id"
    const val PARAM_COMMUNITY_ID = "community_id"
    const val PARAM_COMMUNITY_NAME = "community_name"
    private const val PARAM_COHORT_ID = "cohort_id"

    fun getQueryParam(route: String?, param: String?): String? {
        if (route.isNullOrEmpty() || param.isNullOrEmpty()) {
            return null
        }
        return try {
            Uri.parse(route)?.getQueryParameter(param)
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            null
        }
    }

    fun getHost(route: String?): String? {
        if (route == null) {
            return null
        }
        return Uri.parse(route).host
    }

    fun handleDeepLink(context: Context, url: String?): Intent? {
        val data = Uri.parse(url).normalizeScheme() ?: return null
        if (data.pathSegments.isNullOrEmpty()) {
            return null
        }
        val firstPath = getRouteFromDeepLink(data) ?: return null
        return getRouteIntent(
            context,
            firstPath,
            0
        )
    }

    //create route string as per uri with check for the host (likeminds)
    private fun getRouteFromDeepLink(data: Uri?): String? {
        data?.host ?: return null
        val firstPathSegment = data.pathSegments.firstOrNull()
        when {
            (firstPathSegment == DEEP_LINK_CHATROOM) -> {
                return createChatroomRoute(data)
            }

            (firstPathSegment == DEEP_LINK_CHATROOM_DETAIL) -> {
                return createChatroomDetailRoute(data)
            }
        }
        return when {
            data.scheme == DEEP_LINK_SCHEME -> {
                when (firstPathSegment) {
                    DEEP_LINK_COMMUNITY_FEED -> {
                        createCommunityFeedRoute(data)
                    }

                    else -> null
                }
            }

            else -> {
                createWebsiteRoute(data)
            }
        }
    }

    // https://<domain>/chatroom/chatroom_id=<chatroom_id>
    private fun createChatroomDetailRoute(data: Uri): String? {
        val chatroomId = data.getQueryParameter(PARAM_CHATROOM_ID) ?: return null
        return Uri.Builder()
            .scheme(ROUTE_SCHEME)
            .authority(ROUTE_CHATROOM_DETAIL)
            .appendQueryParameter(PARAM_CHATROOM_ID, chatroomId)
            .build()
            .toString()
    }

    //https://www.likeminds.community/collabcard/<collabcard_id>
    private fun createChatroomRoute(data: Uri): String? {
        val chatroomId = data.lastPathSegment ?: return null
        val cohortId = data.getQueryParameter(PARAM_COHORT_ID)
        return Uri.Builder()
            .scheme("route")
            .authority(ROUTE_CHATROOM)
            .appendQueryParameter("collabcard_id", chatroomId)
            .appendQueryParameter(PARAM_COHORT_ID, cohortId)
            .build()
            .toString()
    }

    /**
     * Community website deep link
     * https://community.likeminds.community/deutsch-in-tandem
     * @return a url with host as route, eg : route://browser
     */
    fun createWebsiteRoute(data: Uri): String? {
        if (data.scheme == HTTPS_SCHEME || data.scheme == HTTP_SCHEME) {
            return Uri.Builder()
                .scheme(ROUTE_SCHEME)
                .authority(ROUTE_BROWSER)
                .appendQueryParameter("link", data.toString())
                .build()
                .toString()
        }
        return null
    }

    //https://www.likeminds.community/community_collabcard?community_id=&community_name=
    private fun createCommunityFeedRoute(data: Uri): String {
        return Uri.Builder()
            .scheme("route")
            .authority(ROUTE_COMMUNITY_FEED)
            .appendQueryParameter("community_id", data.getQueryParameter("community_id"))
            .appendQueryParameter("community_name", data.getQueryParameter("community_name"))
            .build()
            .toString()
    }

    // todo: removed profile routes
    fun getRouteIntent(
        context: Context,
        routeString: String,
        flags: Int = 0,
        source: String? = null,
        map: HashMap<String, String>? = null,
        deepLinkUrl: String? = null,
        notificationId: Int? = null // required when you need to cancel the notification via action when activity is opened
    ): Intent? {
        val route = Uri.parse(routeString)
        var intent: Intent? = null
        when (route.host) {
            ROUTE_CHATROOM, ROUTE_POLL_CHATROOM -> {
                intent = getRouteToChatroom(
                    context,
                    getChatroomRouteWithExtraParameters(route, map),
                    source,
                    deepLinkUrl,
                    notificationId = notificationId
                )
            }

            ROUTE_BROWSER -> {
                intent = getRouteToBrowser(route)
            }

            ROUTE_CHATROOM_DETAIL -> {
                intent = getRouteToChatroomDetail(
                    context,
                    route,
                    source,
                    deepLinkUrl
                )
            }

            ROUTE_MAIL -> {
                intent = getRouteToMail(route)
            }

            ROUTE_DIRECT_MESSAGE -> {
                intent = getRouteToDirectMessage(context, route)
            }
        }
        if (intent != null) {
            intent.flags = flags
        }
        return intent
    }

    //route://collabcard?collabcard_id=
    private fun getRouteToChatroom(
        context: Context,
        route: Uri,
        source: String?,
        deepLinkUrl: String?,
        notificationId: Int? = null
    ): Intent {
        val chatroomId = route.getQueryParameter("collabcard_id")
        val sourceChatroomId = route.getQueryParameter(PARAM_CHATROOM_ID)
        val sourceCommunityId = route.getQueryParameter(PARAM_COMMUNITY_ID)
        val communityName = route.getQueryParameter(PARAM_COMMUNITY_NAME)
        val cohortId = route.getQueryParameter(PARAM_COHORT_ID)

        val builder = ChatroomDetailExtras.Builder()
            .chatroomId(chatroomId ?: sourceChatroomId.toString())
            .source(source)
            .sourceChatroomId(sourceChatroomId)
            .sourceCommunityId(sourceCommunityId)
            .communityName(communityName)
            .cohortId(cohortId)
            .notificationId(notificationId)

        when (source) {
            LMAnalytics.Source.NOTIFICATION -> {
                builder.fromNotification(true).sourceLinkOrRoute(route.toString())
            }

            LMAnalytics.Source.DEEP_LINK -> {
                builder.openedFromLink(true).sourceLinkOrRoute(deepLinkUrl)
            }
        }

        return ChatroomDetailActivity.getIntent(
            context,
            builder.build()
        )
    }

    /**
     * * This function is used to add extra parameters in route
     */
    private fun getChatroomRouteWithExtraParameters(
        route: Uri,
        extraParameters: HashMap<String, String>?,
    ): Uri {
        return if (extraParameters != null) {
            appendQueryParametersToRoute(route, extraParameters)
        } else {
            route
        }
    }

    private fun appendQueryParametersToRoute(uri: Uri, map: HashMap<String, String>): Uri {
        val query = uri.queryParameterNames
        val newUri = Uri.Builder()
            .scheme(uri.scheme)
            .authority(uri.authority)
            .path(uri.path)
        for (item in map) {
            newUri.appendQueryParameter(item.key, item.value)
        }
        for (item in query) {
            newUri.appendQueryParameter(item, uri.getQueryParameter(item))
        }
        return newUri.build()
    }

    private fun getRouteToBrowser(route: Uri): Intent {
        return Intent(Intent.ACTION_VIEW, Uri.parse(route.getQueryParameter("link")))
    }

    //route://chatroom_detail?chatroom_id=
    private fun getRouteToChatroomDetail(
        context: Context,
        route: Uri,
        source: String?,
        deepLinkUrl: String?
    ): Intent {
        val chatroomId = route.getQueryParameter("chatroom_id")
        val conversationId = route.getQueryParameter("conversation_id")

        val builder = ChatroomDetailExtras.Builder()
            .chatroomId(chatroomId.toString())
            .conversationId(conversationId)
            .source(source)

        when (source) {
            LMAnalytics.Source.NOTIFICATION -> {
                builder.fromNotification(true).sourceLinkOrRoute(route.toString())
            }

            LMAnalytics.Source.DEEP_LINK -> {
                builder.openedFromLink(true).sourceLinkOrRoute(deepLinkUrl)
            }
        }

        return ChatroomDetailActivity.getIntent(
            context,
            builder.build()
        )
    }

    //route://mail?to=<email>
    private fun getRouteToMail(route: Uri): Intent? {
        val sendTo = route.getQueryParameter("to")
        if (AuthValidator.isValidEmail(sendTo)) {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:$sendTo")
            return Intent.createChooser(intent, "Select an email client")
        }
        return null
    }

    fun createRouteForMemberProfile(member: MemberViewData?, communityId: String?): String {
        return "<<${member?.name}|route://user_profile/${member?.sdkClientInfo?.uuid}?community_id=${communityId}>>"
    }

    fun Uri.getNullableQueryParameter(key: String): String? {
        val value = this.getQueryParameter(key)
        return if (value == "null") {
            null
        } else {
            value
        }
    }

    private fun getRouteToDirectMessage(context: Context, route: Uri): Intent? {
        val chatroomId = route.getQueryParameter("chatroom_id") ?: return null
        val communityId = route.getQueryParameter("community_id")
        val communityName = route.getQueryParameter(PARAM_COMMUNITY_NAME)
        return ChatroomDetailActivity.getIntent(
            context,
            ChatroomDetailExtras.Builder()
                .chatroomId(chatroomId)
                .communityId(communityId)
                .communityName(communityName)
                .build()
        )
    }

    //route://poll_chatroom?chatroom_id=<>&poll_end=<true/false>
    fun getPollRouteQueryParameters(route: String): Pair<String?, Boolean> {
        val routeUri = Uri.parse(route)
        val chatroomId = routeUri.getQueryParameter("chatroom_id")
        val pollEnd = routeUri.getBooleanQueryParameter("poll_end", false)
        return Pair(chatroomId, pollEnd)
    }

    // create route for direct messages
    // route://direct_messages?chatroom_id=<>&community_id=<>
    fun createDirectMessageRoute(chatroomId: String): String {
        return Uri.Builder()
            .scheme(ROUTE_SCHEME)
            .authority(ROUTE_DIRECT_MESSAGE)
            .appendQueryParameter("chatroom_id", chatroomId)
            .build()
            .toString()
    }
}