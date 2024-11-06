package com.likeminds.chatmm.chatroom.detail.view.adapter

import com.likeminds.chatmm.chatroom.detail.model.LMChatAttachmentPickerItemViewData
import com.likeminds.chatmm.chatroom.detail.view.adapter.databinder.LMChatAttachmentPickerItemViewDataBinder
import com.likeminds.chatmm.utils.customview.BaseRecyclerAdapter
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.BaseViewType

class LMChatAttachmentBarAdapter(
    val listener: LMChatAttachmentBarAdapterListener
) : BaseRecyclerAdapter<BaseViewType>() {

    init {
        initViewDataBinders()
    }

    override fun getSupportedViewDataBinder(): MutableList<ViewDataBinder<*, *>> {
        val viewDataBinders = ArrayList<ViewDataBinder<*, *>>(1)

        val lmChatAttachmentPickerItemViewDataBinder =
            LMChatAttachmentPickerItemViewDataBinder(listener)
        viewDataBinders.add(lmChatAttachmentPickerItemViewDataBinder)

        return viewDataBinders
    }
}

interface LMChatAttachmentBarAdapterListener {
    fun onAttachmentBarItemClicked(attachmentItem: LMChatAttachmentPickerItemViewData)
}