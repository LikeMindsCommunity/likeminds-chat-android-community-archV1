package com.likeminds.chatmm.dm.view.adapter.databinder

import android.graphics.Typeface
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.*
import android.widget.TextView
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.isVisible
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.util.ChatroomUtil
import com.likeminds.chatmm.databinding.ItemDmChatroomBinding
import com.likeminds.chatmm.dm.view.adapter.DMAdapterListener
import com.likeminds.chatmm.homefeed.model.HomeFeedItemViewData
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.model.STATE_ADMIN
import com.likeminds.chatmm.member.util.MemberImageUtil
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.ViewUtils.fetchColor
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_DIRECT_MESSAGE
import com.likeminds.likemindschat.conversation.model.ConversationState

class DMChatroomViewDataBinder(
    private val dmAdapterListener: DMAdapterListener,
    private val userPreferences: UserPreferences
) : ViewDataBinder<ItemDmChatroomBinding, BaseViewType>() {

    companion object {
        private const val LAST_CONVERSATION_MAX_COUNT = 150
    }

    override val viewType: Int
        get() = ITEM_DIRECT_MESSAGE

    override fun createBinder(parent: ViewGroup): ItemDmChatroomBinding {
        val itemChatBinding = ItemDmChatroomBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        setRootClick(itemChatBinding)
        return itemChatBinding
    }

    override fun bindData(binding: ItemDmChatroomBinding, data: BaseViewType, position: Int) {
        binding.apply {
            val context = root.context
            homeFeedItemViewData = data as HomeFeedItemViewData

            val chatroomWithUser = data.chatroom.chatroomWithUser
            val member = data.chatroom.memberViewData

            val memberToBeShown =
                if (userPreferences.getUUID() == chatroomWithUser?.sdkClientInfo?.uuid) {
                    member
                } else {
                    chatroomWithUser
                }

            tvMemberName.text = memberToBeShown?.name
            tvCommunityName.isVisible = false

            if (memberToBeShown?.state == STATE_ADMIN) {
                ivCustomTitleDot.visibility = View.VISIBLE
                tvCmTag.visibility = View.VISIBLE
                tvCmTag.text = memberToBeShown.customTitle
            } else {
                ivCustomTitleDot.visibility = View.GONE
                tvCmTag.visibility = View.GONE
            }

            MemberImageUtil.setImage(
                memberToBeShown?.imageUrl,
                memberToBeShown?.name,
                memberToBeShown?.id,
                imageView = ivMemberImage,
                showRoundImage = true
            )

            showUnseenCount = data.unseenConversationCount > 0

            memberToBeShown?.let {
                setLastConversation(data, this, it)
            }

            if (data.lastConversation?.state == ConversationState.DM_MEMBER_REMOVED_LEFT.value ||
                data.lastConversation?.state == ConversationState.DM_CM_BECOMES_MEMBER_DISABLE.value
            ) {
                tvMemberName.alpha = 0.2f
                tvCommunityName.setTextColor(context.fetchColor(R.color.lm_chat_brown_grey))
                tvLastConversationPersonName.setTextColor(context.fetchColor(R.color.lm_chat_brown_grey))
                tvLastConversation.setTextColor(context.fetchColor(R.color.lm_chat_brown_grey))
            } else {
                tvMemberName.alpha = 1f
                tvCommunityName.setTextColor(context.fetchColor(R.color.lm_chat_grey))
                tvLastConversationPersonName.setTextColor(context.fetchColor(R.color.lm_chat_grey))
                tvLastConversation.setTextColor(context.fetchColor(R.color.lm_chat_grey))
            }
        }
    }

    /**
     * this fun is used to set last conversation text differently for different scenarios
     * */
    private fun setLastConversation(
        data: HomeFeedItemViewData,
        binding: ItemDmChatroomBinding,
        memberToBeShown: MemberViewData
    ) {
        binding.apply {
            val context = root.context
            val lastConversation = data.lastConversation
            if (lastConversation == null) {
                showDMText(this, memberToBeShown)
                return
            }
            if (lastConversation.state == ConversationState.FIRST_CONVERSATION.value) {
                showDMText(this, memberToBeShown)
                return
            }
            if (lastConversation.state == ConversationState.DM_MEMBER_REMOVED_LEFT.value ||
                lastConversation.state == ConversationState.DM_CM_BECOMES_MEMBER_DISABLE.value ||
                lastConversation.state == ConversationState.DM_MEMBER_BECOME_CM.value ||
                lastConversation.state == ConversationState.DM_CM_BECOMES_MEMBER_ENABLE.value ||
                lastConversation.state == ConversationState.DM_MEMBER_BECOMES_CM_ENABLE.value ||
                lastConversation.state == ConversationState.DM_REQUEST_ACCEPTED.value ||
                lastConversation.state == ConversationState.DM_REQUEST_REJECTED.value
            ) {
                tvLastConversationPersonName.visibility = View.GONE
                tvLastConversation.visibility = View.VISIBLE
                val isDisabled =
                    lastConversation.state == ConversationState.DM_MEMBER_REMOVED_LEFT.value
                            || lastConversation.state == ConversationState.DM_CM_BECOMES_MEMBER_DISABLE.value
                val taggingColor = if (isDisabled) {
                    context.fetchColor(R.color.lm_chat_brown_grey)
                } else {
                    context.fetchColor(R.color.lm_chat_grey)
                }
                MemberTaggingDecoder.decode(
                    tvLastConversation,
                    lastConversation.answer,
                    true,
                    taggingColor
                )
                val linkifyLinks =
                    (Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS)
                LinkifyCompat.addLinks(tvLastConversation, linkifyLinks)
                tvLastConversation.setLinkTextColor(LMChatAppearance.getTextLinkColor())
                tvLastConversation.movementMethod = LinkMovementMethod.getInstance()
                return
            }
            if (lastConversation.deletedBy == null) {
                //If last conversation exists and it is not deleted
                tvLastConversation.setTypeface(null, Typeface.NORMAL)
                tvLastConversationPersonName.visibility = View.VISIBLE
                tvLastConversationPersonName.text = data.lastConversationMemberName

                val spannableStringBuilder = ChatroomUtil.getHomeScreenAttachmentData(
                    context, lastConversation
                ).first
                if (spannableStringBuilder.isNotEmpty()) {
                    textViewAttachment.setText(
                        spannableStringBuilder,
                        TextView.BufferType.SPANNABLE
                    )
                    textViewAttachment.visibility = View.VISIBLE
                } else {
                    textViewAttachment.visibility = View.GONE
                }
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
                MemberTaggingDecoder.decode(
                    tvLastConversation,
                    tvLastConversation.text.toString(),
                    false,
                    LMChatAppearance.getTextLinkColor()
                )
            } else {
                //Last conversation was deleted
                tvLastConversationPersonName.visibility = View.GONE
                textViewAttachment.visibility = View.GONE
                tvLastConversation.setTypeface(null, Typeface.ITALIC)
                // todo test for uuid/memberid
                tvLastConversation.text = ChatroomUtil.getDeletedMessage(
                    context,
                    lastConversation,
                    userPreferences.getUUID()
                )
            }
        }
    }

    private fun showDMText(binding: ItemDmChatroomBinding, memberToBeShown: MemberViewData) {
        binding.apply {
            val context = root.context
            tvLastConversationPersonName.visibility = View.GONE
            tvLastConversation.visibility = View.VISIBLE

            tvLastConversation.text = if (memberToBeShown.state == STATE_ADMIN) {
                context.getString(R.string.lm_chat_direct_message_your_community_manager)
            } else {
                "${context.getString(R.string.lm_chat_start_a_conversation_with)} ${memberToBeShown.name}."
            }
        }
    }

    private fun setRootClick(binding: ItemDmChatroomBinding) {
        binding.root.setOnClickListener {
            val homeFeedItemViewData = binding.homeFeedItemViewData ?: return@setOnClickListener
            dmAdapterListener.dmChatroomClicked(homeFeedItemViewData)
        }
    }
}