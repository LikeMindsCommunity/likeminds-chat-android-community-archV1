package com.likeminds.chatmm.homefeed.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.databinding.ItemChannelInviteBinding
import com.likeminds.chatmm.homefeed.model.ChannelInviteViewData
import com.likeminds.chatmm.homefeed.view.adapter.HomeFeedAdapterListener
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.ITEM_CHANNEL_INVITE

class ChannelInviteItemViewDataBinder(
    private val homeAdapterListener: HomeFeedAdapterListener
) : ViewDataBinder<ItemChannelInviteBinding, ChannelInviteViewData>() {

    override val viewType: Int
        get() = ITEM_CHANNEL_INVITE

    override fun createBinder(parent: ViewGroup): ItemChannelInviteBinding {
        val itemChannelInviteBinding = ItemChannelInviteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        setClickListeners(itemChannelInviteBinding)
        return itemChannelInviteBinding
    }

    override fun bindData(
        binding: ItemChannelInviteBinding,
        data: ChannelInviteViewData,
        position: Int
    ) {
        binding.apply {
            val context = root.context

            // set data in binding
            buttonColor = LMChatAppearance.getButtonsColor()
            channelInviteViewData = data
            this.position = position

            val invitedChatroom = data.invitedChatroom

            // set chatroom image
            ViewUtils.setChatroomImage(
                invitedChatroom.id,
                invitedChatroom.header,
                invitedChatroom.chatroomImageUrl,
                ivChatRoom
            )

            val inviteSender = data.inviteSender
            tvLastConversation.text =
                context.getString(R.string.lm_chat_s_invited_you_to_join, inviteSender.name)
        }
    }

    //sets all the required click listeners to the view
    private fun setClickListeners(binding: ItemChannelInviteBinding) {
        binding.apply {
            ivChannelInviteAccept.setOnClickListener {
                val data = this.channelInviteViewData ?: return@setOnClickListener
                val position: Int = this.position ?: return@setOnClickListener
                homeAdapterListener.onAcceptChannelInviteClicked(position, data)
            }

            ivChannelInviteReject.setOnClickListener {
                val data = this.channelInviteViewData ?: return@setOnClickListener
                val position: Int = this.position ?: return@setOnClickListener
                homeAdapterListener.onRejectChannelInviteClicked(position, data)
            }
        }
    }
}