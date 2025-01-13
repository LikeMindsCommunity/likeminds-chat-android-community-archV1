package com.likeminds.chatmm.di.homefeed

import com.likeminds.chatmm.homefeed.view.CommunityChatFragment
import dagger.Subcomponent

@Subcomponent(modules = [HomeFeedViewModelModule::class])
interface HomeFeedComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): HomeFeedComponent
    }

    fun inject(communityChatFragment: CommunityChatFragment)
}