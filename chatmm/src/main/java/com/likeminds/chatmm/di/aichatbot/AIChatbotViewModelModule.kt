package com.likeminds.chatmm.di.aichatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.likeminds.chatmm.aichatbot.viewmodel.LMChatAIBotViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class AIChatbotViewModelModule {

    @Binds
    abstract fun bindAIChatbotViewModelFactory(factory: AIChatbotViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @AIChatbotViewModelKey(LMChatAIBotViewModel::class)
    abstract fun bindAIChatbotViewModelViewModel(lmChatAIBotViewModel: LMChatAIBotViewModel): ViewModel
}