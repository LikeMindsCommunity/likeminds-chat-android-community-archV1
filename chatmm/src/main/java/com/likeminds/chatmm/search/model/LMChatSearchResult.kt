package com.likeminds.chatmm.search.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class LMChatSearchResult private constructor(
    val conversationId: String?
) : Parcelable {
    class Builder {
        private var conversationId: String? = null

        fun conversationId(conversationId: String?) = apply {
            this.conversationId = conversationId
        }

        fun build() = LMChatSearchResult(conversationId)
    }

    fun toBuilder(): Builder {
        return Builder().conversationId(conversationId)
    }
}