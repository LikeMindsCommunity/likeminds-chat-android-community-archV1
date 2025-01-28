package com.likeminds.chatmm.chatroom.create.view.adapter

import android.content.Context
import com.likeminds.chatmm.chatroom.create.view.adapter.databinder.*
import com.likeminds.chatmm.chatroom.detail.model.ChatroomViewData
import com.likeminds.chatmm.conversation.model.AttachmentViewData
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemCreatePollBinding
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.polls.adapter.databinders.ItemCreatePollViewDataBinder
import com.likeminds.chatmm.polls.adapter.databinders.ItemPollMoreOptionsViewDataBinder
import com.likeminds.chatmm.polls.model.*
import com.likeminds.chatmm.polls.view.ItemPollViewDataBinder
import com.likeminds.chatmm.utils.customview.BaseRecyclerAdapter
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType

class ChatroomItemAdapter(
    val userPreferences: UserPreferences,
    val chatroomItemAdapterListener: ChatroomItemAdapterListener? = null,
    val createPollItemAdapterListener: CreatePollItemAdapterListener? = null,
    val pollItemAdapterListener: PollItemAdapterListener? = null,
) : BaseRecyclerAdapter<BaseViewType>() {

    init {
        initViewDataBinders()
    }

    override fun getSupportedViewDataBinder(): MutableList<ViewDataBinder<*, *>> {
        val viewDataBinders = ArrayList<ViewDataBinder<*, *>>(20)

        val createPollViewDataBinder =
            ItemCreatePollViewDataBinder(createPollItemAdapterListener)
        viewDataBinders.add(createPollViewDataBinder)

        val pollMoreOptionsViewDataBinder = ItemPollMoreOptionsViewDataBinder()
        viewDataBinders.add(pollMoreOptionsViewDataBinder)

        val pollViewDataBinder = ItemPollViewDataBinder(
            userPreferences,
            createPollItemAdapterListener,
            pollItemAdapterListener,
            chatroomItemAdapterListener,
        )
        viewDataBinders.add(pollViewDataBinder)

        val imageItemViewDataBinder = ItemImageViewDataBinder(chatroomItemAdapterListener)
        viewDataBinders.add(imageItemViewDataBinder)

        val videoViewDataBinder = ItemVideoViewDataBinder(chatroomItemAdapterListener)
        viewDataBinders.add(videoViewDataBinder)

        val itemImageExpandedViewDataBinder = ItemImageExpandedViewDataBinder()
        viewDataBinders.add(itemImageExpandedViewDataBinder)

        val itemVideoExpandedViewDataBinder = ItemVideoExpandedViewDataBinder()
        viewDataBinders.add(itemVideoExpandedViewDataBinder)

        val documentItemViewDataBinder = ItemDocumentViewDataBinder(chatroomItemAdapterListener)
        viewDataBinders.add(documentItemViewDataBinder)

        val audioItemViewDataBinder = ItemAudioViewDataBinder(chatroomItemAdapterListener)
        viewDataBinders.add(audioItemViewDataBinder)

        return viewDataBinders
    }
}

interface CreatePollItemAdapterListener {
    fun pollCrossed(createPollViewData: CreatePollViewData) {}
    fun addPollItemBinding(
        position: Int,
        itemCreatePollBinding: ItemCreatePollBinding
    ) {
    }

    fun pollSelected(context: Context, pollViewData: PollViewData) {}
}

interface PollItemAdapterListener {
    fun isPollSubmitted(): Boolean
    fun isPollSelected(): Boolean
    fun showVotersList(
        pollId: String?,
        parentId: String?,
        pollInfoData: PollInfoData?,
        positionOfPoll: Int,
    )
}

interface ChatroomItemAdapterListener {
    fun onLongPressChatRoom(chatRoom: ChatroomViewData, itemPosition: Int) {}
    fun onLongPressConversation(conversation: ConversationViewData, itemPosition: Int) {}

    fun isSelectionEnabled(): Boolean {
        return false
    }

    fun isMediaActionVisible(): Boolean {
        return false
    }

    fun isMediaUploadFailed(): Boolean {
        return false
    }

    fun showMemberProfile(memberViewData: MemberViewData) {}

    fun showMembers(screenType: Int) {}

    fun onAudioConversationActionClicked(
        data: AttachmentViewData,
        position: String,
        childPosition: Int,
        progress: Int,
    ) {
    }

    fun onConversationSeekBarChanged(
        progress: Int,
        attachmentViewData: AttachmentViewData,
        parentConversationId: String,
        childPosition: Int,
    ) {
    }

    fun onSeekBarFocussed(value: Boolean) {}

    fun onScreenChanged() {}
}