package com.likeminds.chatmm.di.dm

import com.likeminds.chatmm.dm.view.NetworkingChatFragment
import dagger.Subcomponent

@Subcomponent(modules = [DMViewModelModule::class])
interface DMComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): DMComponent
    }

    fun inject(networkingChatFragment: NetworkingChatFragment)
}