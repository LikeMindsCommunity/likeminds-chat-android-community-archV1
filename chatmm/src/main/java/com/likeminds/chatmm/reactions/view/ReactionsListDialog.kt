package com.likeminds.chatmm.reactions.view

import android.os.Bundle
import com.google.android.material.tabs.TabLayoutMediator
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.DialogReactionsListBinding
import com.likeminds.chatmm.reactions.model.ReactionsListExtras
import com.likeminds.chatmm.reactions.util.ReactionUtil
import com.likeminds.chatmm.reactions.view.adapter.ReactionsTabAdapter
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.customview.BaseBottomSheetFragment

class ReactionsListDialog :
    BaseBottomSheetFragment<DialogReactionsListBinding, Nothing>(),
    ReactionRemovedFragmentListener {

    companion object {
        const val TAG = "MessageReactionsListDialog"
        const val ARG_MESSAGE_REACTIONS_LIST_EXTRAS = "ARG_MESSAGE_REACTIONS_LIST_EXTRAS"

        @JvmStatic
        fun newInstance(
            reactionsListExtras: ReactionsListExtras
        ) = ReactionsListDialog()
            .apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_MESSAGE_REACTIONS_LIST_EXTRAS, reactionsListExtras)
                }
            }
    }

    private var reactionRemovedDialogListener: ReactionRemovedDialogListener? = null
    private lateinit var reactionsTabAdapter: ReactionsTabAdapter
    private var reactionsListExtras: ReactionsListExtras? = null

    override fun getViewModelClass(): Class<Nothing>? {
        return null
    }

    override fun getViewBinding(): DialogReactionsListBinding {
        return DialogReactionsListBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().reactionsComponent()?.inject(this)
    }

    override fun receiveExtras() {
        super.receiveExtras()
        try {
            reactionRemovedDialogListener =
                parentFragment as ReactionRemovedDialogListener?
        } catch (e: ClassCastException) {
            throw ClassCastException("Calling fragment must implement MessageReactionRemovedDialogListener interface")
        }
        arguments?.let { bundle ->
            reactionsListExtras = ExtrasUtil.getParcelable(
                bundle,
                ARG_MESSAGE_REACTIONS_LIST_EXTRAS,
                ReactionsListExtras::class.java
            )
        }
    }

    override fun setUpViews() {
        super.setUpViews()
        if (reactionsListExtras?.isConversation == true) {
            initializeListeners()
        } else {
            initializeChatroomListeners()
        }
    }

    private fun initializeListeners() {
        val allReactions = reactionsListExtras?.conversation?.reactions?.toMutableList()
        val reactionsHashmap = reactionsListExtras?.conversation?.let {
            ReactionUtil.getMessageReactions(it)
        }
        val position = reactionsListExtras?.gridPositionClicked
        val conversationId = reactionsListExtras?.conversation?.id
        if (!reactionsHashmap.isNullOrEmpty()
            && !allReactions.isNullOrEmpty()
            && !conversationId.isNullOrEmpty()
        ) {
            if (reactionRemovedDialogListener != null) {
                reactionsTabAdapter = ReactionsTabAdapter(
                    this,
                    reactionsHashmap.size + 1,
                    allReactions,
                    reactionsHashmap,
                    conversationId
                )
                val reactionsViewPager = binding.reactionsViewPager
                val reactionTabs = binding.reactionsTab
                reactionsViewPager.adapter = reactionsTabAdapter
                if (position == 1 || position == 2) {
                    reactionsViewPager.setCurrentItem(position, true)
                } else {
                    reactionsViewPager.setCurrentItem(0, true)
                }
                TabLayoutMediator(reactionTabs, reactionsViewPager) { tab, position ->
                    if (position == 0) {
                        tab.text = "All (${allReactions.size})"
                    } else {
                        val listOfReactions = reactionsHashmap.keys.toList()
                        val reaction = listOfReactions[position - 1]
                        tab.text =
                            "$reaction ${reactionsHashmap[listOfReactions[position - 1]]?.size}"
                    }
                }.attach()
            }

        }
    }

    private fun initializeChatroomListeners() {
        val allReactions = reactionsListExtras?.chatroom?.reactions?.toMutableList()
        val reactionsHashmap = reactionsListExtras?.chatroom?.let {
            ReactionUtil.getChatroomReactions(it)
        }
        val position = reactionsListExtras?.gridPositionClicked
        val chatroomId = reactionsListExtras?.chatroom?.id
        if (!reactionsHashmap.isNullOrEmpty()
            && !allReactions.isNullOrEmpty()
            && !chatroomId.isNullOrEmpty()
        ) {
            if (reactionRemovedDialogListener != null) {
                reactionsTabAdapter =
                    ReactionsTabAdapter(
                        this,
                        reactionsHashmap.size + 1,
                        allReactions,
                        reactionsHashmap,
                        chatroomId
                    )
                val reactionsViewPager = binding.reactionsViewPager
                val reactionTabs = binding.reactionsTab
                reactionsViewPager.adapter = reactionsTabAdapter
                if (position == 1 || position == 2) {
                    reactionsViewPager.setCurrentItem(position, true)
                } else {
                    reactionsViewPager.setCurrentItem(0, true)
                }
                TabLayoutMediator(reactionTabs, reactionsViewPager) { tab, index ->
                    if (index == 0) {
                        tab.text = "All (${allReactions.size})"
                    } else {
                        val listOfReactions = reactionsHashmap.keys.toList()
                        val reaction = listOfReactions[index - 1]
                        tab.text =
                            "$reaction ${reactionsHashmap[listOfReactions[index - 1]]?.size}"
                    }
                }.attach()
            }

        }
    }


    override fun removedReaction(conversationId: String) {
        reactionRemovedDialogListener?.removedReaction(conversationId)
        dismiss()
    }

    override fun removedChatroomReaction() {
        reactionRemovedDialogListener?.removedChatroomReaction()
        dismiss()
    }
}

interface ReactionRemovedDialogListener {
    fun removedReaction(conversationId: String)
    fun removedChatroomReaction()
}