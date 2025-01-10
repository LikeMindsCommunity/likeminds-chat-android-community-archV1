package com.likeminds.chatmm.homefeed.view.adapter.databinder

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.view.*
import android.widget.TextView
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.detail.util.ChatroomUtil
import com.likeminds.chatmm.databinding.ItemFollowedChatRoomBinding
import com.likeminds.chatmm.homefeed.model.HomeFeedItemViewData
import com.likeminds.chatmm.homefeed.view.adapter.HomeFeedAdapterListener
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.chatmm.utils.model.ITEM_HOME_CHAT_ROOM

class FollowedChatroomViewDataBinder(
    private val userPreferences: UserPreferences,
    private val homeAdapterListener: HomeFeedAdapterListener
) : ViewDataBinder<ItemFollowedChatRoomBinding, HomeFeedItemViewData>() {

    companion object {
        private const val LAST_CONVERSATION_MAX_COUNT = 250
        private const val MAX_UNSEEN_CONVERSATION = 99
    }

    override val viewType: Int
        get() = ITEM_HOME_CHAT_ROOM

    override fun createBinder(parent: ViewGroup): ItemFollowedChatRoomBinding {
        val itemChatBinding = ItemFollowedChatRoomBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        setRootClick(itemChatBinding)
        return itemChatBinding
    }

    private fun setRootClick(binding: ItemFollowedChatRoomBinding) {
        binding.root.setOnClickListener {
            val homeFeedItemViewData = binding.homeFeedItemViewData ?: return@setOnClickListener
            homeAdapterListener.onChatRoomClicked(homeFeedItemViewData)
        }
    }

    override fun bindData(
        binding: ItemFollowedChatRoomBinding,
        data: HomeFeedItemViewData,
        position: Int
    ) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
            homeFeedItemViewData = data
            hideBottomLine = data.isLastItem
            showUnseenCount = data.unseenConversationCount > 0

            ViewUtils.setChatroomImage(
                data.chatroom.id,
                data.chatroom.header,
                data.chatroomImageUrl,
                ivChatRoom
            )

            //Show unseen count
            tvUnseenCount.text =
                if (data.unseenConversationCount > MAX_UNSEEN_CONVERSATION) {
                    root.context.getString(R.string.lm_chat_max_two_digit_number)
                } else {
                    data.unseenConversationCount.toString()
                }

            //Set the chatroom type icon
            if (data.chatTypeDrawableId != null) {
                tvChatroomName.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    data.chatTypeDrawableId,
                    0
                )
                tvChatroomName.compoundDrawables.forEach {
                    it?.setTintList(ColorStateList.valueOf(LMChatAppearance.getButtonsColor()))
                }
            } else {
                tvChatroomName.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    0,
                    0
                )
                tvChatroomName.compoundDrawables.forEach {
                    it?.setTintList(ColorStateList.valueOf(LMChatAppearance.getButtonsColor()))
                }
            }

            //secret chatroom lock icon
            val isSecretChatroom = data.chatroom.isSecret

            if (isSecretChatroom == true) {
                ivSecretChatroom.show()
            } else {
                ivSecretChatroom.hide()
            }

            val lastConversation = data.lastConversation
            if (lastConversation?.deletedBy == null) {
                if (lastConversation != null) {
                    //If last conversation exists and it is not deleted
                    tvLastConversation.setTypeface(
                        tvLastConversation.typeface,
                        Typeface.NORMAL
                    )
                    tvLastConversationMemberName.visibility = View.VISIBLE
                    tvLastConversationMemberName.text =
                        data.lastConversationMemberName

                    // processes and shows data of last conversation for the chatroom in home feed
                    val spannableStringBuilder =
                        ChatroomUtil.getHomeScreenAttachmentData(
                            root.context,
                            lastConversation
                        ).first
                    if (spannableStringBuilder.isNotEmpty()) {
                        tvLastConversationAttachment.setText(
                            spannableStringBuilder,
                            TextView.BufferType.SPANNABLE
                        )
                        tvLastConversationAttachment.visibility = View.VISIBLE
                    } else {
                        tvLastConversationAttachment.visibility = View.GONE
                    }

                    // trims and sets the [lastConversationAnswer] as per the [LAST_CONVERSATION_MAX_COUNT]
                    val lastConversationAnswer = data.lastConversationText
                    if (lastConversationAnswer != null) {
                        tvLastConversation.text = if (
                            lastConversationAnswer.length > LAST_CONVERSATION_MAX_COUNT
                        ) {
                            lastConversationAnswer.substring(
                                0,
                                LAST_CONVERSATION_MAX_COUNT
                            )
                        } else {
                            lastConversationAnswer
                        }
                    }
                } else {
                    //No name and attachment to show here
                    tvLastConversationMemberName.visibility = View.GONE
                    tvLastConversationAttachment.visibility = View.GONE

                    tvLastConversation.setTypeface(
                        tvLastConversation.typeface,
                        Typeface.NORMAL
                    )
                    val chatroomTitle = data.chatroom.title
                    if (chatroomTitle.isNotBlank()) {
                        tvLastConversation.text = chatroomTitle
                    }
                }
                MemberTaggingDecoder.decode(
                    tvLastConversation,
                    tvLastConversation.text.toString(),
                    false,
                    LMChatAppearance.getTextLinkColor()
                )
            } else {
                //Last conversation was deleted
                tvLastConversationMemberName.visibility = View.GONE
                tvLastConversationAttachment.visibility = View.GONE
                tvLastConversation.setTypeface(
                    tvLastConversation.typeface,
                    Typeface.ITALIC
                )
                tvLastConversation.text = ChatroomUtil.getDeletedMessage(
                    root.context,
                    lastConversation,
                    userPreferences.getUUID()
                )
            }
        }
    }
}