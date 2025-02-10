package com.likeminds.chatmm.aichatbot

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.R
import com.likeminds.chatmm.aichatbot.model.LMChatAIBotButtonProps
import com.likeminds.chatmm.aichatbot.util.LMChatAIBotPreferences
import com.likeminds.chatmm.aichatbot.view.LMChatAIBotInitiationActivity
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailFragment.Companion.SOURCE_AI_CHATBOT
import com.likeminds.chatmm.utils.ViewUtils

class LMChatAIBotButton : ExtendedFloatingActionButton {

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
        backgroundTintList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ColorStateList.valueOf(resources.getColor(R.color.lm_chat_start_ai_chatbot_blue, null))
        } else {
            ColorStateList.valueOf(context.resources.getColor(R.color.lm_chat_start_ai_chatbot_blue))
        }

        text = context.getString(R.string.lm_chat_ai_bot)
        textSize = 14f

        val whiteColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ColorStateList.valueOf(resources.getColor(R.color.lm_chat_white, null))
        } else {
            ColorStateList.valueOf(context.resources.getColor(R.color.lm_chat_white))
        }
        setTextColor(whiteColor)

        isExtended = true

        setIconResource(R.drawable.lm_chat_ic_start_ai_chat_bot)
        iconTint = whiteColor
    }

    private lateinit var chatAIButtonProps: LMChatAIBotButtonProps

    fun setChatAIButtonProps(chatAIButtonProps: LMChatAIBotButtonProps) {
        chatAIButtonProps.apply {
            if (apiKey == null) {
                if (accessToken.isNullOrEmpty() || refreshToken.isNullOrEmpty()) {
                    throw IllegalArgumentException("Please pass `accessToken` and `refreshToken`")
                } else {
                    this@LMChatAIBotButton.chatAIButtonProps = chatAIButtonProps
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
                        this@LMChatAIBotButton.chatAIButtonProps = chatAIButtonProps
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
            LMChatAIBotPreferences(context).getChatroomIDWithAIChatbot()
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