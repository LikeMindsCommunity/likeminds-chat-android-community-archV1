package com.likeminds.chatmm.chatroom.detail.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_ATTACHMENT_PICKER
import kotlinx.parcelize.Parcelize

@Parcelize
class LMChatAttachmentPickerItemViewData private constructor(
    val attachmentName: String,
    @DrawableRes val attachmentIcon: Int,
    val attachmentType: LMChatAttachmentType
) : Parcelable, BaseViewType {
    override val viewType: Int
        get() = ITEM_ATTACHMENT_PICKER

    class Builder {
        private var attachmentName: String = ""
        private var attachmentIcon: Int = 0
        private var attachmentType: LMChatAttachmentType = LMChatAttachmentType.CAMERA

        fun attachmentName(attachmentName: String) = apply { this.attachmentName = attachmentName }
        fun attachmentIcon(@DrawableRes attachmentIcon: Int) =
            apply { this.attachmentIcon = attachmentIcon }

        fun attachmentType(attachmentType: LMChatAttachmentType) =
            apply { this.attachmentType = attachmentType }

        fun build(): LMChatAttachmentPickerItemViewData {
            return LMChatAttachmentPickerItemViewData(
                attachmentName,
                attachmentIcon,
                attachmentType
            )
        }
    }

    fun toBuilder(): Builder {
        return Builder()
            .attachmentName(attachmentName)
            .attachmentIcon(attachmentIcon)
            .attachmentType(attachmentType)
    }
}