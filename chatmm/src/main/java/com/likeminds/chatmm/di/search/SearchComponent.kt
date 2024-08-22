package com.likeminds.chatmm.di.search

import com.likeminds.chatmm.search.view.LMChatSearchActivity
import com.likeminds.chatmm.search.view.LMChatSearchFragment
import dagger.Subcomponent

@Subcomponent(modules = [SearchViewModelModule::class])
interface SearchComponent {

    @Subcomponent.Factory
    interface Factory {
        fun create(): SearchComponent
    }

    fun inject(searchActivity: LMChatSearchActivity)
    fun inject(searchFragment: LMChatSearchFragment)
}