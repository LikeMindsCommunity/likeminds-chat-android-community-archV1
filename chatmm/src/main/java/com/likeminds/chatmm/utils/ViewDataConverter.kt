package com.likeminds.chatmm.utils

import android.content.Context
import android.net.Uri
import com.likeminds.chatmm.chatroom.detail.model.*
import com.likeminds.chatmm.chatroom.detail.util.ChatroomUtil
import com.likeminds.chatmm.chatroom.explore.model.ExploreViewData
import com.likeminds.chatmm.conversation.model.*
import com.likeminds.chatmm.dm.model.CheckDMLimitViewData
import com.likeminds.chatmm.dm.model.CheckDMTabViewData
import com.likeminds.chatmm.homefeed.model.ChannelInviteViewData
import com.likeminds.chatmm.media.model.SingleUriData
import com.likeminds.chatmm.member.model.*
import com.likeminds.chatmm.member.util.MemberImageUtil
import com.likeminds.chatmm.polls.model.PollInfoData
import com.likeminds.chatmm.polls.model.PollViewData
import com.likeminds.chatmm.pushnotification.model.*
import com.likeminds.chatmm.reactions.model.ReactionViewData
import com.likeminds.chatmm.report.model.ReportTagViewData
import com.likeminds.chatmm.search.model.*
import com.likeminds.chatmm.search.util.SearchUtils
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.chatmm.utils.mediauploader.worker.UploadHelper
import com.likeminds.chatmm.utils.membertagging.model.TagViewData
import com.likeminds.chatmm.utils.model.ITEM_VIEW_PARTICIPANTS
import com.likeminds.chatmm.widget.model.WidgetViewData
import com.likeminds.likemindschat.chatroom.model.*
import com.likeminds.likemindschat.community.model.Member
import com.likeminds.likemindschat.conversation.model.*
import com.likeminds.likemindschat.dm.model.CheckDMLimitResponse
import com.likeminds.likemindschat.dm.model.CheckDMTabResponse
import com.likeminds.likemindschat.helper.model.GroupTag
import com.likeminds.likemindschat.moderation.model.ReportTag
import com.likeminds.likemindschat.notification.model.*
import com.likeminds.likemindschat.poll.model.Poll
import com.likeminds.likemindschat.search.model.SearchChatroom
import com.likeminds.likemindschat.search.model.SearchConversation
import com.likeminds.likemindschat.user.model.*
import com.likeminds.likemindschat.widget.model.Widget
import java.io.File

object ViewDataConverter {

    /**--------------------------------
     * Network Model -> View Data Model
    --------------------------------*/

    fun convertChatroom(
        chatroom: Chatroom?,
        currentMemberId: String? = null,
        viewType: Int = 0,
    ): ChatroomViewData? {
        if (chatroom == null) {
            return null
        }

        var showFollowTelescope = false
        var showFollowAutoTag = false
        if (chatroom.followStatus == false) {
            showFollowTelescope = true
        }
        if (chatroom.member?.sdkClientInfo?.uuid == currentMemberId) {
            showFollowTelescope = false
        }
        if (chatroom.isTagged == true) {
            showFollowTelescope = false
            showFollowAutoTag = true
        }
        if (chatroom.followStatus == true) {
            showFollowTelescope = false
            showFollowAutoTag = false
        }

        return ChatroomViewData.Builder()
            .id(chatroom.id)
            .communityId(chatroom.communityId.toString())
            .communityName(chatroom.communityName ?: "")
            .memberViewData(convertMember(chatroom.member))
            .createdAt(chatroom.createdAt ?: 0)
            .title(chatroom.title)
            .answerText(ChatroomUtil.removeTemporaryText(chatroom.answerText))
            .state(chatroom.state)
            .shareUrl(chatroom.shareUrl)
            .type(chatroom.type)
            .date(chatroom.date)
            .about(chatroom.about)
            .header(chatroom.header)
            .showFollowTelescope(showFollowTelescope)
            .showFollowAutoTag(showFollowAutoTag)
            .cardCreationTime(chatroom.cardCreationTime)
            .totalResponseCount(chatroom.totalResponseCount)
            .totalAllResponseCount(chatroom.totalAllResponseCount ?: 0)
            .dynamicViewType(viewType)
            .muteStatus(chatroom.muteStatus ?: false)
            .followStatus(chatroom.followStatus ?: false)
            .date(chatroom.date)
            .isTagged(chatroom.isTagged)
            .deletedBy(chatroom.deletedBy)
            .updatedAt(chatroom.updatedAt)
            .draftConversation(chatroom.draftConversation)
            .isSecret(chatroom.isSecret)
            .secretChatroomParticipants(chatroom.secretChatroomParticipants?.toList())
            .secretChatroomLeft(chatroom.secretChatroomLeft)
            .unseenCount(chatroom.unseenCount)
            .isEdited(chatroom.isEdited)
            .topic(convertConversation(chatroom.topic))
            .reactions(chatroom.reactions?.mapNotNull { reaction ->
                convertChatroomReactions(reaction, chatroom.id)
            })
            .memberCanMessage(chatroom.memberCanMessage)
            .chatroomImageUrl(chatroom.chatroomImageUrl)
            .participantsCount(chatroom.participantsCount?.toInt() ?: 0)
            .isPending(chatroom.isPending)
            .isPinned(chatroom.isPinned)
            .unreadConversationCount(chatroom.unreadConversationCount)
            .autoFollowDone(chatroom.autoFollowDone)
            .deletedByMember(convertMember(chatroom.deletedByMember))
            .chatRequestState(chatroom.chatRequestState)
            .chatRequestedById(chatroom.chatRequestedById)
            .chatRequestedBy(convertMember(chatroom.chatRequestedBy))
            .chatRequestCreatedAt(chatroom.chatRequestCreatedAt)
            .isPrivateMember(chatroom.isPrivateMember)
            .chatroomWithUser(convertMember(chatroom.chatroomWithUser))
            .chatroomWithUserId(chatroom.chatroomWithUserId)
            .build()
    }

    private fun convertChatroomReactions(
        reactionRO: Reaction?,
        id: String,
    ): ReactionViewData? {
        if (reactionRO == null) {
            return null
        }
        val memberViewData = convertMember(reactionRO.member)
        return ReactionViewData.Builder()
            .reaction(reactionRO.reaction)
            .memberViewData(memberViewData)
            .chatroomId(id)
            .build()
    }

    fun convertChatroomForHome(
        chatroom: Chatroom,
        dynamicViewType: Int? = null
    ): ChatroomViewData {
        return ChatroomViewData.Builder()
            .id(chatroom.id)
            .communityId(chatroom.communityId ?: "")
            .communityName(chatroom.communityName ?: "")
            .memberViewData(convertMember(chatroom.member))
            .memberState(chatroom.member?.state)
            .createdAt(chatroom.createdAt ?: 0)
            .title(chatroom.title)
            .answerText(ChatroomUtil.removeTemporaryText(chatroom.answerText))
            .state(chatroom.state)
            .type(chatroom.type)
            .header(chatroom.header)
            .dynamicViewType(dynamicViewType)
            .muteStatus(chatroom.muteStatus ?: false)
            .followStatus(chatroom.followStatus ?: false)
            .date(chatroom.date)
            .isTagged(chatroom.isTagged)
            .isPending(chatroom.isPending)
            .deletedBy(chatroom.deletedBy)
            .deletedByMember(convertMember(chatroom.deletedByMember))
            .updatedAt(chatroom.updatedAt)
            .isSecret(chatroom.isSecret)
            .unseenCount(chatroom.unseenCount)
            .isEdited(chatroom.isEdited)
            .chatroomImageUrl(chatroom.chatroomImageUrl)
            .chatRequestState(chatroom.chatRequestState)
            .chatRequestedById(chatroom.chatRequestedById)
            .chatRequestedBy(convertMember(chatroom.chatRequestedBy))
            .chatRequestCreatedAt(chatroom.chatRequestCreatedAt)
            .isPrivateMember(chatroom.isPrivateMember)
            .chatroomWithUser(convertMember(chatroom.chatroomWithUser))
            .chatroomWithUserId(chatroom.chatroomWithUserId)
            .build()
    }

    fun convertChatroomForExplore(
        chatroom: Chatroom?,
        memberUUID: String,
        sortIndex: Int
    ): ExploreViewData? {
        if (chatroom == null) return null
        return ExploreViewData.Builder()
            .isPinned(chatroom.isPinned ?: false)
            .isCreator(chatroom.member?.sdkClientInfo?.uuid == memberUUID)
            .externalSeen(chatroom.externalSeen)
            .isSecret(chatroom.isSecret ?: false)
            .followStatus(chatroom.followStatus ?: false)
            .participantsCount(chatroom.participantsCount?.toInt() ?: 0)
            .totalResponseCount(chatroom.totalResponseCount)
            .id(chatroom.id)
            .header(chatroom.header ?: "")
            .title(chatroom.title)
            .imageUrl(chatroom.member?.imageUrl)
            .chatroomImageUrl(chatroom.chatroomImageUrl)
            .chatroomViewData(convertChatroom(chatroom))
            .build()
    }

    /**
     * convert [Conversation] to [ConversationViewData]
     */
    fun convertConversations(conversations: List<Conversation>): List<ConversationViewData> {
        return conversations.mapNotNull {
            convertConversation(it)
        }
    }

    /**
     * convert [Conversation] to [ConversationViewData]
     */
    fun convertConversation(
        conversation: Conversation?,
        memberViewData: MemberViewData? = null
    ): ConversationViewData? {
        if (conversation == null) {
            return null
        }
        val updatedAnswer = ChatroomUtil.removeTemporaryText(conversation.answer)
        return ConversationViewData.Builder()
            .id(conversation.id ?: "")
            .memberViewData(memberViewData ?: convertMember(conversation.member))
            .createdAt(conversation.createdAt.toString())
            .createdEpoch(conversation.createdEpoch ?: 0L)
            .answer(updatedAnswer)
            .state(conversation.state)
            .attachments(
                conversation.attachments?.mapNotNull { attachment ->
                    convertAttachment(
                        attachment,
                        conversation.member?.name ?: "",
                        "${conversation.date ?: ""}, ${conversation.createdAt ?: ""}"
                    )
                }?.let {
                    ArrayList(it)
                }
            )
            .ogTags(convertOGTags(conversation.ogTags))
            .date(conversation.date)
            .deletedBy(conversation.deletedBy)
            .chatroomId(conversation.chatroomId)
            .communityId(conversation.communityId)
            .lastSeen(conversation.lastSeen)
            .replyConversation(convertConversation(conversation.replyConversation))
            .isEdited(conversation.isEdited)
            .localCreatedEpoch(conversation.localCreatedEpoch)
            .replyChatroomId(conversation.replyChatroomId)
            .attachmentCount(conversation.attachmentCount ?: 0)
            .attachmentsUploaded(conversation.attachmentUploaded)
            .workerUUID(conversation.workerUUID)
            .temporaryId(conversation.temporaryId)
            .shortAnswer(ViewMoreUtil.getShortAnswer(updatedAnswer, 1000))
            .pollInfoData(convertPollInfoData(conversation))
            .lastSeen(conversation.lastSeen)
            .reactions(convertConversationReactions(conversation.reactions, conversation.id))
            .deletedByMember(convertMember(conversation.deletedByMember))
            .widgetId(conversation.widgetId)
            .widget(convertWidget(conversation.widget))
            .attachmentsUploadedEpoch(conversation.attachmentsUploadedEpoch)
            .build()
    }

    // converts network [Member] to [MemberViewData]
    fun convertMember(member: Member?): MemberViewData {
        if (member == null) {
            return MemberViewData.Builder().build()
        }
        return MemberViewData.Builder()
            .id(member.id)
            .name(member.name)
            .imageUrl(member.imageUrl)
            .userUniqueId(member.userUniqueId)
            .state(member.state ?: 0)
            .customIntroText(member.customIntroText)
            .customClickText(member.customClickText)
            .customTitle(member.customTitle)
            .communityId(member.communityId.toString())
            .isGuest(member.isGuest)
            .sdkClientInfo(convertSDKClientInfo(member.sdkClientInfo))
            .uuid(member.uuid)
            .isOwner(member.isOwner)
            .memberSince(member.memberSince)
            .roles(member.roles)
            .build()
    }

    // converts list of conversation reactions to list of [ReactionViewData]
    private fun convertConversationReactions(
        reactions: List<Reaction>?,
        conversationId: String?
    ): List<ReactionViewData>? {
        return reactions?.map {
            convertConversationReaction(it, conversationId)
        }
    }

    // converts a [Reaction] to [ReactionViewData]
    private fun convertConversationReaction(
        reactionData: Reaction,
        conversationId: String?
    ): ReactionViewData {
        return ReactionViewData.Builder()
            .reaction(reactionData.reaction)
            .memberViewData(convertMember(reactionData.member))
            .conversationId(conversationId)
            .build()
    }

    // converts network model [MemberStateResponse] to [MemberStateViewData]
    fun convertMemberState(memberStateResponse: MemberStateResponse?): MemberStateViewData? {
        if (memberStateResponse == null) {
            return null
        }
        return MemberStateViewData.Builder()
            .state(memberStateResponse.state)
            .memberViewData(convertMemberFromMemberState(memberStateResponse))
            .memberRights(memberStateResponse.memberRights.mapNotNull {
                convertManagementRights(it)
            })
            .build()
    }

    private fun convertMemberFromMemberState(
        memberStateResponse: MemberStateResponse?
    ): MemberViewData? {
        if (memberStateResponse == null) {
            return null
        }

        return MemberViewData.Builder()
            .id(memberStateResponse.id)
            .state(memberStateResponse.state)
            .userUniqueId(memberStateResponse.userUniqueId)
            .customTitle(memberStateResponse.customTitle)
            .imageUrl(memberStateResponse.imageUrl)
            .isGuest(memberStateResponse.isGuest)
            .name(memberStateResponse.name)
            .updatedAt(memberStateResponse.updatedAt)
            .isOwner(memberStateResponse.isOwner)
            .sdkClientInfo(convertSDKClientInfo(memberStateResponse.sdkClientInfo))
            .roles(memberStateResponse.roles)
            .build()
    }

    // converts network model [ManagementRightPermissionData] to [ManagementRightPermissionViewData]
    fun convertManagementRights(
        managementRightPermissionData: ManagementRightPermissionData?
    ): ManagementRightPermissionViewData? {
        if (managementRightPermissionData == null) {
            return null
        }
        return ManagementRightPermissionViewData.Builder()
            .id(managementRightPermissionData.id)
            .state(managementRightPermissionData.state)
            .title(managementRightPermissionData.title)
            .subtitle(managementRightPermissionData.subtitle)
            .isSelected(managementRightPermissionData.isSelected)
            .isLocked(managementRightPermissionData.isLocked)
            .build()
    }

    // converts poll data from network conversation to [PollInfoData]
    private fun convertPollInfoData(conversation: Conversation): PollInfoData {
        return PollInfoData.Builder()
            .allowAddOption(conversation.allowAddOption)
            .pollType(conversation.pollType)
            .pollViewDataList(convertPolls(conversation.polls))
            .expiryTime(conversation.expiryTime)
            .isAnonymous(conversation.isAnonymous)
            .multipleSelectNum(conversation.multipleSelectNum)
            .pollTypeText(conversation.pollTypeText)
            .submitTypeText(conversation.submitTypeText)
            .multipleSelectState(conversation.multipleSelectState)
            .pollAnswerText(conversation.pollAnswerText)
            .toShowResult(conversation.toShowResults)
            .isPollSubmitted(checkIsPollSubmitted(conversation.polls?.toMutableList()))
            .build()
    }

    // checks if poll is submitted or not and sets poll isSelected key
    private fun checkIsPollSubmitted(polls: MutableList<Poll>?): Boolean {
        var isPollSubmitted = false
        polls?.forEach {
            if (it.isSelected == true) {
                isPollSubmitted = true
            }
        }
        return isPollSubmitted
    }

    // converts network list of [Poll] to list of [PollViewData]
    private fun convertPolls(polls: List<Poll>?): List<PollViewData>? {
        return polls?.map {
            convertPoll(it)
        }
    }

    // converts network [Poll] to [PollViewData]
    fun convertPoll(poll: Poll): PollViewData {
        return PollViewData.Builder()
            .id(poll.id)
            .member(convertMember(poll.member))
            .isSelected(poll.isSelected)
            .text(poll.text)
            .percentage(poll.percentage)
            .noVotes(poll.noVotes)
            .isSelected(poll.isSelected)
            .build()
    }

    // converts SDKClientInfo network model to view data model
    private fun convertSDKClientInfo(
        sdkClientInfo: SDKClientInfo?
    ): SDKClientInfoViewData {
        if (sdkClientInfo == null) {
            return SDKClientInfoViewData.Builder().build()
        }
        return SDKClientInfoViewData.Builder()
            .communityId(sdkClientInfo.community)
            .user(sdkClientInfo.user)
            .userUniqueId(sdkClientInfo.userUniqueId)
            .uuid(sdkClientInfo.uuid)
            .build()
    }

    // converts LinkOGTags view data model to network model
    private fun convertOGTags(
        linkOGTags: LinkOGTags?
    ): LinkOGTagsViewData? {
        if (linkOGTags == null) {
            return null
        }
        return LinkOGTagsViewData.Builder()
            .title(linkOGTags.title)
            .image(linkOGTags.image)
            .description(linkOGTags.description)
            .url(linkOGTags.url)
            .build()
    }

    private fun convertAttachment(
        attachment: Attachment?,
        title: String? = null,
        subTitle: String? = null
    ): AttachmentViewData? {
        if (attachment == null) {
            return null
        }
        val attachmentMeta = if (attachment.meta != null) {
            AttachmentMetaViewData.Builder()
                .duration(attachment.meta?.duration)
                .numberOfPage(attachment.meta?.numberOfPage)
                .size(attachment.meta?.size)
                .build()
        } else {
            null
        }
        return AttachmentViewData.Builder()
            .id(attachment.id)
            .name(attachment.name)
            .uri(Uri.parse(attachment.url))
            .url(attachment.url)
            .type(attachment.type)
            .index(attachment.index)
            .width(attachment.width)
            .height(attachment.height)
            .title(title)
            .subTitle(subTitle)
            .awsFolderPath(attachment.awsFolderPath)
            .localFilePath(attachment.localFilePath)
            .thumbnail(attachment.thumbnailUrl)
            .thumbnailAWSFolderPath(attachment.thumbnailAWSFolderPath)
            .thumbnailLocalFilePath(attachment.thumbnailLocalFilePath)
            .meta(attachmentMeta)
            .isUploaded(attachment.isUploaded)
            .build()
    }

    fun convertUser(user: User?): MemberViewData? {
        if (user == null) {
            return null
        }
        return MemberViewData.Builder()
            .id(user.id)
            .name(user.name)
            .imageUrl(user.imageUrl)
            .customTitle(user.customTitle)
            .isGuest(user.isGuest)
            .sdkClientInfo(convertSDKClientInfo(user.sdkClientInfo))
            .roles(user.roles)
            .build()
    }

    fun convertGroupTag(groupTag: GroupTag?): TagViewData? {
        if (groupTag == null) return null
        return TagViewData.Builder()
            .name(groupTag.name)
            .imageUrl(groupTag.imageUrl)
            .tag(groupTag.tag)
            .route(groupTag.route)
            .description(groupTag.description)
            .build()
    }

    fun convertMemberTag(member: Member?): TagViewData? {
        if (member == null) return null
        val nameDrawable = MemberImageUtil.getNameDrawable(
            MemberImageUtil.SIXTY_PX,
            member.sdkClientInfo?.uuid,
            member.name
        )
        return TagViewData.Builder()
            .name(member.name)
            .id(member.id.toInt())
            .imageUrl(member.imageUrl)
            .isGuest(member.isGuest)
            .userUniqueId(member.userUniqueId)
            .placeHolder(nameDrawable.first)
            .uuid(member.sdkClientInfo?.uuid ?: "")
            .build()
    }

    /**
     * convert [LinkOGTags] to [LinkOGTagsViewData]
     * @param linkOGTags: object of [LinkOGTags]
     **/
    fun convertLinkOGTags(linkOGTags: LinkOGTags): LinkOGTagsViewData {
        return LinkOGTagsViewData.Builder()
            .url(linkOGTags.url)
            .description(linkOGTags.description)
            .title(linkOGTags.title)
            .image(linkOGTags.image)
            .build()
    }

    /**
     * convert [LinkOGTags] to [LinkOGTagsViewData]
     * @param linkOGTags: object of [LinkOGTags]
     **/
    fun convertChatroomActions(chatroomActions: List<ChatroomAction>): List<ChatroomActionViewData> {
        return chatroomActions.map {
            convertChatroomAction(it)
        }
    }

    /**
     * convert [ChatroomAction] to [ChatroomActionViewData]
     *
     * @param chatroomAction: object to [ChatroomAction]
     */
    private fun convertChatroomAction(chatroomAction: ChatroomAction): ChatroomActionViewData {
        return ChatroomActionViewData.Builder()
            .id(chatroomAction.id.toString())
            .title(chatroomAction.title)
            .route(chatroomAction.route)
            .build()
    }

    /**
     * convert [Widget] to [WidgetViewData]
     *
     * @param widget: object to [Widget]
     */
    private fun convertWidget(widget: Widget?): WidgetViewData? {
        if (widget == null) return null
        return WidgetViewData.Builder()
            .id(widget.id)
            .parentEntityId(widget.parentEntityId)
            .parentEntityType(widget.parentEntityType)
            .metadata(widget.metadata.toString())
            .createdAt(widget.createdAt)
            .updatedAt(widget.updatedAt)
            .build()
    }

    /**
     * converts list of [ChannelInvite] to list of [ChannelInviteViewData]
     *
     * @param channelInvites: list of objects to [ChannelInvite]
     */
    fun convertChannelInvites(channelInvites: List<ChannelInvite>): List<ChannelInviteViewData> {
        return channelInvites.map { channelInvite ->
            convertChannelInvite(channelInvite)
        }
    }

    /**
     * converts [ChannelInvite] to [ChannelInviteViewData]
     *
     * @param channelInvite: object to [ChannelInvite]
     */
    private fun convertChannelInvite(channelInvite: ChannelInvite): ChannelInviteViewData {
        return ChannelInviteViewData.Builder()
            .id(channelInvite.id)
            .inviteReceiver(convertMember(channelInvite.inviteReceiver))
            .inviteSender(convertMember(channelInvite.inviteSender))
            .invitedChatroom(convertChatroomForHome(channelInvite.chatroom))
            .inviteStatus(channelInvite.inviteStatus)
            .createdAt(channelInvite.createdAt)
            .updatedAt(channelInvite.updatedAt)
            .build()
    }

    /**--------------------------------
     * View Data Model -> Network Model
    --------------------------------*/

    // creates a Conversation network model for posting a conversation
    fun convertConversation(
        context: Context,
        uuid: String,
        communityId: String?,
        request: PostConversationRequest,
        fileUris: List<SingleUriData>?,
        conversationCreatedEpoch: Long,
        loggedInUserUUID: String
    ): Conversation {

        val metadata = request.metadata
        val widget = if (metadata != null) {
            Widget.Builder()
                .id("-$conversationCreatedEpoch")
                .metadata(metadata)
                .parentEntityType("message")
                .createdAt(conversationCreatedEpoch)
                .updatedAt(conversationCreatedEpoch)
                .build()
        } else {
            null
        }

        val chatroomId = request.chatroomId

        return Conversation.Builder()
            .id(request.temporaryId)
            .chatroomId(chatroomId)
            .communityId(communityId)
            .answer(request.text)
            .state(ConversationState.NORMAL.value)
            .createdEpoch(conversationCreatedEpoch)
            .memberId(uuid)
            .createdAt(TimeUtil.generateCreatedAt(conversationCreatedEpoch))
            .attachments(convertAttachments(context, fileUris, chatroomId, loggedInUserUUID))
            .lastSeen(true)
            .ogTags(request.ogTags)
            .date(TimeUtil.generateDate(conversationCreatedEpoch))
            .replyConversationId(request.repliedConversationId)
            .attachmentCount(fileUris?.size ?: 0)
            .localCreatedEpoch(conversationCreatedEpoch)
            .temporaryId(request.temporaryId)
            .isEdited(false)
            .replyChatroomId(request.repliedChatroomId)
            .attachmentUploaded(false)
            .widget(widget)
            .build()
    }

    // converts list of SingleUriData to list of network Attachment model
    private fun convertAttachments(
        context: Context,
        fileUris: List<SingleUriData>?,
        chatroomId: String,
        loggedInUserUUID: String
    ): List<Attachment>? {
        return fileUris?.mapIndexed { index, singleUriData ->
            convertAttachment(context, singleUriData, index, chatroomId, loggedInUserUUID)
        }
    }

    // converts SingleUriData to network Attachment model
    private fun convertAttachment(
        context: Context,
        singleUriData: SingleUriData,
        index: Int,
        chatroomId: String,
        loggedInUserUUID: String
    ): Attachment {
        val attachmentMeta = AttachmentMeta.Builder()
            .numberOfPage(singleUriData.pdfPageCount)
            .duration(singleUriData.duration)
            .size(singleUriData.size)
            .build()

        //local file path
        val localFilePath = FileUtil.getRealPath(context, singleUriData.uri).path

        //file
        val file = File(localFilePath)

        //server path
        val serverPath = UploadHelper.getAttachmentFilePath(
            chatroomId,
            loggedInUserUUID,
            singleUriData.fileType,
            file
        )

        val attachmentBuilder = Attachment.Builder()
            .name(singleUriData.mediaName)
            .url(singleUriData.uri.toString())
            .type(singleUriData.fileType)
            .index(index)
            .width(singleUriData.width)
            .height(singleUriData.height)
            .awsFolderPath(serverPath)
            .localFilePath(localFilePath)
            .meta(attachmentMeta)

        //add thumbnail meta if exist
        val thumbnailUri = singleUriData.thumbnailUri
        if (thumbnailUri != null) {
            val thumbnailLocalFilePath = FileUtil.getRealPath(context, thumbnailUri).path
            val thumbnailFile = File(thumbnailLocalFilePath)
            val thumbnailAWSFolderPath = UploadHelper.getAttachmentFilePath(
                chatroomId,
                loggedInUserUUID,
                singleUriData.fileType,
                thumbnailFile,
                isThumbnail = true
            )

            attachmentBuilder.thumbnailUrl(thumbnailUri.toString())
                .thumbnailLocalFilePath(thumbnailLocalFilePath)
                .thumbnailAWSFolderPath(thumbnailAWSFolderPath)
        }

        val attachment = attachmentBuilder.build()
        return attachment
    }

    // converts [ConversationViewData] to [Conversation] model
    fun convertConversation(
        conversationViewData: ConversationViewData
    ): Conversation {
        return Conversation.Builder()
            .id(conversationViewData.id)
            .chatroomId(conversationViewData.chatroomId)
            .communityId(conversationViewData.communityId)
            .answer(conversationViewData.answer)
            .createdEpoch(conversationViewData.createdEpoch)
            .memberId(conversationViewData.memberViewData.sdkClientInfo.uuid)
            .createdAt(conversationViewData.createdAt)
            .attachments(convertAttachmentViewDataList(conversationViewData.attachments))
            .lastSeen(conversationViewData.lastSeen)
            .ogTags(convertLinkOGTags(conversationViewData.ogTags))
            .date(conversationViewData.date)
            .isEdited(conversationViewData.isEdited)
            .state(conversationViewData.state)
            .replyConversationId(conversationViewData.replyConversation?.id)
            .replyChatroomId(conversationViewData.replyChatroomId)
            .attachmentCount(conversationViewData.attachmentCount)
            .attachmentUploaded(conversationViewData.attachmentsUploaded)
            .workerUUID(conversationViewData.workerUUID)
            .localCreatedEpoch(conversationViewData.localCreatedEpoch)
            .deletedBy(conversationViewData.deletedBy)
            .temporaryId(conversationViewData.temporaryId)
            .isEdited(conversationViewData.isEdited)
            .replyChatroomId(conversationViewData.replyChatroomId)
            .attachmentsUploadedEpoch(conversationViewData.attachmentsUploadedEpoch)
            .build()
    }

    // converts list of AttachmentViewData to list of network Attachment model
    fun convertAttachmentViewDataList(attachmentViewDataList: List<AttachmentViewData>?): List<Attachment>? {
        return attachmentViewDataList?.map { attachmentViewData ->
            convertAttachmentViewData(attachmentViewData)
        }
    }

    // converts AttachmentViewData to network Attachment model
    private fun convertAttachmentViewData(
        attachmentViewData: AttachmentViewData,
    ): Attachment {
        return Attachment.Builder()
            .name(attachmentViewData.name)
            .url(attachmentViewData.url ?: "")
            .type(attachmentViewData.type)
            .index(attachmentViewData.index)
            .width(attachmentViewData.width)
            .height(attachmentViewData.height)
            .awsFolderPath(attachmentViewData.awsFolderPath)
            .thumbnailAWSFolderPath(attachmentViewData.thumbnailAWSFolderPath)
            .localFilePath(attachmentViewData.localFilePath)
            .thumbnailUrl(attachmentViewData.thumbnail.toString())
            .thumbnailLocalFilePath(attachmentViewData.thumbnailLocalFilePath.toString())
            .meta(
                AttachmentMeta.Builder()
                    .numberOfPage(attachmentViewData.meta?.numberOfPage)
                    .duration(attachmentViewData.meta?.duration)
                    .size(attachmentViewData.meta?.size)
                    .build()
            )
            .isUploaded(attachmentViewData.isUploaded)
            .build()
    }

    // converts LinkOGTags view data model to network model
    fun convertLinkOGTags(
        linkOGTagsViewData: LinkOGTagsViewData?
    ): LinkOGTags? {
        if (linkOGTagsViewData == null) {
            return null
        }
        return LinkOGTags.Builder()
            .title(linkOGTagsViewData.title)
            .image(linkOGTagsViewData.image)
            .description(linkOGTagsViewData.description)
            .url(linkOGTagsViewData.url)
            .build()
    }

    // converts PollViewData model list to network Poll model list
    fun createPolls(pollViewDataList: List<PollViewData>): List<Poll> {
        return pollViewDataList.map {
            convertPoll(it)
        }
    }

    // converts PollViewData model to network Poll model
    fun convertPoll(pollViewData: PollViewData): Poll {
        return Poll.Builder()
            .id(pollViewData.id)
            .isSelected(pollViewData.isSelected)
            .noVotes(pollViewData.noVotes)
            .percentage(pollViewData.percentage)
            .text(pollViewData.text)
            .build()
    }

    // creates [GetUnreadChatroomsRequest] from [ChatroomNotificationViewData]
    fun createGetUnreadChatroomsRequest(
        chatroomNotificationViewData: ChatroomNotificationViewData
    ): GetUnreadChatroomsRequest {

        chatroomNotificationViewData.apply {
            return GetUnreadChatroomsRequest.Builder()
                .chatroom(
                    Chatroom.Builder()
                        .id(chatroomId)
                        .title(chatroomTitle)
                        .header(chatroomName)
                        .communityName(communityName)
                        .communityId(communityId.toString())
                        .member(createMember(chatroomNotificationViewData.chatroomCreator))
                        .muteStatus(false)
                        .followStatus(true)
                        .build()
                )
                .chatroomLastConversation(
                    Conversation.Builder()
                        .id(chatroomLastConversationId)
                        .answer(chatroomLastConversation ?: "")
                        .createdEpoch(chatroomLastConversationTimestamp)
                        .chatroomId(chatroomId)
                        .communityId(communityId.toString())
                        .attachments(createAttachmentsFromNotification(attachments))
                        .member(createMember(chatroomNotificationViewData.chatroomLastConversationCreator))
                        .build()
                )
                .build()
        }
    }

    // creates list of [Attachment] from list of attachments received in notification
    private fun createAttachmentsFromNotification(attachments: List<AttachmentViewData>?): List<Attachment>? {
        return attachments?.map { notificationAttachment ->
            createAttachmentFromNotification(notificationAttachment)
        }
    }

    // creates [Attachment] from the attachment received in notification
    private fun createAttachmentFromNotification(attachment: AttachmentViewData): Attachment {
        return Attachment.Builder()
            //todo:
            .id("-${System.currentTimeMillis()}")
            .type(attachment.type)
            .url(attachment.url ?: "")
            .height(attachment.height)
            .width(attachment.width)
            .name(attachment.name)
            .meta(createAttachmentMetaFromNotification(attachment.meta))
            .isUploaded(attachment.isUploaded)
            .build()
    }

    // creates [Attachment] from the attachment meta received in notification
    private fun createAttachmentMetaFromNotification(attachmentMetaData: AttachmentMetaViewData?): AttachmentMeta? {
        if (attachmentMetaData == null) {
            return null
        }

        return AttachmentMeta.Builder()
            .duration(attachmentMetaData.duration)
            .size(attachmentMetaData.size)
            .numberOfPage(attachmentMetaData.numberOfPage)
            .build()
    }

    // creates [Member] from the member object received in notification
    private fun createMember(memberViewData: MemberViewData?): Member? {
        if (memberViewData == null) {
            return null
        }

        return Member.Builder()
            .id(memberViewData.id ?: "")
            .imageUrl(memberViewData.imageUrl ?: "")
            .isGuest(memberViewData.isGuest ?: false)
            .name(memberViewData.name ?: "")
            .updatedAt(memberViewData.updatedAt)
            .userUniqueId(memberViewData.userUniqueId ?: "")
            .uuid(memberViewData.uuid)
            .sdkClientInfo(createSDKClientInfo(memberViewData.sdkClientInfo))
            .build()
    }

    // creates [SDKClientInfo] from the sdk client info object received in notification
    private fun createSDKClientInfo(sdkClientInfoViewData: SDKClientInfoViewData?): SDKClientInfo? {
        if (sdkClientInfoViewData == null) {
            return null
        }

        return SDKClientInfo(
            sdkClientInfoViewData.communityId,
            sdkClientInfoViewData.user,
            sdkClientInfoViewData.userUniqueId,
            sdkClientInfoViewData.uuid,
        )
    }

    fun convertSearchChatroomHeaders(
        chatrooms: List<SearchChatroom>,
        followStatus: Boolean,
        keyword: String
    ): List<SearchChatroomHeaderViewData> {
        return chatrooms.map {
            convertSearchChatroomHeader(it, followStatus, keyword)
        }
    }

    private fun convertSearchChatroomHeader(
        searchedChatroom: SearchChatroom,
        followStatus: Boolean,
        keyword: String
    ): SearchChatroomHeaderViewData {
        return SearchChatroomHeaderViewData.Builder()
            .chatroom(convertChatroomForSearch(searchedChatroom))
            .followStatus(followStatus)
            .keywordMatchedInCommunityName(
                SearchUtils.findMatchedKeywords(
                    keyword,
                    searchedChatroom.community.name
                )
            )
            .build()
    }

    fun convertSearchChatroomTitles(
        chatrooms: List<SearchChatroom>,
        followStatus: Boolean,
        keyword: String
    ): List<SearchChatroomTitleViewData> {
        return chatrooms.map {
            convertSearchChatroomTitle(it, followStatus, keyword)
        }
    }

    private fun convertSearchChatroomTitle(
        searchChatroom: SearchChatroom,
        followStatus: Boolean,
        keyword: String
    ): SearchChatroomTitleViewData {
        return SearchChatroomTitleViewData.Builder()
            .chatroom(convertChatroomForSearch(searchChatroom))
            .followStatus(followStatus)
            .keywordMatchedInCommunityName(
                SearchUtils.findMatchedKeywords(
                    keyword,
                    searchChatroom.community.name
                )
            )
            .keywordMatchedInChatroomName(
                SearchUtils.findMatchedKeywords(
                    keyword,
                    searchChatroom.chatroom.header
                )
            )
            .keywordMatchedInMessageText(
                SearchUtils.findMatchedKeywords(
                    keyword,
                    searchChatroom.chatroom.title,
                    isMessage = true
                )
            )
            .build()
    }

    private fun convertChatroomForSearch(
        searchChatroom: SearchChatroom
    ): ChatroomViewData {
        val member = MemberViewData.Builder()
            .id(searchChatroom.member.id)
            .name(searchChatroom.member.name)
            .roles(searchChatroom.member.roles)
            .build()

        return ChatroomViewData.Builder()
            .id(searchChatroom.chatroom.id)
            .communityId(searchChatroom.chatroom.communityId ?: "")
            .communityName(searchChatroom.chatroom.communityName ?: "")
            .memberViewData(member)
            .dynamicViewType(0)
            .createdAt(searchChatroom.chatroom.createdAt ?: 0L)
            .title(searchChatroom.chatroom.title)
            .answerText(ChatroomUtil.removeTemporaryText(searchChatroom.chatroom.answerText))
            .state(searchChatroom.state)
            .type(searchChatroom.chatroom.type)
            .header(searchChatroom.chatroom.header)
            .muteStatus(searchChatroom.muteStatus)
            .followStatus(searchChatroom.followStatus)
            .date(searchChatroom.chatroom.date)
            .isTagged(searchChatroom.isTagged)
            .isPending(searchChatroom.chatroom.isPending)
            .deletedBy(searchChatroom.chatroom.deletedBy)
            .deletedByMember(convertMember(searchChatroom.chatroom.deletedByMember))
            .updatedAt(searchChatroom.updatedAt)
            .isSecret(searchChatroom.chatroom.isSecret)
            .isDisabled(searchChatroom.isDisabled)
            .chatroomImageUrl(searchChatroom.chatroom.chatroomImageUrl)
            .build()
    }

    fun convertSearchConversations(
        conversations: List<SearchConversation>,
        followStatus: Boolean,
        keyword: String
    ): List<SearchConversationViewData> {
        return conversations.map {
            convertSearchConversation(it, followStatus, keyword)
        }
    }

    private fun convertSearchConversation(
        searchConversation: SearchConversation,
        followStatus: Boolean,
        keyword: String
    ): SearchConversationViewData {
        val updatedAnswer = ChatroomUtil.removeTemporaryText(searchConversation.answer)
        return SearchConversationViewData.Builder()
            .chatroom(convertChatroom(searchConversation.chatroom))
            .chatroomAnswer(convertConversationForSearch(searchConversation))
            .chatroomName(searchConversation.chatroom.header)
            .senderName(searchConversation.member.name)
            .chatroomAnswerId(searchConversation.id.toString())
            .answer(updatedAnswer)
            .time(TimeUtil.getLastConversationTime(searchConversation.lastUpdated))
            .followStatus(followStatus)
            .keywordMatchedInCommunityName(
                SearchUtils.findMatchedKeywords(
                    keyword,
                    searchConversation.community.name
                )
            )
            .keywordMatchedInChatroomName(
                SearchUtils.findMatchedKeywords(
                    keyword,
                    searchConversation.chatroom.header
                )
            )
            .keywordMatchedInMessageText(
                SearchUtils.findMatchedKeywords(
                    keyword,
                    updatedAnswer,
                    isMessage = true
                )
            )
            .build()
    }

    private fun convertConversationForSearch(
        searchConversation: SearchConversation
    ): ConversationViewData {
        val member = MemberViewData.Builder()
            .id(searchConversation.member.id)
            .name(searchConversation.member.name)
            .roles(searchConversation.member.roles)
            .build()

        return ConversationViewData.Builder()
            .id(searchConversation.id.toString())
            .state(searchConversation.state)
            .attachmentCount(searchConversation.attachmentCount)
            .attachments(
                searchConversation.attachments.mapNotNull { attachment ->
                    convertAttachment(attachment)
                }.let {
                    ArrayList(it)
                }
            )
            .attachmentsUploaded(searchConversation.attachmentsUploaded)
            .isEdited(searchConversation.isEdited)
            .createdAt(searchConversation.createdAt.toString())
            .communityId(searchConversation.community.id)
            .memberViewData(member)
            .createdAt(searchConversation.chatroom.createdAt.toString())
            .answer(ChatroomUtil.removeTemporaryText(searchConversation.answer))
            .date(searchConversation.chatroom.date)
            .deletedBy(searchConversation.chatroom.deletedBy)
            .deletedByMember(convertMember(searchConversation.chatroom.deletedByMember))
            .build()
    }

    fun convertParticipants(participant: Member): MemberViewData {
        return MemberViewData.Builder()
            .dynamicViewType(ITEM_VIEW_PARTICIPANTS)
            .id(participant.id)
            .imageUrl(participant.imageUrl)
            .isGuest(participant.isGuest)
            .name(participant.name)
            .userUniqueId(participant.userUniqueId)
            .customTitle(participant.customTitle)
            .sdkClientInfo(convertSDKClientInfo(participant.sdkClientInfo))
            .roles(participant.roles)
            .build()
    }

    // converts list of [ChatroomNotificationData] to list of [ChatroomNotificationViewData]
    fun convertChatroomNotificationDataList(
        unreadConversations: List<ChatroomNotificationData>
    ): List<ChatroomNotificationViewData> {
        return unreadConversations.map {
            convertChatroomNotificationData(it)
        }
    }

    // converts [ChatroomNotificationData] to [ChatroomNotificationViewData]
    private fun convertChatroomNotificationData(
        unreadConversation: ChatroomNotificationData
    ): ChatroomNotificationViewData {
        return ChatroomNotificationViewData.Builder()
            .communityName(unreadConversation.communityName)
            .chatroomName(unreadConversation.chatroomName)
            .chatroomTitle(unreadConversation.chatroomTitle)
            .chatroomUserName(unreadConversation.chatroomUserName)
            .chatroomUserImage(unreadConversation.chatroomUserImage)
            .chatroomId(unreadConversation.chatroomId)
            .communityImage(unreadConversation.communityImage)
            .communityId(unreadConversation.communityId)
            .route(unreadConversation.route)
            .chatroomUnreadConversationCount(unreadConversation.chatroomUnreadConversationCount)
            .chatroomLastConversation(unreadConversation.chatroomLastConversation)
            .chatroomLastConversationUserName(unreadConversation.chatroomLastConversationUserName)
            .chatroomLastConversationUserImage(unreadConversation.chatroomLastConversationUserImage)
            .routeChild(unreadConversation.routeChild)
            .chatroomLastConversationUserTimestamp(unreadConversation.chatroomLastConversationUserTimestamp)
            .attachments(
                unreadConversation.attachments?.mapNotNull { attachment ->
                    convertAttachment(attachment)
                }?.let {
                    ArrayList(it)
                })
            .build()
    }

    /**
     * convert list of [ReportTag] to list of [ReportTagViewData]
     * @param tags: list of [ReportTag]
     * */
    fun convertReportTag(
        tags: List<ReportTag>
    ): List<ReportTagViewData> {
        return tags.map { tag ->
            ReportTagViewData.Builder()
                .id(tag.id)
                .name(tag.name)
                .isSelected(false)
                .build()
        }
    }

    // converts dm limit response to view data model
    fun convertCheckDMLimit(checkDMLimitResponse: CheckDMLimitResponse): CheckDMLimitViewData {
        return CheckDMLimitViewData.Builder()
            .isRequestDMLimitExceeded(checkDMLimitResponse.isRequestDMLimitExceeded)
            .newRequestDMTimestamp(checkDMLimitResponse.newRequestDMTimestamp)
            .numberInDuration(checkDMLimitResponse.userDMLimit?.numberInDuration)
            .duration(checkDMLimitResponse.userDMLimit?.duration)
            .chatroomId(checkDMLimitResponse.chatroomId)
            .build()
    }

    // converts check dm tab response to view data model
    fun convertCheckDMTabResponse(checkDMTabResponse: CheckDMTabResponse?): CheckDMTabViewData? {
        if (checkDMTabResponse == null) {
            return null
        }
        return CheckDMTabViewData.Builder()
            .hideDMTab(checkDMTabResponse.hideDMTab)
            .hideDMText(checkDMTabResponse.hideDMText)
            .isCM(checkDMTabResponse.isCM)
            .unreadDMCount(checkDMTabResponse.unreadDMCount)
            .build()

    }
}