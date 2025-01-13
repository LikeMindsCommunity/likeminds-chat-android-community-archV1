package com.likeminds.chatmm.chatroom.explore.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.explore.model.ExploreViewData
import com.likeminds.chatmm.chatroom.explore.view.adapter.ChatroomExploreAdapterListener
import com.likeminds.chatmm.databinding.ItemChatroomExploreBinding
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.ITEM_EXPLORE

internal class ChatroomExploreViewDataBinder(
    private val listener: ChatroomExploreAdapterListener,
) : ViewDataBinder<ItemChatroomExploreBinding, ExploreViewData>() {

    override val viewType: Int
        get() = ITEM_EXPLORE

    override fun createBinder(parent: ViewGroup): ItemChatroomExploreBinding {
        val binding = ItemChatroomExploreBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        binding.apply {
            clRoot.setOnClickListener {
                val data = this.data ?: return@setOnClickListener
                val position: Int = this.position ?: return@setOnClickListener
                listener.onChatroomClick(data, position)
            }
            btnJoin.setOnClickListener {
                onJoinClick(this)
            }
            btnJoined.setOnClickListener {
                onJoinClick(this)
            }
        }
        return binding
    }

    override fun bindData(
        binding: ItemChatroomExploreBinding,
        data: ExploreViewData,
        position: Int
    ) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
            this.data = data
            this.position = position

            ViewUtils.setChatroomImage(
                data.id,
                data.header,
                data.chatroomImageUrl,
                ivChatRoom
            )

            //for pinned icon
            ivPinned.isVisible = data.isPinned == true

            //for new icon badge
            tvNew.isVisible = data.externalSeen == false

            //for chatroom header
            tvHeader.isVisible = data.header.isNotEmpty()
            tvHeader.text = data.header

            //for participants count
            val participantsCount = data.participantsCount
            tvParticipant.text = participantsCount.toString()

            // for conversation count
            val totalResponseCount = data.totalResponseCount
            tvResponses.text = totalResponseCount.toString()

            //for title
            tvTitle.isVisible = !data.title.isNullOrEmpty()
            tvTitle.text = data.title

            //for secret chatroom
            if (data.isSecret) {
                btnJoin.isVisible = false
                btnJoined.isVisible = false
                ivSecret.isVisible = true
            } else {
                ivSecret.isVisible = false
                if (data.followStatus) {
                    btnJoin.isVisible = false
                    btnJoined.isVisible = true
                } else {
                    btnJoin.isVisible = true
                    btnJoined.isVisible = false
                }
            }
        }
    }

    // processes user request on join button click
    private fun onJoinClick(binding: ItemChatroomExploreBinding) {
        binding.apply {
            val data = this.data ?: return
            val position: Int = this.position ?: return
            if (data.followStatus) {
                listener.onJoinClick(false, position, data)
            } else {
                listener.onJoinClick(true, position, data)
            }
        }
    }
}