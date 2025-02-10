package com.likeminds.chatmm

import android.util.Log
import com.likeminds.chatmm.SDKApplication.Companion.LOG_TAG

object LMAnalytics {
    object Events {
        const val CHATROOM_LINK_CLICKED = "Chatroom link clicked"
        const val USER_TAGS_SOMEONE = "User tags someone"
        const val CHATROOM_MUTED = "Chatroom muted"
        const val CHATROOM_UNMUTED = "Chatroom unmuted"
        const val CHATROOM_RESPONDED = "Chatroom responded"
        const val MEMBER_PROFILE_VIEW = "Member profile view"
        const val POLL_CREATION_COMPLETED = "Poll creation completed"
        const val POLL_VOTED = "Poll voted"
        const val POLL_VOTING_SKIPPED = "Poll voting skipped"
        const val POLL_OPTION_CREATED = "Poll option created"
        const val POLL_VOTING_EDITED = "Poll voting edited"
        const val CHAT_ROOM_DELETED = "Chatroom deleted"
        const val CHAT_ROOM_FOLLOWED = "Chatroom followed"
        const val CHAT_ROOM_LEFT = "Chatroom left"
        const val CHAT_ROOM_OPENED = "Chatroom opened"
        const val CHAT_ROOM_SHARED = "Chatroom shared"
        const val CHAT_ROOM_UN_FOLLOWED = "Chatroom unfollowed"
        const val HOME_FEED_PAGE_OPENED = "Home feed page opened"
        const val REACTIONS_CLICKED = "Reactions Click"
        const val SEARCH_ICON_CLICKED = "Clicked search icon"
        const val SEARCH_CROSS_ICON_CLICKED = "Clicked cross search icon"
        const val CHATROOM_SEARCHED = "Chatroom searched"
        const val CHATROOM_SEARCH_CLOSED = "Chatroom search closed"
        const val MESSAGE_SEARCHED = "Message searched"
        const val MESSAGE_SEARCH_CLOSED = "Message search closed"
        const val EMOTICON_TRAY_OPENED = "Emoticon Tray Opened"
        const val REACTION_ADDED = "Reaction Added"
        const val REACTION_LIST_OPENED = "Reaction List Opened"
        const val REACTION_REMOVED = "Reaction Removed"
        const val CHATROOM_AUTO_FOLLOW = "Auto follow enabled"
        const val NOTIFICATION_RECEIVED = "Notification Received"
        const val NOTIFICATION_CLICKED = "Notification Clicked"
        const val SET_CHATROOM_TOPIC = "Current topic updated"

        //Audio Voice Note
        const val VOICE_NOTE_RECORDED = "Voice message recorded"
        const val VOICE_NOTE_PREVIEWED = "Voice message previewed"
        const val VOICE_NOTE_CANCELED = "Voice message canceled"
        const val VOICE_NOTE_SENT = "Voice message sent"
        const val VOICE_NOTE_PLAYED = "Voice message played"

        //Poll Results
        const val POLL_RESULT_VIEWED = "Poll answers viewed"
        const val POLL_RESULTS_TOGGLED = "Poll results toggled"

        //On boarding Flow
        const val COMMUNITY_TAB_CLICKED = "Community tab clicked"
        const val COMMUNITY_FEED_CLICKED = "Community feed clicked"

        //Attachments Clicked
        const val IMAGE_VIEWED = "Image viewed"
        const val VIDEO_PLAYED = "Video played"
        const val AUDIO_PLAYED = "Audio played"
        const val CHAT_LINK_CLICKED = "Chat link clicked"

        //Message Actions
        const val MESSAGE_EDITED = "Message Edited"
        const val MESSAGE_DELETED = "Message Deleted"
        const val MESSAGE_COPIED = "Message Copied"
        const val MESSAGE_REPLY = "Message Reply"

        //Moderation and Reporting
        const val MEMBER_PROFILE_REPORT = "Member profile report"
        const val MEMBER_PROFILE_REPORT_CONFIRMED = "Member profile report confirmed"
        const val MESSAGE_REPORTED = "Message reported"

        //Sync related
        const val SYNC_COMPLETE = "Sync Complete"

        //Third Party Share
        const val THIRD_PARTY_SHARING = "Third party sharing"
        const val THIRD_PARTY_ABANDONED = "Third party abandoned"

        const val SDK_INITIATE = "SDK initiate"
        const val DM_CHATROOM_CREATED = "DM Chatroom created"
    }

    object Keys {
        const val CHATROOM_ID = "chatroom_id"
        const val CHATROOM_NAME = "chatroom_name"
        const val CHATROOM_TYPE = "chatroom_type"
        const val COMMUNITY_NAME = "community_name"
        const val MESSAGE_ID = "message_id"
        const val COMMUNITY_ID = "community_id"
        const val UUID = "uuid"
        const val SOURCE = "source"
    }

    object Source {
        const val MESSAGE_REACTIONS_FROM_LONG_PRESS = "long press"
        const val MESSAGE_REACTIONS_FROM_REACTION_BUTTON = "reaction button"
        const val COMMUNITY_TAB = "community_tab"
        const val COMMUNITY_FEED = "home_feed"
        const val HOME_FEED = "explore_feed"
        const val NOTIFICATION = "notification"
        const val DEEP_LINK = "deep_link"
        const val POLL_RESULT = "poll_result"
        const val MESSAGE_REACTIONS = "message_reactions"
        const val DIRECT_MESSAGES_SCREEN = "direct_messages_screen"
    }

    /**
     * called to trigger events
     * @param eventName - name of the event to trigger
     * @param eventProperties - {key: value} pair for properties related to event
     * */
    fun track(eventName: String, eventProperties: Map<String, String?> = mapOf()) {
        Log.d(
            LOG_TAG, """
            eventName: $eventName
            eventProperties: $eventProperties
        """.trimIndent()
        )
        SDKApplication.getLikeMindsCallback()?.getAnalyticsEvents(eventName, eventProperties)
    }
}