package com.likeminds.chatmm.pushnotification.util

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.Keep
import androidx.core.app.*
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.google.gson.Gson
import com.likeminds.chatmm.*
import com.likeminds.chatmm.SDKApplication.Companion.LOG_TAG
import com.likeminds.chatmm.chatroom.detail.util.ChatroomUtil
import com.likeminds.chatmm.di.DaggerLikeMindsChatComponent
import com.likeminds.chatmm.di.LikeMindsChatComponent
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.media.util.MediaUtils
import com.likeminds.chatmm.pushnotification.model.*
import com.likeminds.chatmm.pushnotification.viewmodel.LMNotificationViewModel
import com.likeminds.chatmm.theme.model.LMTheme
import com.likeminds.chatmm.utils.Route
import com.likeminds.chatmm.utils.ValueUtils
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import org.json.JSONObject
import javax.inject.Inject

@Keep
class LMChatNotificationHandler {

    @Inject
    lateinit var lmNotificationViewModel: LMNotificationViewModel

    private val gson by lazy {
        Gson()
    }

    private var appComponent: LikeMindsChatComponent? = null

    private lateinit var mApplication: Application

    //icon of notification
    private var notificationIcon: Int = 0

    //color of notification text
    private var notificationTextColor: Int = 0

    companion object {
        private var notificationHandler: LMChatNotificationHandler? = null

        private var launcherIntent: Intent? = null

        const val GENERAL_CHANNEL_ID = "notification_general"
        const val CHATROOM_CHANNEL_ID = "chatroom_channel_id"
        const val NOTIFICATION_TITLE = "title"
        const val NOTIFICATION_SUB_TITLE = "sub_title"
        const val NOTIFICATION_ROUTE = "route"
        const val NOTIFICATION_UNREAD_NEW_CHATROOM = "unread_new_chatroom"
        const val NOTIFICATION_CATEGORY = "category"
        const val NOTIFICATION_SUBCATEGORY = "subcategory"
        const val NOTIFICATION_UNREAD_CONVERSATION_GROUP_ID = 101

        private const val NOTIFICATION_DATA = "notification_data"

        @JvmStatic
        fun getInstance(): LMChatNotificationHandler {
            if (notificationHandler == null) {
                notificationHandler = LMChatNotificationHandler()
            }
            return notificationHandler!!
        }

        fun getVoteAction(
            context: Context,
            notificationActionData: NotificationActionData,
        ): NotificationCompat.Action {
            val voteIntent =
                Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
                    putExtra(
                        NotificationActionBroadcastReceiver.BUNDLE_NEW_POLL_VOTE_CHAT_ROOM,
                        notificationActionData
                    )
                    action = NotificationActionBroadcastReceiver.ACTION_NEW_CHATROOM_VOTE
                }
            val votePendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    System.currentTimeMillis().toInt(),
                    voteIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

            return NotificationCompat.Action.Builder(
                R.drawable.lm_chat_ic_vote, "Vote", votePendingIntent
            ).build()
        }

        fun getFollowAction(
            context: Context,
            notificationActionData: NotificationActionData,
        ): NotificationCompat.Action {
            val followIntent =
                Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
                    putExtra(
                        NotificationActionBroadcastReceiver.BUNDLE_NEW_FOLLOW_CHAT_ROOM,
                        notificationActionData
                    )
                    action = NotificationActionBroadcastReceiver.ACTION_NEW_CHATROOM_FOLLOW
                }
            val followPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    System.currentTimeMillis().toInt(),
                    followIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

            return NotificationCompat.Action.Builder(
                R.drawable.lm_chat_ic_follow_notification, "Follow", followPendingIntent
            ).build()
        }

        fun getMarkAsReadAction(
            context: Context,
            gson: Gson,
            payload: NotificationExtras,
        ): NotificationCompat.Action {
            val text = gson.toJson(payload)
            val markAsReadIntent =
                Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
                    putExtra(
                        NotificationActionBroadcastReceiver.BUNDLE_MARK_AS_READ_CHAT_ROOM,
                        text
                    )
                    action = NotificationActionBroadcastReceiver.ACTION_CHATROOM_MARK_AS_READ
                }
            val markAsReadPendingIntent: PendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    System.currentTimeMillis().toInt(),
                    markAsReadIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

            return NotificationCompat.Action.Builder(
                R.drawable.lm_chat_ic_mark_as_read,
                context.getString(R.string.lm_chat_mark_as_read),
                markAsReadPendingIntent
            ).build()
        }

        fun getReplyAction(
            context: Context,
            isNewChatroom: Boolean,
            gson: Gson,
            extras: NotificationExtras,
        ): NotificationCompat.Action {
            val remoteInput =
                RemoteInput.Builder(NotificationActionBroadcastReceiver.KEY_REPLY).run {
                    setLabel(context.getString(R.string.lm_chat_reply))
                    build()
                }

            val replyIntent =
                Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
                    action = if (isNewChatroom) {
                        putExtra(
                            NotificationActionBroadcastReceiver.BUNDLE_NEW_REPLY_CHAT_ROOM,
                            gson.toJson(extras)
                        )
                        NotificationActionBroadcastReceiver.ACTION_NEW_CHATROOM_REPLY
                    } else {
                        putExtra(
                            NotificationActionBroadcastReceiver.BUNDLE_REPLY_CHAT_ROOM,
                            gson.toJson(extras)
                        )
                        NotificationActionBroadcastReceiver.ACTION_CHATROOM_REPLY
                    }
                }

            val replyPendingIntent =
                PendingIntent.getBroadcast(
                    context,
                    extras.chatroomId.toInt(),
                    replyIntent,
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

            return NotificationCompat.Action.Builder(
                R.drawable.lm_chat_ic_reply,
                context.getString(R.string.lm_chat_reply),
                replyPendingIntent
            ).addRemoteInput(remoteInput).build()
        }

        //create pending intent as per route in notification
        fun getRoutePendingIntent(
            context: Context,
            notificationId: Int,
            route: String,
            notificationTitle: String,
            notificationMessage: String,
            category: String?,
            subcategory: String?,
        ): PendingIntent? {
            //get intent for route
            val intent = Route.getRouteIntent(
                context,
                route,
                0,
                LMAnalytics.Source.NOTIFICATION
            )

            if (intent?.getBundleExtra("bundle") != null) {
                intent.getBundleExtra("bundle")!!.putParcelable(
                    NOTIFICATION_DATA,
                    NotificationActionData.Builder()
                        .groupRoute(route)
                        .childRoute(route)
                        .notificationTitle(notificationTitle)
                        .notificationMessage(notificationMessage)
                        .category(category)
                        .subcategory(subcategory)
                        .build()
                )
            } else {
                intent?.putExtra(
                    NOTIFICATION_DATA, NotificationActionData.Builder()
                        .groupRoute(route)
                        .childRoute(route)
                        .notificationTitle(notificationTitle)
                        .notificationMessage(notificationMessage)
                        .category(category)
                        .subcategory(subcategory)
                        .build()
                )
            }

            var resultPendingIntent: PendingIntent? = null

            if (intent != null) {
                resultPendingIntent = PendingIntent.getActivities(
                    context,
                    notificationId,
                    arrayOf(launcherIntent, intent),
                    PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
            return resultPendingIntent
        }
    }

    //create the instance of the handler and channel for notification
    fun create(application: Application, launcherIntent: Intent) {
        mApplication = application
        createRockyComponent(application)

        appComponent!!.inject(this)

        notificationIcon = R.drawable.lm_chat_ic_notification

        notificationTextColor = LMTheme.getButtonsColor()

        LMChatNotificationHandler.launcherIntent = launcherIntent

        createNotificationChannel()
    }

    private fun createRockyComponent(application: Application) {
        if (appComponent == null) {
            appComponent = DaggerLikeMindsChatComponent.builder()
                .application(application)
                .build()
        }
    }

    private fun getCommunityId(route: String?): String? {
        return Route.getQueryParam(route, "community_id")
    }

    private fun getChatroomId(route: String?): String? {
        return Route.getQueryParam(route, "collabcard_id")
            ?: Route.getQueryParam(route, "chatroom_id")
    }

    //handle and show notification
    fun handleNotification(data: MutableMap<String, String>) {
        Log.d(
            LOG_TAG,
            "notification received with timestamp:${System.currentTimeMillis()} data: $data"
        )

        val title = data[NOTIFICATION_TITLE] ?: return
        val subTitle = data[NOTIFICATION_SUB_TITLE] ?: return
        val route = data[NOTIFICATION_ROUTE] ?: return
        val routeHost = Route.getHost(route)
        val category = data[NOTIFICATION_CATEGORY]
        val subcategory = data[NOTIFICATION_SUBCATEGORY]
        val unreadNewChatroom = data[NOTIFICATION_UNREAD_NEW_CHATROOM]

        //validate data
        if (category.isNullOrEmpty() && subcategory.isNullOrEmpty()) {
            return
        }

        //create payload for analytics event
        val payloadJson = JSONObject().apply {
            put(NOTIFICATION_TITLE, title)
            put(NOTIFICATION_SUB_TITLE, subTitle)
            put(NOTIFICATION_ROUTE, route)
            put(NOTIFICATION_UNREAD_NEW_CHATROOM, unreadNewChatroom)
        }

        // Send notification received event other than sync notifications
        if (routeHost != Route.ROUTE_SYNC) {
            LMAnalytics.track(
                LMAnalytics.Events.NOTIFICATION_RECEIVED,
                mapOf(
                    "payload" to payloadJson.toString(),
                    NOTIFICATION_CATEGORY to category,
                    NOTIFICATION_SUBCATEGORY to subcategory
                )
            )
        }

        when {
            //when new chatroom is created
            unreadNewChatroom != null -> {
                Log.d(
                    LOG_TAG,
                    "chatroom notification for new chatroom, timestamp:${System.currentTimeMillis()}"
                )
                val unreadNewChatroomData = gson.fromJson(
                    unreadNewChatroom,
                    ChatroomNotificationViewData::class.java
                )
                if (unreadNewChatroomData != null) {
                    initChatroomGroupNotification(
                        mApplication, unreadNewChatroomData,
                        title,
                        subTitle,
                        category,
                        subcategory
                    )
                }
            }
            //chatroom notification -> for messages only
            Route.ROUTE_CHATROOM == routeHost -> {
                Log.d(
                    LOG_TAG,
                    "chatroom notification for stack, timestamp:${System.currentTimeMillis()}"
                )

                getCommunityId(route)?.let { _ ->
                    val chatroomIdReceivedFromRoute = getChatroomId(route)
                    val chatroomIdOpened = SDKApplication.getInstance().openedChatroomId
                    if (chatroomIdOpened != chatroomIdReceivedFromRoute) {
                        lmNotificationViewModel.fetchUnreadConversations {
                            Log.d(
                                LOG_TAG, """
                            conversations received from viewmodel with timestamp:${System.currentTimeMillis()}
                        """.trimIndent()
                            )
                            if (it != null) {
                                val conversations = it.filter { notificationData ->
                                    !notificationData.chatroomLastConversationUserName.isNullOrEmpty()
                                }
                                initConversationsGroupNotification(
                                    mApplication,
                                    conversations,
                                    title,
                                    subTitle,
                                    category,
                                    subcategory
                                )
                            }
                        }
                    }
                }
            }

            title.isNotBlank() && subTitle.isNotBlank() && route.isNotBlank() -> {
                when (routeHost) {
                    //for poll chatroom
                    Route.ROUTE_POLL_CHATROOM -> {
                        Log.d(
                            LOG_TAG,
                            "chatroom notification for poll chatroom, timestamp:${System.currentTimeMillis()}"
                        )
                        sendNewPollChatRoomSingleNotification(
                            mApplication,
                            title,
                            subTitle,
                            route,
                            category,
                            subcategory
                        )
                    }
                    //for other cases
                    else -> {
                        val chatroomIdReceivedFromRoute = getChatroomId(route)
                        val chatroomIdOpened = SDKApplication.getInstance().openedChatroomId
                        if (chatroomIdOpened != chatroomIdReceivedFromRoute) {
                            sendNormalNotification(
                                mApplication,
                                title,
                                subTitle,
                                route,
                                category,
                                subcategory
                            )
                        }
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(
                LOG_TAG, """
                channel created with timestamp:${System.currentTimeMillis()}
            """.trimIndent()
            )
            createGeneralNotificationChannel()
            createChatroomNotificationChannel()
        }
    }

    private fun createGeneralNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = mApplication.getString(R.string.lm_chat_general_channel_name)
            val descriptionText =
                mApplication.getString(R.string.lm_chat_general_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(GENERAL_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                mApplication.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createChatroomNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = mApplication.getString(R.string.lm_chat_chatroom_channel_name)
            val descriptionText =
                mApplication.getString(R.string.lm_chat_chatroom_channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHATROOM_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                mApplication.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * create pending intent and show notifications accordingly
     * */
    private fun sendNormalNotification(
        context: Context,
        title: String,
        subTitle: String,
        route: String,
        category: String?,
        subcategory: String?,
    ) {
        Log.d(
            LOG_TAG,
            "chatroom notification for normal, timestamp:${System.currentTimeMillis()}"
        )
        // notificationId is a unique int for each notification that you must define
        val notificationId = route.hashCode()
        val resultPendingIntent: PendingIntent? =
            getRoutePendingIntent(
                context,
                notificationId,
                route,
                title,
                subTitle,
                category,
                subcategory
            )
        val notificationBuilder = NotificationCompat.Builder(mApplication, GENERAL_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subTitle)
            .setSmallIcon(notificationIcon)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subTitle))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (resultPendingIntent != null) {
            notificationBuilder.setContentIntent(resultPendingIntent)
        }
        with(NotificationManagerCompat.from(mApplication)) {
            Log.d(
                LOG_TAG,
                "displayed normal chatroom notification, timestamp:${System.currentTimeMillis()}"
            )
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun initChatroomGroupNotification(
        context: Context,
        chatroom: ChatroomNotificationViewData,
        title: String,
        subTitle: String,
        category: String?,
        subcategory: String?,
    ) {
        createNotificationChannel()
        MediaUtils.fetchImage(
            context, chatroom.communityImage, true
        ) { communityImageBitmap ->
            val images = chatroom.attachments?.filter { it.type == IMAGE || it.type == GIF }
            if (!images.isNullOrEmpty()) {
                MediaUtils.fetchImage(
                    context, images[0].url
                ) { chatImageBitmap ->
                    sendChatroomGroupNotification(
                        context, chatroom, communityImageBitmap,
                        chatImageBitmap, title, subTitle, category, subcategory
                    )
                }
            } else {
                sendChatroomGroupNotification(
                    context,
                    chatroom,
                    communityImageBitmap,
                    title = title,
                    subTitle = subTitle,
                    category = category,
                    subcategory = subcategory
                )
            }
        }
    }

    private fun sendNewPollChatRoomSingleNotification(
        context: Context,
        title: String,
        subTitle: String,
        route: String,
        category: String?,
        subcategory: String?,
    ) {
        createNotificationChannel()
        // notificationId is a unique int for each notification that you must define
        val notificationId = route.hashCode()
        val routeData = Route.getPollRouteQueryParameters(route)
        val resultPendingIntent: PendingIntent? =
            getRoutePendingIntent(
                context,
                notificationId,
                route,
                title,
                subTitle,
                category,
                subcategory
            )
        val notificationBuilder = NotificationCompat.Builder(mApplication, CHATROOM_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subTitle)
            .setSmallIcon(notificationIcon)
            .setAutoCancel(true)
            .setColor(notificationTextColor)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subTitle))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        if (!routeData.second) {
            notificationBuilder
                .addAction(
                    getVoteAction(
                        context, NotificationActionData.Builder()
                            .groupRoute(route)
                            .childRoute(route)
                            .notificationTitle(title)
                            .notificationMessage(subTitle)
                            .category(category)
                            .subcategory(subcategory)
                            .build()
                    )
                )
                .addAction(
                    getFollowAction(
                        mApplication,
                        NotificationActionData.Builder()
                            .groupRoute(route)
                            .childRoute(route)
                            .chatroomId(routeData.first)
                            .notificationTitle(title)
                            .notificationMessage(subTitle)
                            .category(category)
                            .subcategory(subcategory)
                            .build()
                    )
                )
        }

        if (resultPendingIntent != null) {
            notificationBuilder.setContentIntent(resultPendingIntent)
        }
        with(NotificationManagerCompat.from(mApplication)) {
            notify(notificationId, notificationBuilder.build())
        }
    }

    private fun sendChatroomGroupNotification(
        context: Context,
        chatroom: ChatroomNotificationViewData,
        communityImageBitmap: Bitmap?,
        chatImageBitmap: Bitmap? = null,
        title: String,
        subTitle: String,
        category: String?,
        subcategory: String?,
    ) {
        val contentText = MemberTaggingDecoder.decode(chatroom.chatroomTitle)
        val childRoute = chatroom.routeChild
        val chatroomId = chatroom.chatroomId
        val communityId = chatroom.communityId
        val groupRoute = chatroom.route
        val communityName = chatroom.communityName
        val chatroomName = chatroom.chatroomName

        //Child notification
        val childPendingIntent: PendingIntent? = getRoutePendingIntent(
            context,
            chatroomId.toInt(),
            childRoute,
            title,
            subTitle,
            category,
            subcategory
        )
        val lockScreenSingleNotification =
            NotificationCompat.Builder(mApplication, GENERAL_CHANNEL_ID)
                .setSmallIcon(notificationIcon)
                .setColor(notificationTextColor)
                .setContentTitle(mApplication.getString(R.string.lm_chat_app_name))
                .setContentText(chatroomName)
        val chatroomPerson = Person.Builder()
            .setKey(chatroomId.toString())
            .setName(chatroom.chatroomUserName)
            .build()

        val messages = getMessages(
            context,
            contentText,
            chatroom,
            System.currentTimeMillis(),
            chatroomPerson,
            chatImageBitmap = chatImageBitmap
        )

        var chatroomMessagingStyle =
            NotificationCompat.MessagingStyle(chatroomPerson)
                .setConversationTitle(chatroom.chatroomName)
                .setGroupConversation(true)

        messages.forEach { message ->
            chatroomMessagingStyle = chatroomMessagingStyle.addMessage(message)
        }

        val singleNotification =
            NotificationCompat.Builder(mApplication, GENERAL_CHANNEL_ID)
                .setLargeIcon(communityImageBitmap)
                .setSmallIcon(notificationIcon)
                .setGroup(groupRoute)
                .addAction(
                    getFollowAction(
                        mApplication, NotificationActionData.Builder()
                            .chatroomId(chatroomId)
                            .communityId(communityId)
                            .groupRoute(groupRoute)
                            .childRoute(childRoute)
                            .notificationTitle(title)
                            .notificationMessage(subTitle)
                            .category(category)
                            .subcategory(subcategory)
                            .build()
                    )
                )
                .addAction(
                    getReplyAction(
                        mApplication,
                        true,
                        gson,
                        NotificationExtras.Builder()
                            .chatroomId(chatroomId)
                            .title(chatroomName)
                            .route(groupRoute)
                            .childRoute(childRoute)
                            .notificationTitle(title)
                            .notificationMessage(subTitle)
                            .extraCategory(category)
                            .extraSubcategory(subcategory)
                            .build()
                    )
                )
                .setColor(notificationTextColor)
                .setStyle(chatroomMessagingStyle).setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(lockScreenSingleNotification.build())

        if (childPendingIntent != null) {
            singleNotification.setContentIntent(childPendingIntent)
        }
        //Group notification
        val groupPendingIntent: PendingIntent? = getRoutePendingIntent(
            context,
            communityId,
            groupRoute,
            title,
            subTitle,
            category,
            subcategory
        )
        val lockScreenGroupNotification =
            NotificationCompat.Builder(mApplication, GENERAL_CHANNEL_ID)
                .setSmallIcon(notificationIcon)
                .setContentTitle(mApplication.getString(R.string.lm_chat_app_name))
                .setColor(notificationTextColor)
                .setContentText("$communityName (New chatrooms)")
        val groupNotification =
            NotificationCompat.Builder(mApplication, CHATROOM_CHANNEL_ID)
                .setSmallIcon(notificationIcon)
                .setSubText(communityName)
                .setGroup(groupRoute)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setColor(notificationTextColor)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(lockScreenGroupNotification.build())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        if (groupPendingIntent != null) {
            groupNotification.setContentIntent(groupPendingIntent)
        }
        //Notify user
        with(NotificationManagerCompat.from(mApplication)) {
            notify(groupRoute, chatroomId.toInt(), singleNotification.build())
            notify(groupRoute, communityId, groupNotification.build())
        }
    }

    private fun initConversationsGroupNotification(
        context: Context,
        unreadConversations: List<ChatroomNotificationViewData>,
        title: String,
        subTitle: String,
        category: String?,
        subcategory: String?,
    ) {
        createNotificationChannel()
        var sortedUnreadConversations =
            unreadConversations.sortedByDescending { it.chatroomLastConversationUserTimestamp }

        val list = ValueUtils.generateLexicoGraphicalList(sortedUnreadConversations.size)
        // Add the numeric lex number as sorting key
        sortedUnreadConversations = sortedUnreadConversations.mapIndexed { index, it ->
            it.toBuilder().sortKey(list[index]).build()
        }

        //Group conversations by route
        sortedUnreadConversations.groupBy { notificationData ->
            notificationData.route
        }.forEach { (route, conversations) ->
            sendConversationsGroupNotification(
                context,
                conversations,
                route,
                title,
                subTitle,
                category,
                subcategory
            )
        }
    }

    /**
     * Shows the notifications in a background thread using coroutines
     */
    private fun sendConversationsGroupNotification(
        context: Context,
        sortedUnreadConversations: List<ChatroomNotificationViewData>,
        groupRoute: String,
        title: String,
        subTitle: String,
        category: String?,
        subcategory: String?,
    ) {
        if (sortedUnreadConversations.isEmpty()) {
            return
        }
        val notificationManagerCompat = NotificationManagerCompat.from(context)
        //Start loop of unread notification
        sortedUnreadConversations.forEachIndexed { index, unreadConversation ->
            //Fetch community image
            MediaUtils.fetchImage(
                context,
                unreadConversation.communityImage,
                true
            ) { communityImageBitmap ->
                //Fetch attachment first image
                fetchUnreadConversationsFirstImage(
                    context,
                    unreadConversation
                ) { unreadConversationFirstImageUri ->
                    val chatroomId = unreadConversation.chatroomId
                    val chatroomName = unreadConversation.chatroomName
                    val childRoute = unreadConversation.routeChild
                    //To show notification time
                    val contentText = MemberTaggingDecoder.decode(
                        unreadConversation.chatroomLastConversation
                    )
                    var notificationTime =
                        unreadConversation.chatroomLastConversationUserTimestamp
                    if (notificationTime != null) {
                        notificationTime *= 1000
                    } else {
                        notificationTime = System.currentTimeMillis()
                    }
                    //Individual notification in message style
                    val unreadConversationPerson = Person.Builder()
                        .setKey(chatroomId.toString())
                        .setName(unreadConversation.chatroomLastConversationUserName)
                        .build()
                    val messages = getMessages(
                        context,
                        contentText,
                        unreadConversation,
                        notificationTime,
                        unreadConversationPerson,
                        chatImageUri = unreadConversationFirstImageUri
                    )
                    var unreadConversationMessagingStyle =
                        NotificationCompat.MessagingStyle(unreadConversationPerson)
                            .setConversationTitle(chatroomName)
                            .setGroupConversation(true)
                    messages.forEach { message ->
                        unreadConversationMessagingStyle =
                            unreadConversationMessagingStyle.addMessage(message)
                    }
                    //Build notification
                    val notificationBuilder =
                        NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
                            .setSortKey(unreadConversation.sortKey)
                            .setSmallIcon(notificationIcon)
                            .setGroup(groupRoute)
                            .setColor(notificationTextColor)
                            .setStyle(unreadConversationMessagingStyle)
                            .setLargeIcon(communityImageBitmap)
                            .setWhen(notificationTime)
                            .addAction(
                                getReplyAction(
                                    context,
                                    false,
                                    gson,
                                    NotificationExtras.Builder()
                                        .chatroomId(chatroomId)
                                        .title(chatroomName)
                                        .route(groupRoute)
                                        .childRoute(childRoute)
                                        .notificationTitle(title)
                                        .notificationMessage(subTitle)
                                        .build()
                                )
                            )
                            .addAction(
                                getMarkAsReadAction(
                                    context,
                                    gson,
                                    NotificationExtras.Builder()
                                        .chatroomId(chatroomId)
                                        .route(groupRoute)
                                        .childRoute(childRoute)
                                        .notificationTitle(title)
                                        .notificationMessage(subTitle)
                                        .build()
                                )
                            )
                            .setShowWhen(true)
                            .setAutoCancel(true)
                            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    //Click on individual notification in the group
                    val childPendingIntent: PendingIntent? =
                        getRoutePendingIntent(
                            context,
                            chatroomId.toInt(),
                            childRoute,
                            title,
                            subTitle,
                            category,
                            subcategory
                        )
                    if (childPendingIntent != null) {
                        notificationBuilder.setContentIntent(childPendingIntent)
                    }
                    //Notify individual notification
                    with(notificationManagerCompat) {
                        notify(groupRoute, chatroomId.toInt(), notificationBuilder.build())
                    }
                    //Once all notifications are notified, post summary
                    if (sortedUnreadConversations.size - 1 == index) {
                        showUnreadConversationGroupNotification(
                            context,
                            sortedUnreadConversations,
                            groupRoute,
                            title,
                            subTitle,
                            notificationManagerCompat,
                            category,
                            subcategory
                        )
                    }
                }
            }
        }
    }

    private fun showUnreadConversationGroupNotification(
        context: Context,
        sortedUnreadConversations: List<ChatroomNotificationViewData>,
        groupRoute: String,
        title: String,
        subTitle: String,
        notificationManagerCompat: NotificationManagerCompat,
        category: String?,
        subcategory: String?,
    ) {
        val totalUnreadConversations = sortedUnreadConversations.sumOf {
            it.chatroomUnreadConversationCount
        }
        val unreadConversationsSize = sortedUnreadConversations.size
        val groupSummaryText = if (unreadConversationsSize == 1) {
            "1 new message"
        } else {
            "$totalUnreadConversations messages from $unreadConversationsSize chatrooms"
        }
        //To display on locked screen
        val lockScreenGroupNotification =
            NotificationCompat.Builder(context, GENERAL_CHANNEL_ID)
                .setSmallIcon(notificationIcon)
                .setContentTitle(mApplication.getString(R.string.lm_chat_app_name))
                .setColor(notificationTextColor)
                .setContentText(groupSummaryText)
        //Group summary
        val groupNotification =
            NotificationCompat.Builder(context, CHATROOM_CHANNEL_ID)
                .setSmallIcon(notificationIcon)
                .setSubText(groupSummaryText)
                .setContentTitle(context.getString(R.string.lm_chat_app_name))
                .setContentText(groupSummaryText)
                .setGroup(groupRoute)
                .setGroupSummary(true)
                .setColor(notificationTextColor)
                .setAutoCancel(true)
                .setPublicVersion(lockScreenGroupNotification.build())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        //Click of group notification
        val groupPendingIntent: PendingIntent? = getRoutePendingIntent(
            context,
            NOTIFICATION_UNREAD_CONVERSATION_GROUP_ID,
            groupRoute,
            title,
            subTitle,
            category,
            subcategory
        )
        if (groupPendingIntent != null) {
            groupNotification.setContentIntent(groupPendingIntent)
        }
        //Notify group notification
        with(notificationManagerCompat) {
            Log.d(LOG_TAG, "group notification displayed timestamp :${System.currentTimeMillis()}")
            notify(
                groupRoute,
                NOTIFICATION_UNREAD_CONVERSATION_GROUP_ID,
                groupNotification.build()
            )
        }
    }

    private fun getMessages(
        context: Context,
        contentText: String,
        unreadConversation: ChatroomNotificationViewData,
        notificationTime: Long,
        unreadConversationPerson: Person,
        chatImageBitmap: Bitmap? = null,
        chatImageUri: Uri? = null,
    ): ArrayList<NotificationCompat.MessagingStyle.Message> {
        val messagesList = ArrayList<NotificationCompat.MessagingStyle.Message>()
        var mediaCountMessage: NotificationCompat.MessagingStyle.Message? = null
        var imageMessage: NotificationCompat.MessagingStyle.Message? = null

        val imagesCount = unreadConversation.attachments?.filter { it.type == IMAGE }?.size ?: 0
        val gifsCount = unreadConversation.attachments?.filter { it.type == GIF }?.size ?: 0
        val pdfCount = unreadConversation.attachments?.filter { it.type == PDF }?.size ?: 0
        val videoCount = unreadConversation.attachments?.filter { it.type == VIDEO }?.size ?: 0
        val audioCount = unreadConversation.attachments?.filter { it.type == AUDIO }?.size ?: 0
        val voiceNoteCount =
            unreadConversation.attachments?.filter { it.type == VOICE_NOTE }?.size ?: 0
        var updatedContentText = ChatroomUtil.removeTemporaryText(contentText)

        updatedContentText = when {
            updatedContentText.isNotEmpty() -> updatedContentText
            imagesCount > 0 && videoCount > 0 -> ""
            imagesCount > 0 -> if (imagesCount > 1) "Photos" else "Photo"
            gifsCount > 0 -> if (gifsCount > 1) "GIFs" else "GIF"
            videoCount > 0 -> if (videoCount > 1) "Videos" else "Video"
            pdfCount > 0 -> if (pdfCount > 1) "Documents" else "Document"
            audioCount > 0 -> if (audioCount > 1) "Audios" else "Audio"
            voiceNoteCount > 0 -> if (voiceNoteCount > 1) "Voice Notes" else "Voice Note"
            else -> ""
        }

        val mediaCountText = StringBuilder()
        if (imagesCount > 0) {
            if (imagesCount > 1 || videoCount > 0) {
                mediaCountText.append(imagesCount).append(" ")
                    .append(context.getString(R.string.lm_chat_camera_emoji))
            } else {
                mediaCountText.append(context.getString(R.string.lm_chat_camera_emoji))
            }
        }

        if (gifsCount > 0) {
            if (mediaCountText.isNotEmpty()) mediaCountText.append(" ")
            if (gifsCount > 1) {
                mediaCountText.append(gifsCount).append(" ")
                    .append(context.getString(R.string.lm_chat_gif_emoji))
            } else {
                mediaCountText.append(context.getString(R.string.lm_chat_gif_emoji))
            }
        }

        if (audioCount > 0) {
            if (mediaCountText.isNotEmpty()) mediaCountText.append(" ")
            if (audioCount > 1) {
                mediaCountText.append(audioCount).append(" ")
                    .append(context.getString(R.string.lm_chat_audio_emoji))
            } else {
                mediaCountText.append(context.getString(R.string.lm_chat_audio_emoji))
            }
        }

        if (voiceNoteCount > 0) {
            if (mediaCountText.isNotEmpty()) mediaCountText.append(" ")
            if (voiceNoteCount > 1) {
                mediaCountText.append(voiceNoteCount).append(" ")
                    .append(context.getString(R.string.lm_chat_mic_emoji))
            } else {
                mediaCountText.append(context.getString(R.string.lm_chat_mic_emoji))
            }
        }

        if (imagesCount > 0 || gifsCount > 0) {
            var imageUri: Uri? = null
            if (chatImageBitmap != null) {
                imageUri = FileUtil.getUriFromBitmapWithRandomName(
                    context,
                    chatImageBitmap,
                    shareUriExternally = true
                )
            } else if (chatImageUri != null) {
                imageUri = chatImageUri
            }

            if (imageUri != null) {
                imageMessage = NotificationCompat.MessagingStyle.Message(
                    "", notificationTime, unreadConversationPerson
                ).setData("image/", imageUri)
            }
        }

        if (pdfCount > 0) {
            if (mediaCountText.isNotEmpty()) mediaCountText.append(" ")
            if (pdfCount > 1) {
                mediaCountText.append(pdfCount).append(" ")
                    .append(context.getString(R.string.lm_chat_document_emoji))
            } else {
                mediaCountText.append(context.getString(R.string.lm_chat_document_emoji))
            }
        }

        if (videoCount > 0) {
            if (mediaCountText.isNotEmpty()) mediaCountText.append(" ")
            mediaCountText.append(videoCount).append(" ")
                .append(context.getString(R.string.lm_chat_video_emoji))
        }

        mediaCountText.append(" ").append(updatedContentText)

        if (mediaCountText.trim().isNotEmpty()) {
            mediaCountMessage = NotificationCompat.MessagingStyle.Message(
                mediaCountText.trim(), notificationTime, unreadConversationPerson
            )
        }

        mediaCountMessage?.let { messagesList.add(it) }
        imageMessage?.let { messagesList.add(it) }
        return messagesList
    }

    /**
     * Fetches the Uris asynchronously
     */
    private fun fetchUnreadConversationsFirstImage(
        context: Context,
        unreadConversation: ChatroomNotificationViewData,
        callback: (uri: Uri?) -> Unit,
    ) {
        val images = unreadConversation.attachments?.filter {
            it.type == IMAGE || it.type == GIF
        }.orEmpty()
        if (images.isNotEmpty()) {
            val imageUrl = images[0].thumbnail ?: images[0].url
            if (!imageUrl.isNullOrEmpty()) {
                MediaUtils.fetchImageUri(context, imageUrl) {
                    callback(it)
                }
            } else {
                callback(null)
            }
        } else {
            callback(null)
        }
    }
}