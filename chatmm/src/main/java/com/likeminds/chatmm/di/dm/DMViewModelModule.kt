package com.likeminds.chatmm.di.dm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.likeminds.chatmm.dm.viewmodel.NetworkingChatViewModel
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap

@Module
abstract class DMViewModelModule {

    @Binds
    abstract fun bindDMFeedViewModelFactory(factory: DMViewModelFactory): ViewModelProvider.Factory

    @Binds
    @IntoMap
    @DMViewModelKey(NetworkingChatViewModel::class)
    abstract fun bindDMFeedViewModel(networkingChatViewModel: NetworkingChatViewModel): ViewModel
}