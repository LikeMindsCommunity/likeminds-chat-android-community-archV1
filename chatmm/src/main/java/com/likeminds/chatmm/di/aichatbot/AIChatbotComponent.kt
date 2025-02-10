package com.likeminds.chatmm.di.aichatbot

import com.likeminds.chatmm.aichatbot.view.LMChatAIBotInitiationActivity
import com.likeminds.chatmm.aichatbot.view.LMChatAIBotInitiationFragment
import dagger.Subcomponent

@Subcomponent(modules = [AIChatbotViewModelModule::class])
interface AIChatbotComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): AIChatbotComponent
    }

    fun inject(lmChatAIBotInitiationActivity: LMChatAIBotInitiationActivity)

    fun inject(lmChatAIBotInitiationFragment: LMChatAIBotInitiationFragment)
}