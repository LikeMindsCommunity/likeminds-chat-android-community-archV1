package com.likeminds.chatmm.di

import android.app.Application
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.di.aichatbot.AIChatbotComponent
import com.likeminds.chatmm.di.aichatbot.AIChatbotComponentModule
import com.likeminds.chatmm.di.chat.ChatComponent
import com.likeminds.chatmm.di.chat.ChatComponentModule
import com.likeminds.chatmm.di.chatroomdetail.ChatroomDetailComponent
import com.likeminds.chatmm.di.chatroomdetail.ChatroomDetailComponentModule
import com.likeminds.chatmm.di.dm.DMComponent
import com.likeminds.chatmm.di.dm.DMComponentModule
import com.likeminds.chatmm.di.explore.ExploreComponent
import com.likeminds.chatmm.di.explore.ExploreComponentModule
import com.likeminds.chatmm.di.homefeed.HomeFeedComponent
import com.likeminds.chatmm.di.homefeed.HomeFeedComponentModule
import com.likeminds.chatmm.di.media.MediaComponent
import com.likeminds.chatmm.di.media.MediaComponentModule
import com.likeminds.chatmm.di.member.MemberComponent
import com.likeminds.chatmm.di.member.MemberComponentModule
import com.likeminds.chatmm.di.polls.PollsComponent
import com.likeminds.chatmm.di.polls.PollsComponentModule
import com.likeminds.chatmm.di.reactions.ReactionsComponent
import com.likeminds.chatmm.di.reactions.ReactionsComponentModule
import com.likeminds.chatmm.di.report.ReportComponent
import com.likeminds.chatmm.di.report.ReportComponentModule
import com.likeminds.chatmm.di.search.SearchComponent
import com.likeminds.chatmm.pushnotification.util.LMChatNotificationHandler
import com.likeminds.chatmm.pushnotification.util.NotificationActionBroadcastReceiver
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        AppModule::class,
        HomeFeedComponentModule::class,
        ExploreComponentModule::class,
        ChatroomDetailComponentModule::class,
        MediaComponentModule::class,
        PollsComponentModule::class,
        ReactionsComponentModule::class,
        ReportComponentModule::class,
        DMComponentModule::class,
        MemberComponentModule::class,
        ChatComponentModule::class,
        AIChatbotComponentModule::class
    ]
)
interface LikeMindsChatComponent {
    fun inject(sdkApplication: SDKApplication)
    fun inject(lmNotificationHandler: LMChatNotificationHandler)
    fun inject(broadcastReceiver: NotificationActionBroadcastReceiver)
    fun homeFeedComponent(): HomeFeedComponent.Factory
    fun exploreComponent(): ExploreComponent.Factory
    fun chatroomDetailComponent(): ChatroomDetailComponent.Factory
    fun mediaComponent(): MediaComponent.Factory
    fun pollsComponent(): PollsComponent.Factory
    fun searchComponent(): SearchComponent.Factory
    fun reactionsComponent(): ReactionsComponent.Factory
    fun reportComponent(): ReportComponent.Factory
    fun dmComponent(): DMComponent.Factory
    fun memberComponent(): MemberComponent.Factory
    fun chatComponent(): ChatComponent.Factory
    fun aiChatbotComponent(): AIChatbotComponent.Factory

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): LikeMindsChatComponent
    }
}