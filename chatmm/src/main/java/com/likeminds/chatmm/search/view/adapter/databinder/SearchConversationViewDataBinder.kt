package com.likeminds.chatmm.search.view.adapter.databinder

import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.likeminds.chatmm.R
import com.likeminds.chatmm.chatroom.detail.util.ChatroomUtil
import com.likeminds.chatmm.databinding.ItemSearchConversationBinding
import com.likeminds.chatmm.member.util.MemberUtil
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.search.model.SearchConversationViewData
import com.likeminds.chatmm.search.util.SearchUtils
import com.likeminds.chatmm.search.view.adapter.SearchAdapterListener
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.membertagging.MemberTaggingDecoder
import com.likeminds.chatmm.utils.model.ITEM_SEARCH_MESSAGE

class SearchConversationViewDataBinder(
    private val listener: SearchAdapterListener,
    private var userPreferences: UserPreferences
) : ViewDataBinder<ItemSearchConversationBinding, SearchConversationViewData>() {

    override val viewType: Int
        get() = ITEM_SEARCH_MESSAGE

    override fun createBinder(parent: ViewGroup): ItemSearchConversationBinding {
        val binding = ItemSearchConversationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        setListeners(binding)
        return binding
    }

    override fun bindData(
        binding: ItemSearchConversationBinding,
        data: SearchConversationViewData,
        position: Int
    ) {
        binding.apply {
            val context = root.context

            searchedConversationViewData = data
            hideBottomLine = data.isLast
            hideNotFollowed = data.followStatus == true

            val chatroomViewData = data.chatroom

            //highlight keyword matched in chatroom name
            tvChatroomName.text =
                if (!data.keywordMatchedInChatroomName.isNullOrEmpty()) {
                    data.chatroom?.header?.let {
                        SearchUtils.getHighlightedText(
                            it,
                            data.keywordMatchedInChatroomName,
                            ContextCompat.getColor(context, R.color.lm_chat_black)
                        )
                    }
                } else {
                    data.chatroom?.header
                }

            val chatTypeDrawable =
                ChatroomUtil.getTypeDrawable(context, chatroomViewData?.type)
            if (chatTypeDrawable == null) {
                ivChatroomType.hide()
            } else {
                ivChatroomType.show()
                ivChatroomType.setImageDrawable(chatTypeDrawable)
            }

            val updatedAnswer = data.answer.replace("\n", " ")

            val senderName = MemberUtil.getFirstNameToShow(
                userPreferences,
                data.chatroomAnswer.memberViewData
            )

            //decode member tagging
            val answerWithNonHighlightedTags = MemberTaggingDecoder.decode(updatedAnswer)

            val tvConversationText = SpannableStringBuilder("$senderName ")

            tvConversationText.append(
                ChatroomUtil.getHomeScreenAttachmentData(
                    context,
                    data.chatroomAnswer
                ).first
            )

            //trimming of the text to  be shown
            if ((data.keywordMatchedInMessageText?.size ?: 0) > 0) {
                tvConversationText.append(data.keywordMatchedInMessageText?.let {
                    SearchUtils.getTrimmedText(
                        answerWithNonHighlightedTags,
                        it,
                        ContextCompat.getColor(context, R.color.lm_chat_black)
                    )
                })
            } else {
                tvConversationText.append(answerWithNonHighlightedTags)
            }

            tvConversation.setText(tvConversationText, TextView.BufferType.SPANNABLE)
            tvTimestamp.text = data.time
        }
    }

    private fun setListeners(binding: ItemSearchConversationBinding) {
        binding.root.setOnClickListener {
            val messageViewData = binding.searchedConversationViewData ?: return@setOnClickListener
            listener.onMessageClicked(messageViewData)
        }
    }
}