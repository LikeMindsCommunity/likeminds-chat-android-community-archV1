package com.likeminds.chatmm.aichatbot

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import com.google.android.material.button.MaterialButton
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.R
import com.likeminds.chatmm.aichatbot.model.LMChatAIButtonProps
import com.likeminds.chatmm.aichatbot.util.LMChatAIChatBotPreferences
import com.likeminds.chatmm.aichatbot.view.LMChatAIBotInitiationActivity
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailFragment.Companion.SOURCE_AI_CHATBOT
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.ViewUtils

class LMChatAIButton : MaterialButton {

    constructor(context: Context) : super(context) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        initialize()
    }

    private fun initialize() {
        backgroundTintList = ColorStateList.valueOf(LMChatAppearance.getButtonsColor())
        text = context.getString(R.string.lm_chat_ai_bot)
    }

    private lateinit var chatAIButtonProps: LMChatAIButtonProps

    fun setChatAIButtonProps(chatAIButtonProps: LMChatAIButtonProps) {
        chatAIButtonProps.apply {
            if (apiKey == null) {
                if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                    throw IllegalArgumentException("Please pass `accessToken` and `refreshToken`")
                } else {
                    this@LMChatAIButton.chatAIButtonProps = chatAIButtonProps
                }
            } else {
                when {
                    apiKey.isEmpty() -> {
                        throw IllegalArgumentException("Please pass `apiKey`")
                    }

                    uuid.isNullOrEmpty() -> {
                        throw IllegalArgumentException("Please pass `uuid`")
                    }

                    userName.isNullOrEmpty() -> {
                        throw IllegalArgumentException("Please pass `userName`")
                    }

                    else -> {
                        this@LMChatAIButton.chatAIButtonProps = chatAIButtonProps
                    }
                }
            }
        }

        this.setOnClickListener {
            startAIChatBot()
        }
    }

    @Throws(Exception::class)
    fun startAIChatBot() {
        chatAIButtonProps.apply {
            if (!::chatAIButtonProps.isInitialized) {
                throw IllegalStateException("Please call `setChatAIButtonProps()` with appropriate parameters.")
            }

            if (apiKey != null) {
                LMChatCore.showChat(
                    context,
                    apiKey,
                    userName ?: "",
                    uuid ?: "",
                    success = {
                        initiateChatBot()
                    },
                    error = {
                        ViewUtils.showShortToast(
                            context,
                            context.getString(R.string.lm_chat_something_went_wrong)
                        )
                    }
                )
            } else {
                LMChatCore.showChat(
                    context,
                    accessToken,
                    refreshToken,
                    success = {
                        initiateChatBot()
                    },
                    error = {
                        ViewUtils.showShortToast(
                            context,
                            context.getString(R.string.lm_chat_something_went_wrong)
                        )
                    }
                )
            }
        }
    }

    private fun initiateChatBot() {
        val chatroomIDWithAIChatbot =
            LMChatAIChatBotPreferences(context).getChatroomIDWithAIChatbot()
        if (chatroomIDWithAIChatbot.isEmpty()) {
            LMChatAIBotInitiationActivity.start(context)
        } else {
            ChatroomDetailActivity.start(
                context,
                ChatroomDetailExtras.Builder()
                    .chatroomId(chatroomIDWithAIChatbot)
                    .source(SOURCE_AI_CHATBOT)
                    .build()
            )
        }
    }
}