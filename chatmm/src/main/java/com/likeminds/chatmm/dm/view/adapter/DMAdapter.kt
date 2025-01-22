package com.likeminds.chatmm.dm.view.adapter

import com.likeminds.chatmm.dm.view.adapter.databinder.DMChatroomViewDataBinder
import com.likeminds.chatmm.dm.view.adapter.databinder.EmptyDMFeedViewDataBinder
import com.likeminds.chatmm.homefeed.model.HomeFeedItemViewData
import com.likeminds.chatmm.homefeed.view.adapter.databinder.HomeChatroomListShimmerViewDataBinder
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.search.view.adapter.databinder.SingleShimmerViewDataBinder
import com.likeminds.chatmm.utils.customview.BaseRecyclerAdapter
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType

class DMAdapter(
    private val dmAdapterListener: DMAdapterListener,
    private val userPreferences: UserPreferences
) : BaseRecyclerAdapter<BaseViewType>() {

    init {
        initViewDataBinders()
    }

    override fun getSupportedViewDataBinder(): MutableList<ViewDataBinder<*, *>> {
        val viewDataBinders = ArrayList<ViewDataBinder<*, *>>(4)

        val dmChatroomViewDataBinder = DMChatroomViewDataBinder(
            dmAdapterListener,
            userPreferences
        )
        viewDataBinders.add(dmChatroomViewDataBinder)

        val singleShimmerViewDataBinder = SingleShimmerViewDataBinder()
        viewDataBinders.add(singleShimmerViewDataBinder)

        val chatroomListShimmerViewDataBinder = HomeChatroomListShimmerViewDataBinder()
        viewDataBinders.add(chatroomListShimmerViewDataBinder)

        val emptyViewDataBinder = EmptyDMFeedViewDataBinder()
        viewDataBinders.add(emptyViewDataBinder)

        return viewDataBinders
    }
}

interface DMAdapterListener {
    fun dmChatroomClicked(homeFeedItemViewData: HomeFeedItemViewData)
}