package com.likeminds.chatmm.di.chat

import com.likeminds.chatmm.chat.view.CommunityHybridChatFragment
import dagger.Subcomponent

@Subcomponent(modules = [ChatViewModelModule::class])
interface ChatComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): ChatComponent
    }

    fun inject(chatFragment: CommunityHybridChatFragment)
}