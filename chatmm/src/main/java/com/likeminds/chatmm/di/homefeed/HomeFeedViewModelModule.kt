package com.likeminds.chatmm.di.homefeed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.likeminds.chatmm.homefeed.viewmodel.CommunityChatViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class HomeFeedViewModelModule {

    @Binds
    abstract fun bindHomeFeedViewModelFactory(factory: HomeFeedViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @HomeFeedViewModelKey(CommunityChatViewModel::class)
    abstract fun bindHomeFeedViewModel(communityChatViewModel: CommunityChatViewModel): ViewModel
}