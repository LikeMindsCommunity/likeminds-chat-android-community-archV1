package com.likeminds.chatmm.search.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LMChatSearchExtras private constructor(
    val chatroomId: String?
) : Parcelable {
    class Builder {
        private var chatroomId: String? = null

        fun chatroomId(chatroomId: String?) = apply {
            this.chatroomId = chatroomId
        }

        fun build() = LMChatSearchExtras(chatroomId)
    }

    fun toBuilder(): Builder {
        return Builder().chatroomId(chatroomId)
    }
}