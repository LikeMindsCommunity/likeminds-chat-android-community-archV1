package com.likeminds.chatmm.homefeed.view.adapter.databinder

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.databinding.ItemHomeFeedExploreBinding
import com.likeminds.chatmm.homefeed.model.HomeFeedExploreViewData
import com.likeminds.chatmm.homefeed.view.adapter.HomeFeedAdapterListener
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.ITEM_HOME_FEED_EXPLORE
import javax.inject.Inject

class HomeFeedExploreViewDataBinder @Inject constructor(
    private val homeAdapterListener: HomeFeedAdapterListener?
) : ViewDataBinder<ItemHomeFeedExploreBinding, HomeFeedExploreViewData>() {

    override val viewType: Int
        get() = ITEM_HOME_FEED_EXPLORE

    override fun createBinder(parent: ViewGroup): ItemHomeFeedExploreBinding {
        val binding = ItemHomeFeedExploreBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        setRootClick(binding)
        return binding
    }

    private fun setRootClick(binding: ItemHomeFeedExploreBinding) {
        binding.root.setOnClickListener {
            homeAdapterListener?.homeFeedClicked()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun bindData(
        binding: ItemHomeFeedExploreBinding,
        data: HomeFeedExploreViewData,
        position: Int
    ) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
            this.data = data

            val context = this.root.context
            if (data.newChatRooms > 0) {
                tvUnreadChatroom.isVisible = true
                if (data.newChatRooms > 99) {
                    tvUnreadChatroom.text = context.getString(R.string.lm_chat_ninety_nine_plus_new)
                } else {
                    tvUnreadChatroom.text =
                        context.getString(R.string.lm_chat_new_chatroom_count, data.newChatRooms)
                }
            } else if (data.totalChatRooms > 0) {
                tvUnreadChatroom.isVisible = true
                if (data.totalChatRooms > 99) {
                    tvUnreadChatroom.text = context.getString(R.string.lm_chat_ninety_nine_plus_chatrooms)
                } else {
                    tvUnreadChatroom.text = context.resources.getQuantityString(
                        R.plurals.lm_chat_chatrooms_count,
                        data.totalChatRooms,
                        data.totalChatRooms
                    )
                }
            } else {
                tvUnreadChatroom.isVisible = false
            }
        }
    }
}