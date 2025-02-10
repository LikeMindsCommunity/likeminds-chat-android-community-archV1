package com.likeminds.chatmm.chatroom.detail.view.adapter

import androidx.recyclerview.widget.DiffUtil
import com.likeminds.chatmm.chatroom.detail.model.*
import com.likeminds.chatmm.conversation.model.*
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.polls.model.PollViewData
import com.likeminds.chatmm.reactions.model.ReactionViewData
import com.likeminds.chatmm.utils.model.BaseViewType

internal class ChatroomDetailDiffUtilCallback(
    private val oldList: List<BaseViewType>,
    private val newList: List<BaseViewType>,
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size

    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return when {
            oldItem is ChatroomViewData && newItem is ChatroomViewData -> {
                oldItem.id == newItem.id
            }
            oldItem is ConversationViewData && newItem is ConversationViewData -> {
                oldItem.id == newItem.id || oldItem.id == newItem.temporaryId || oldItem.temporaryId == newItem.id
            }
            oldItem is ChatroomDateViewData && newItem is ChatroomDateViewData -> {
                oldItem.date == newItem.date
            }
            oldItem is ConversationListShimmerViewData && newItem is ConversationListShimmerViewData -> true
            oldItem is AutoFollowedTaggedActionViewData && newItem is AutoFollowedTaggedActionViewData -> true
            oldItem is FollowItemViewData && newItem is FollowItemViewData -> true
            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val oldItem = oldList[oldItemPosition]
        val newItem = newList[newItemPosition]
        return when {
            oldItem is ChatroomViewData && newItem is ChatroomViewData -> {
                chatroom(oldItem, newItem)
            }
            oldItem is ConversationViewData && newItem is ConversationViewData -> {
                conversation(oldItem, newItem)
            }
            oldItem is ChatroomDateViewData && newItem is ChatroomDateViewData -> {
                oldItem.date == newItem.date
            }
            oldItem is ConversationListShimmerViewData && newItem is ConversationListShimmerViewData -> true
            oldItem is AutoFollowedTaggedActionViewData && newItem is AutoFollowedTaggedActionViewData -> true
            oldItem is FollowItemViewData && newItem is FollowItemViewData -> true
            else -> false
        }
    }

    private fun conversation(
        oldItem: ConversationViewData?,
        newItem: ConversationViewData?,
    ): Boolean {
        if (oldItem == null && newItem == null) {
            return true
        } else if (oldItem == null || newItem == null) {
            return false
        }
        return oldItem.id == newItem.id
                && oldItem.temporaryId == newItem.temporaryId
                && oldItem.createdAt == newItem.createdAt
                && oldItem.createdEpoch == newItem.createdEpoch
                && oldItem.answer == newItem.answer
                && attachments(oldItem.attachments, newItem.attachments)
                && oldItem.lastSeen == newItem.lastSeen
                && oldItem.ogTags?.title == newItem.ogTags?.title
                && conversation(oldItem.replyConversation, newItem.replyConversation)
                && oldItem.isEdited == newItem.isEdited
                && oldItem.deletedBy == newItem.deletedBy
                && oldItem.attachmentCount == newItem.attachmentCount
                && oldItem.attachmentsUploaded == newItem.attachmentsUploaded
                && oldItem.workerUUID == newItem.workerUUID
                && oldItem.isLastItem == newItem.isLastItem
                && reactions(oldItem.reactions, newItem.reactions)
                && oldItem.replyChatroomId == newItem.replyChatroomId
    }

    private fun reactions(
        oldItem: List<ReactionViewData>?,
        newItem: List<ReactionViewData>?,
    ): Boolean {
        if (oldItem.isNullOrEmpty() && newItem.isNullOrEmpty()) {
            return true
        }
        if (oldItem?.size != newItem?.size) {
            return false
        }
        for (i in oldItem!!.indices) {
            if (!reaction(oldItem[i], newItem!![i])) {
                return false
            }
        }
        return true
    }

    private fun reaction(
        oldItem: ReactionViewData,
        newItem: ReactionViewData,
    ): Boolean {
        return oldItem.reaction == newItem.reaction
                && member(oldItem.memberViewData, newItem.memberViewData)
    }

    private fun attachments(
        oldItem: List<AttachmentViewData>?,
        newItem: List<AttachmentViewData>?,
    ): Boolean {
        if (oldItem.isNullOrEmpty() && newItem.isNullOrEmpty()) {
            return true
        }
        if (oldItem?.size != newItem?.size) {
            return false
        }
        for (i in oldItem!!.indices) {
            if (!attachment(oldItem[i], newItem!![i])) {
                return false
            }
        }
        return true
    }

    private fun attachment(
        oldItem: AttachmentViewData,
        newItem: AttachmentViewData,
    ): Boolean {
        return oldItem.uri == newItem.uri
                && oldItem.type == newItem.type
                && oldItem.index == newItem.index
                && oldItem.width == newItem.width
                && oldItem.height == newItem.height
                && oldItem.mediaLeft == newItem.mediaLeft
                && oldItem.title == newItem.title
                && oldItem.subTitle == newItem.subTitle
                && oldItem.parentViewItemPosition == newItem.parentViewItemPosition
                && oldItem.awsFolderPath == newItem.awsFolderPath
                && oldItem.localFilePath == newItem.localFilePath
                && oldItem.thumbnail == newItem.thumbnail
                && oldItem.thumbnailAWSFolderPath == newItem.thumbnailAWSFolderPath
                && oldItem.thumbnailLocalFilePath == newItem.thumbnailLocalFilePath
    }

    private fun chatroom(oldItem: ChatroomViewData?, newItem: ChatroomViewData?): Boolean {
        return if (oldItem == null && newItem == null) {
            true
        } else if (oldItem == null || newItem == null) {
            false
        } else {
            return member(oldItem.memberViewData, newItem.memberViewData)
                    && oldItem.id == newItem.id
                    && oldItem.communityId == newItem.communityId
                    && oldItem.communityName == newItem.communityName
                    && oldItem.title == newItem.title
                    && oldItem.createdAt == newItem.createdAt
                    && oldItem.answerText == newItem.answerText
                    && oldItem.state == newItem.state
                    && oldItem.shareUrl == newItem.shareUrl
                    && oldItem.type == newItem.type
                    && oldItem.date == newItem.date
                    && oldItem.header == newItem.header
                    && oldItem.cardCreationTime == newItem.cardCreationTime
                    && oldItem.totalResponseCount == newItem.totalResponseCount
                    && oldItem.deletedBy == newItem.deletedBy
                    && oldItem.about == newItem.about
                    && oldItem.isSecret == newItem.isSecret
                    && compareIntegerList(
                oldItem.secretChatroomParticipants,
                newItem.secretChatroomParticipants
            )
                    && oldItem.secretChatroomLeft == newItem.secretChatroomLeft
                    && oldItem.isEdited == newItem.isEdited
        }
    }

    private fun memberList(
        oldItem: List<MemberViewData>?,
        newItem: List<MemberViewData>?,
    ): Boolean {
        if (oldItem.isNullOrEmpty() && newItem.isNullOrEmpty()) {
            return true
        }
        if (oldItem?.size != newItem?.size) {
            return false
        }
        for (i in oldItem!!.indices) {
            if (!member(oldItem[i], newItem!![i])) {
                return false
            }
        }
        return true
    }

    private fun member(
        oldItem: MemberViewData?,
        newItem: MemberViewData?,
    ): Boolean {
        return oldItem?.id == newItem?.id
                && oldItem?.name == newItem?.name
                && oldItem?.imageUrl == newItem?.imageUrl
    }

    private fun pollViewDataList(
        oldItem: List<PollViewData>?,
        newItem: List<PollViewData>?,
    ): Boolean {
        if (oldItem.isNullOrEmpty() && newItem.isNullOrEmpty()) {
            return true
        }
        if (oldItem?.size != newItem?.size) {
            return false
        }
        for (i in oldItem!!.indices) {
            if (!pollViewData(oldItem[i], newItem!![i])) {
                return false
            }
        }
        return true
    }

    private fun pollViewData(oldItem: PollViewData, newItem: PollViewData): Boolean {
        return oldItem.id == newItem.id
                && oldItem.text == newItem.text
                && oldItem.isSelected == newItem.isSelected
                && oldItem.percentage == newItem.percentage
                && oldItem.noVotes == newItem.noVotes
                && member(oldItem.member, newItem.member)
    }

    private fun linkOGTags(
        oldItem: LinkOGTagsViewData?,
        newItem: LinkOGTagsViewData?,
    ): Boolean {
        return oldItem?.title == newItem?.title && oldItem?.image == newItem?.image
                && oldItem?.description == newItem?.description
                && oldItem?.url == newItem?.url
    }

    private fun compareIntegerList(oldItem: List<Int>?, newItem: List<Int>?): Boolean {
        if (oldItem.isNullOrEmpty() && newItem.isNullOrEmpty()) {
            return true
        }
        if (oldItem?.size != newItem?.size) {
            return false
        }
        for (i in oldItem!!.indices) {
            if (oldItem != newItem) {
                return false
            }
        }
        return true
    }

}
