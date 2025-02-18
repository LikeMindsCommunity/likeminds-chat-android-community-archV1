package com.likeminds.chatmm.reactions.view

import androidx.recyclerview.widget.LinearLayoutManager
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.chatroom.detail.view.adapter.ReactionAdapter
import com.likeminds.chatmm.chatroom.detail.view.adapter.ReactionAdapterListener
import com.likeminds.chatmm.databinding.FragmentReactionListBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.reactions.model.ReactionViewData
import com.likeminds.chatmm.reactions.view.adapter.ReactionsTabAdapter
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import javax.inject.Inject

class ReactionListFragment : BaseFragment<FragmentReactionListBinding, Nothing>(),
    ReactionAdapterListener {

    companion object {
        const val TAG = "ReactionListFragment"
        fun getInstance(): ReactionListFragment {
            return ReactionListFragment()
        }
    }

    private var reactionRemovedFragmentListener:
            ReactionRemovedFragmentListener? = null

    lateinit var adapter: ReactionAdapter

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun getViewModelClass(): Class<Nothing>? {
        return null
    }

    override fun getViewBinding(): FragmentReactionListBinding {
        return FragmentReactionListBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        try {
            reactionRemovedFragmentListener =
                parentFragment as ReactionRemovedFragmentListener?
        } catch (e: ClassCastException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            throw ClassCastException("Calling fragment must implement MessageReactionRemovedFragmentListener interface")
        }
        SDKApplication.getInstance().reactionsComponent()?.inject(this)
    }

    override fun setUpViews() {
        super.setUpViews()
        initRecyclerView()
        arguments?.takeIf { it.containsKey(ReactionsTabAdapter.ARG_COUNT) }
        adapter.setItems(getReactionsList())
    }

    private fun initRecyclerView() {
        adapter = ReactionAdapter(userPreferences, this)
        val layoutManager = LinearLayoutManager(context)
        binding.rvReactions.apply {
            this.layoutManager = layoutManager
            setHasFixedSize(true)
            adapter = this@ReactionListFragment.adapter
        }
    }

    private fun getAllReactionsList(): ArrayList<ReactionViewData>? {
        arguments?.takeIf { it.containsKey(ReactionsTabAdapter.ARG_LIST_ALL) }?.apply {
            return getParcelableArrayList(ReactionsTabAdapter.ARG_LIST_ALL)
        }
        return null
    }

    private fun getMembersList(): ArrayList<ReactionViewData>? {
        arguments?.takeIf { it.containsKey(ReactionsTabAdapter.ARG_LIST) }?.apply {
            return getParcelableArrayList(ReactionsTabAdapter.ARG_LIST)
        }
        return null
    }

    private fun getReaction(): String? {
        arguments?.takeIf { it.containsKey(ReactionsTabAdapter.ARG_REACTION) }?.apply {
            return getString(ReactionsTabAdapter.ARG_REACTION)
        }
        return null
    }

    private fun getConversationId(): String? {
        arguments?.takeIf { it.containsKey(ReactionsTabAdapter.ARG_CONVERSATION_ID) }?.apply {
            return getString(ReactionsTabAdapter.ARG_CONVERSATION_ID)
        }
        return null
    }

    private fun getReactionsList(): List<BaseViewType> {
        val dataList = mutableListOf<BaseViewType>()
        if (!getMembersList().isNullOrEmpty() && !getReaction().isNullOrEmpty() && !getConversationId().isNullOrEmpty()) {
            getMembersList()?.map {
                dataList.add(it)
            }
        }
        if (!getAllReactionsList().isNullOrEmpty()) {
            getAllReactionsList()?.map {
                dataList.add(it)
            }
        }
        return dataList
    }

    override fun removeReaction(conversationId: String) {
        reactionRemovedFragmentListener?.removedReaction(conversationId)
    }

    override fun removeChatroomReaction() {
        reactionRemovedFragmentListener?.removedChatroomReaction()
    }
}

interface ReactionRemovedFragmentListener {
    fun removedReaction(conversationId: String)
    fun removedChatroomReaction()
}