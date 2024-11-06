package com.likeminds.chatmm.chatroom.detail.view.adapter.databinder

import android.view.LayoutInflater
import android.view.ViewGroup
import com.likeminds.chatmm.chatroom.detail.model.LMChatAttachmentPickerItemViewData
import com.likeminds.chatmm.chatroom.detail.view.adapter.LMChatAttachmentBarAdapterListener
import com.likeminds.chatmm.databinding.LmChatItemPickerAttachmentBinding
import com.likeminds.chatmm.utils.customview.ViewDataBinder
import com.likeminds.chatmm.utils.model.ITEM_ATTACHMENT_PICKER

class LMChatAttachmentPickerItemViewDataBinder(
    val listener: LMChatAttachmentBarAdapterListener
) : ViewDataBinder<LmChatItemPickerAttachmentBinding, LMChatAttachmentPickerItemViewData>() {
    override val viewType: Int
        get() = ITEM_ATTACHMENT_PICKER

    override fun createBinder(parent: ViewGroup): LmChatItemPickerAttachmentBinding {
        val binding = LmChatItemPickerAttachmentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        setListener(binding)

        return binding
    }

    override fun bindData(
        binding: LmChatItemPickerAttachmentBinding,
        data: LMChatAttachmentPickerItemViewData,
        position: Int
    ) {
        binding.apply {
            attachmentData = data

            ivAttachmentIcon.setImageResource(data.attachmentIcon)
            tvAttachmentLabel.text = data.attachmentName
        }
    }

    private fun setListener(binding: LmChatItemPickerAttachmentBinding) {
        binding.root.setOnClickListener {
            val attachmentData = binding.attachmentData ?: return@setOnClickListener
            listener.onAttachmentBarItemClicked(attachmentData)
        }
    }
}