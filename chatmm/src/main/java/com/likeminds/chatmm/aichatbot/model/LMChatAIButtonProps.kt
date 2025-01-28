package com.likeminds.chatmm.aichatbot.model

class LMChatAIButtonProps private constructor(
    val apiKey: String?,
    val uuid: String?,
    val userName: String?,
    val imageUrl: String?,
    val isGuest: String?,
    val accessToken: String?,
    val refreshToken: String?
) {
    class Builder {
        private var apiKey: String? = null
        private var uuid: String? = null
        private var userName: String? = null
        private var imageUrl: String? = null
        private var isGuest: String? = null
        private var accessToken: String? = null
        private var refreshToken: String? = null

        fun apiKey(apiKey: String?) = apply {
            this.apiKey = apiKey
        }

        fun uuid(uuid: String?) = apply {
            this.uuid = uuid
        }

        fun userName(userName: String?) = apply {
            this.userName = userName
        }

        fun imageUrl(imageUrl: String?) = apply {
            this.imageUrl = imageUrl
        }

        fun isGuest(isGuest: String?) = apply {
            this.isGuest = isGuest
        }

        fun accessToken(accessToken: String?) = apply {
            this.accessToken = accessToken
        }

        fun refreshToken(refreshToken: String?) = apply {
            this.refreshToken = refreshToken
        }
    }

    fun toBuilder(): Builder {
        return Builder().apiKey(apiKey)
            .uuid(uuid)
            .userName(userName)
            .imageUrl(imageUrl)
            .isGuest(isGuest)
            .accessToken(accessToken)
            .refreshToken(refreshToken)
    }
}