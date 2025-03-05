package com.likeminds.chatmm.conversation.view.adapter.databinder

import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.util.Linkify
import android.view.*
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.text.util.LinkifyCompat
import androidx.databinding.DataBindingUtil
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.model.TYPE_DIRECT_MESSAGE
import com.likeminds.chatmm.chatroom.detail.view.adapter.ChatroomDetailAdapterListener
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.databinding.ItemConversationActionBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.chatmm.utils.membertagging.util.MemberTaggingClickableSpan
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_CONVERSATION_ACTION
import com.likeminds.likemindschat.conversation.model.ConversationState
import com.likeminds.likemindschat.user.model.MemberBlockState

class ConversationActionItemViewDataBinder(
    private val userPreferences: UserPreferences,
    private val chatroomDetailAdapterListener: ChatroomDetailAdapterListener?,
) : ViewDataBinder<ItemConversationActionBinding, BaseViewType>() {

    override val viewType: Int
        get() = ITEM_CONVERSATION_ACTION

    override fun createBinder(parent: ViewGroup): ItemConversationActionBinding {
        val inflater = LayoutInflater.from(parent.context)
        return DataBindingUtil.inflate(
            inflater, R.layout.item_conversation_action, parent, false
        )
    }

    override fun bindData(
        binding: ItemConversationActionBinding,
        data: BaseViewType,
        position: Int,
    ) {
        binding.conversation = data as ConversationViewData
        initActionTextView(
            binding.tvAction,
            data,
            position
        )
    }

    private fun initActionTextView(
        tvAction: TextView,
        conversation: ConversationViewData,
        position: Int
    ) {
        tvAction.apply {
            MemberTaggingDecoder.decode(
                tvAction,
                conversation.answer,
                true,
                ContextCompat.getColor(
                    tvAction.context,
                    R.color.lm_chat_white
                )
            ) { _ ->
                // todo: onMemberClick()
            }

            val editable = tvAction.editableText
            val spans = tvAction.editableText.getSpans(
                0,
                tvAction.editableText.length,
                MemberTaggingClickableSpan::class.java
            )
            val loggedInMemberUUID = userPreferences.getUUID()
            val loggedInMemberId = userPreferences.getMemberId()

            spans.reversed().forEach { span ->
                if (conversation.state != ConversationState.DM_MEMBER_REMOVED_LEFT.value ||
                    conversation.state != ConversationState.DM_CM_BECOMES_MEMBER_DISABLE.value ||
                    conversation.state != ConversationState.DM_MEMBER_BECOME_CM.value ||
                    conversation.state != ConversationState.DM_CM_BECOMES_MEMBER_ENABLE.value ||
                    conversation.state != ConversationState.DM_MEMBER_BECOMES_CM_ENABLE.value
                ) {
                    if ((span.getMemberUUID() == loggedInMemberUUID) || (span.getMemberUUID() == loggedInMemberId)) {
                        val startIndex = editable.getSpanStart(span)
                        val endIndex = editable.getSpanEnd(span)
                        if (chatroomDetailAdapterListener?.getChatRoomType() == TYPE_DIRECT_MESSAGE && conversation.state == ConversationState.FIRST_CONVERSATION.value) {
                            editable.replace(startIndex, endIndex, "")
                        } else {
                            editable.replace(startIndex, endIndex, "You")
                        }
                    }
                }
            }

            val tapToUndoString = tvAction.context.getString(R.string.lm_chat_tap_to_undo)

            if (conversation.showTapToUndo) {
                if (!editable.contains(tapToUndoString)) {
                    val tapToUndoSpannable =
                        SpannableString(tapToUndoString)
                    val tapToUndoSpan = object : ClickableSpan() {
                        override fun onClick(widget: View) {
                            chatroomDetailAdapterListener?.blockMember(
                                position,
                                MemberBlockState.MEMBER_UNBLOCKED
                            )
                        }
                    }
                    tapToUndoSpannable.setSpan(
                        tapToUndoSpan,
                        0,
                        tapToUndoString.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    editable.append(tapToUndoSpannable)
                }
            } else {
                if (editable.contains(tapToUndoString)) {
                    editable.removeSuffix(tapToUndoString)
                }
            }

            val linkifyLinks =
                (Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS)
            LinkifyCompat.addLinks(tvAction, linkifyLinks)
            tvAction.setLinkTextColor(
                ContextCompat.getColor(
                    tvAction.context,
                    R.color.lm_chat_white
                )
            )
            tvAction.movementMethod = LinkMovementMethod.getInstance()
        }
    }
}