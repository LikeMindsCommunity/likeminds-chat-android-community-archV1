package com.likeminds.chatmm.polls.view

import android.graphics.drawable.*
import android.view.*
import androidx.core.content.ContextCompat
import com.likeminds.chatmm.R
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.chatroom.create.view.adapter.*
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil.hasPollEnded
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil.isAddNewOptionEnabled
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil.isInstantPoll
import com.likeminds.chatmm.chatroom.detail.util.ChatroomConversationItemViewDataBinderUtil.isMultipleItemPoll
import com.likeminds.chatmm.databinding.ItemPollBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.polls.model.PollViewData
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.ITEM_POLL

class ItemPollViewDataBinder constructor(
    private val userPreferences: UserPreferences,
    private val createItemAdapterListener: CreatePollItemAdapterListener?,
    private val pollItemAdapterListener: PollItemAdapterListener?,
    private val chatroomItemAdapterListener: ChatroomItemAdapterListener?
) : ViewDataBinder<ItemPollBinding, PollViewData>() {

    override val viewType: Int
        get() = ITEM_POLL

    override fun createBinder(parent: ViewGroup): ItemPollBinding {
        val binding = ItemPollBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )

        setNoVotesClickListener(binding)
        return binding
    }

    override fun bindData(
        binding: ItemPollBinding,
        data: PollViewData,
        position: Int
    ) {
        binding.apply {
            buttonColor = LMChatAppearance.getButtonsColor()
            pollViewData = data
            this.position = position
            val context = root.context
            val isPollSelectedByUser = pollItemAdapterListener?.isPollSelected() ?: false
            val isMultipleItemPoll = data.pollInfoData.isMultipleItemPoll()
            val isPollSubmitted = pollItemAdapterListener?.isPollSubmitted() ?: false
            val isInstantPoll = data.pollInfoData.isInstantPoll()
            val hasPollEnded = data.pollInfoData.hasPollEnded()
            val toShowResults = data.toShowResults

            initPollCheckView(
                this,
                data,
                isMultipleItemPoll,
                isInstantPoll
            )
            root.setOnClickListener {
                if (!checkForSelection(this, checkForSelectionEnabled = true)) {
                    if (data.pollInfoData.hasPollEnded()) {
                        ViewUtils.showShortToast(
                            context,
                            context.getString(R.string.lm_chat_poll_ended_message_for_option_selection)
                        )
                        return@setOnClickListener
                    }
                    createItemAdapterListener?.pollSelected(root.context, data)
                }
            }

            if (toShowResults != null) {
                if (toShowResults == true) {
                    initPollNoVotes(this, data)
                } else {
                    tvNoVotes.hide()
                }
            } else {
                if (isPollSelectedByUser && isPollSubmitted && isInstantPoll) {
                    initPollNoVotes(this, data)
                } else {
                    tvNoVotes.hide()
                }
            }

            initPollBackground(
                this,
                data,
                isPollSelectedByUser,
                isMultipleItemPoll,
                hasPollEnded
            )
            initPollBackgroundProgress(
                this,
                data,
                isPollSelectedByUser,
                isPollSubmitted,
                isInstantPoll,
                hasPollEnded,
                toShowResults
            )
            //initPollImage(binding, data)
            initAddedByText(this, data)
        }
    }

    private fun initAddedByText(binding: ItemPollBinding, data: PollViewData) {
        if (data.pollInfoData.isAddNewOptionEnabled()) {
            binding.tvAddedBy.text =
                if (userPreferences.getUUID() == data.member?.id) {
                    "Added by You"
                } else {
                    String.format("Added by %s", data.member?.name)
                }
            binding.tvAddedBy.visibility = View.VISIBLE
        } else {
            binding.tvAddedBy.visibility = View.GONE
        }
    }

    private fun initPollCheckView(
        binding: ItemPollBinding,
        data: PollViewData,
        multipleItemPoll: Boolean,
        instantPoll: Boolean
    ) {
        if ((multipleItemPoll || !instantPoll) && data.isSelected == true) {
            binding.ivChecked.visibility = View.VISIBLE
        } else {
            binding.ivChecked.visibility = View.GONE
        }
    }

    private fun initPollNoVotes(binding: ItemPollBinding, data: PollViewData) {
        binding.apply {
            val noVotes = data.noVotes ?: 0
            val context = binding.root.context
            tvNoVotes.text = if (noVotes > 0)
                context.resources.getQuantityString(
                    R.plurals.lm_chat_votes,
                    noVotes,
                    noVotes
                )
            else {
                context.getString(R.string.lm_chat_zero_vote)
            }
            tvNoVotes.visibility = View.VISIBLE
            if (data.isSelected == true) {
                tvNoVotes.setTextColor(LMChatAppearance.getButtonsColor())
            } else {
                tvNoVotes.setTextColor(
                    ContextCompat.getColor(context, R.color.lm_chat_grey)
                )
            }
        }
    }

    private fun initPollBackground(
        binding: ItemPollBinding,
        data: PollViewData,
        isPollSelectedByUser: Boolean,
        multipleItemPoll: Boolean,
        hasPollEnded: Boolean
    ) {
        val drawable = binding.clPoll1.background as GradientDrawable
        drawable.mutate()
        val width = ViewUtils.dpToPx(1)
        if (!isPollSelectedByUser && hasPollEnded) {
            drawable.setStroke(
                width,
                ContextCompat.getColor(binding.root.context, R.color.lm_chat_cloudy_blue)
            )
        } else {
            if (isPollSelectedByUser) {
                if (data.isSelected == true) {
                    drawable.setStroke(width, LMChatAppearance.getButtonsColor())
                } else {
                    drawable.setStroke(
                        width,
                        ContextCompat.getColor(binding.root.context, R.color.lm_chat_cloudy_blue)
                    )
                }
            } else {
                if (multipleItemPoll) {
                    drawable.setStroke(
                        width,
                        ContextCompat.getColor(binding.root.context, R.color.lm_chat_cloudy_blue)
                    )
                } else {
                    drawable.setStroke(width, LMChatAppearance.getButtonsColor())
                }
            }
        }
    }

    private fun initPollBackgroundProgress(
        binding: ItemPollBinding,
        data: PollViewData,
        isPollSelectedByUser: Boolean,
        pollSubmitted: Boolean,
        instantPoll: Boolean,
        hasPollEnded: Boolean,
        toShowResults: Boolean?
    ) {
        if (toShowResults != null) {
            if (toShowResults == true) {
                binding.backgroundProgress.progress = data.percentage ?: 0
            } else {
                binding.backgroundProgress.progress = 0
            }
        } else {
            if (isPollSelectedByUser) {
                if (pollSubmitted && instantPoll) {
                    // Show progress if user has submitted the poll
                    binding.backgroundProgress.progress = data.percentage ?: 0
                } else {
                    binding.backgroundProgress.progress = 0
                }
            } else {
                // Don't show progress if user hasn't selected any value
                binding.backgroundProgress.progress = 0
            }
        }

        val drawable = binding.backgroundProgress.progressDrawable as LayerDrawable
        val clip = drawable.findDrawableByLayerId(R.id.progress) as ClipDrawable
        if (!isPollSelectedByUser && hasPollEnded) {
            // Show green progress for all if user hasn't participated
            clip.setTint(ContextCompat.getColor(binding.root.context, R.color.lm_chat_cloudy_blue))
        } else {
            if (data.isSelected == true) {
                clip.setTint(LMChatAppearance.getButtonsColor())
            } else {
                clip.setTint(ContextCompat.getColor(binding.root.context, R.color.lm_chat_cloudy_blue))
            }
        }
    }

    private fun checkForSelection(
        binding: ItemPollBinding,
        checkForSelectionEnabled: Boolean = false
    ): Boolean {
        val pollViewData = binding.pollViewData ?: return false
        val parentConversation = pollViewData.parentConversation ?: return false
        val parentPosition = pollViewData.parentViewItemPosition ?: return false
        if (checkForSelectionEnabled) {
            if (chatroomItemAdapterListener?.isSelectionEnabled() == true) {
                chatroomItemAdapterListener.onLongPressConversation(
                    parentConversation,
                    parentPosition
                )
                return true
            }
        } else {
            chatroomItemAdapterListener?.onLongPressConversation(parentConversation, parentPosition)
            return true
        }
        return false
    }

    private fun setNoVotesClickListener(binding: ItemPollBinding) {
        binding.apply {
            tvNoVotes.setOnClickListener {
                if (!checkForSelection(this, checkForSelectionEnabled = true)) {
                    val pollViewData = pollViewData ?: return@setOnClickListener
                    val position = this.position
                    if (pollViewData.toShowResults == true) {
                        pollItemAdapterListener?.showVotersList(
                            pollViewData.id,
                            pollViewData.parentId,
                            pollViewData.pollInfoData,
                            position
                        )
                    }
                }
            }

            tvNoVotes.setOnLongClickListener {
                checkForSelection(binding)
                true
            }

            root.setOnLongClickListener {
                checkForSelection(binding)
                true
            }
        }
    }
}