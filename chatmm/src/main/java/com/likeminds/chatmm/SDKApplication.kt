package com.likeminds.chatmm

import android.app.Application
import android.content.Context
import com.amazonaws.mobile.client.*
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.google.android.exoplayer2.util.Log
import com.likeminds.chatmm.di.DaggerLikeMindsChatComponent
import com.likeminds.chatmm.di.LikeMindsChatComponent
import com.likeminds.chatmm.di.aichatbot.AIChatbotComponent
import com.likeminds.chatmm.di.chat.ChatComponent
import com.likeminds.chatmm.di.chatroomdetail.ChatroomDetailComponent
import com.likeminds.chatmm.di.dm.DMComponent
import com.likeminds.chatmm.di.explore.ExploreComponent
import com.likeminds.chatmm.di.homefeed.HomeFeedComponent
import com.likeminds.chatmm.di.media.MediaComponent
import com.likeminds.chatmm.di.member.MemberComponent
import com.likeminds.chatmm.di.polls.PollsComponent
import com.likeminds.chatmm.di.reactions.ReactionsComponent
import com.likeminds.chatmm.di.report.ReportComponent
import com.likeminds.chatmm.di.search.SearchComponent
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.theme.model.LMChatAppearanceRequest
import com.likeminds.chatmm.utils.user.LMChatUserMetaData
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.LMChatSDKCallback
import com.likeminds.likemindschat.conversation.model.ConversationState
import com.likeminds.likemindschat.helper.model.LMChatInitiateLoggerRequest
import com.likeminds.likemindschat.helper.model.LMSeverity
import com.likeminds.likemindschat.user.model.InitiateUserRequest
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SDKApplication : LMChatSDKCallback {

    @Inject
    lateinit var transferUtility: TransferUtility

    private lateinit var lmChatUserMetaData: LMChatUserMetaData

    private lateinit var mChatClient: LMChatClient

    private var likeMindsChatComponent: LikeMindsChatComponent? = null

    //used to track opened chatroom
    var openedChatroomId: String? = null

    private var homeFeedComponent: HomeFeedComponent? = null
    private var exploreComponent: ExploreComponent? = null
    private var chatroomDetailComponent: ChatroomDetailComponent? = null
    private var mediaComponent: MediaComponent? = null
    private var searchComponent: SearchComponent? = null
    private var pollsComponent: PollsComponent? = null
    private var reactionsComponent: ReactionsComponent? = null
    private var reportComponent: ReportComponent? = null
    private var dmComponent: DMComponent? = null
    private var memberComponent: MemberComponent? = null
    private var chatComponent: ChatComponent? = null
    private var aiChatbotComponent: AIChatbotComponent? = null

    companion object {
        const val LOG_TAG = "LikeMindsChat"
        private var sdkApplicationInstance: SDKApplication? = null
        private var lmChatCoreCallback: LMChatCoreCallback? = null

        var selectedTheme = LMChatTheme.COMMUNITY_CHAT

        /**
         * @return Singleton Instance of SDK Application class, which used for injecting dagger in fragments.
         * */
        @JvmStatic
        fun getInstance(): SDKApplication {
            if (sdkApplicationInstance == null) {
                sdkApplicationInstance = SDKApplication()
            }
            return sdkApplicationInstance!!
        }

        /**
         * @return Singleton Instance of Call backs required
         * */
        @JvmStatic
        fun getLikeMindsCallback(): LMChatCoreCallback? {
            return lmChatCoreCallback
        }
    }

    suspend fun initSDKApplication(
        application: Application,
        theme: LMChatTheme,
        lmChatCoreCallback: LMChatCoreCallback?,
        lmChatAppearanceRequest: LMChatAppearanceRequest?,
        domain: String? = null,
        enablePushNotifications: Boolean = false,
        deviceId: String? = null,
        shareLogsWithLM: Boolean,
        excludeConversationStates: List<ConversationState> = emptyList()
    ) {
        val initiateLoggerRequest = if (shareLogsWithLM) {
            LMChatInitiateLoggerRequest.Builder()
                .shareLogsWithLM(true)
                .coreVersion("${BuildConfig.APP_MAJOR}.${BuildConfig.APP_MINOR}.${BuildConfig.APP_PATCH}")
                .logLevel(LMSeverity.INFO)
                .onErrorHandler { exception, trace ->
                    lmChatCoreCallback?.onErrorHandler(exception, trace)
                }
                .build()
        } else {
            null
        }

        Log.d(
            "PUI",
            "excludeConversationStates received in SDKApplication: ${excludeConversationStates.map { it.value }}"
        )

        mChatClient = LMChatClient.Builder(application)
            .lmChatSDKCallback(this)
            .initiateLoggerRequest(initiateLoggerRequest)
            .excludedConversationStates(excludeConversationStates)
            .build()

        selectedTheme = theme

        SDKApplication.lmChatCoreCallback = lmChatCoreCallback
        setupTheme(lmChatAppearanceRequest)
        initAppComponent(application)
        EmojiManager.install(GoogleEmojiProvider())
        initAWSMobileClient(application)

        lmChatUserMetaData = LMChatUserMetaData.getInstance()
        lmChatUserMetaData.init(domain, enablePushNotifications, deviceId)
    }

    // sets theme to the app
    fun setupTheme(lmChatAppearanceRequest: LMChatAppearanceRequest?) {
        val chatTheme = lmChatAppearanceRequest ?: LMChatAppearanceRequest.Builder().build()
        LMChatAppearance.setTheme(chatTheme)
    }

    private fun initAWSMobileClient(applicationContext: Context) {
        AWSMobileClient.getInstance()
            .initialize(applicationContext, object : Callback<UserStateDetails> {
                override fun onResult(result: UserStateDetails?) {
                }

                override fun onError(e: java.lang.Exception?) {
                }
            })
    }

    /**
     * initiate dagger for the sdk
     *
     * @param application : The client will pass instance application to the function
     * */
    private fun initAppComponent(application: Application) {
        if (likeMindsChatComponent == null) {
            likeMindsChatComponent = DaggerLikeMindsChatComponent.builder()
                .application(application)
                .build()
        }
        likeMindsChatComponent!!.inject(this)
    }

    /**
     * initiate and return HomeFeedComponent: All dependencies required for home package
     * */
    fun homeFeedComponent(): HomeFeedComponent? {
        if (homeFeedComponent == null) {
            homeFeedComponent = likeMindsChatComponent?.homeFeedComponent()?.create()
        }
        return homeFeedComponent
    }

    /**
     * initiate and return ExploreComponent: All dependencies required for explore screen
     * */
    fun exploreComponent(): ExploreComponent? {
        if (exploreComponent == null) {
            exploreComponent = likeMindsChatComponent?.exploreComponent()?.create()
        }
        return exploreComponent
    }

    /**
     * initiate and return ChatroomDetailComponent: All dependencies required for chatroom screen package
     * */
    fun chatroomDetailComponent(): ChatroomDetailComponent? {
        if (chatroomDetailComponent == null) {
            chatroomDetailComponent = likeMindsChatComponent?.chatroomDetailComponent()?.create()
        }

        return chatroomDetailComponent
    }

    /**
     * initiate and return MediaComponent: All dependencies required for media package
     * */
    fun mediaComponent(): MediaComponent? {
        if (mediaComponent == null) {
            mediaComponent = likeMindsChatComponent?.mediaComponent()?.create()
        }
        return mediaComponent
    }

    /**
     * initiate and return PollsComponent: All dependencies required for polls package
     * */
    fun pollsComponent(): PollsComponent? {
        if (pollsComponent == null) {
            pollsComponent = likeMindsChatComponent?.pollsComponent()?.create()
        }
        return pollsComponent
    }

    /**
     * initiate and return [SearchComponent]: All dependencies required for search screens
     */
    fun searchComponent(): SearchComponent? {
        if (searchComponent == null) {
            searchComponent = likeMindsChatComponent?.searchComponent()?.create()
        }

        return searchComponent
    }

    /**
     * initiate and return ReactionsComponent: All dependencies required for reactions package
     * */
    fun reactionsComponent(): ReactionsComponent? {
        if (reactionsComponent == null) {
            reactionsComponent = likeMindsChatComponent?.reactionsComponent()?.create()
        }
        return reactionsComponent
    }

    /**
     * initiate and return ReportComponent: All dependencies required for report package
     * */
    fun reportComponent(): ReportComponent? {
        if (reportComponent == null) {
            reportComponent = likeMindsChatComponent?.reportComponent()?.create()
        }
        return reportComponent
    }

    /**
     * initiate and return DMComponent: All dependencies required for report package
     * */
    fun dmComponent(): DMComponent? {
        if (dmComponent == null) {
            dmComponent = likeMindsChatComponent?.dmComponent()?.create()
        }
        return dmComponent
    }

    /**
     * initiate and return MemberComponent: All dependencies required for report package
     * */
    fun memberComponent(): MemberComponent? {
        if (memberComponent == null) {
            memberComponent = likeMindsChatComponent?.memberComponent()?.create()
        }
        return memberComponent
    }

    /**
     * initiate and return ChatComponent: All dependencies required for report package
     * */
    fun chatComponent(): ChatComponent? {
        if (chatComponent == null) {
            chatComponent = likeMindsChatComponent?.chatComponent()?.create()
        }
        return chatComponent
    }

    /**
     * initiate and return AIChatbotComponent: All dependencies required for chatroom screen package
     * */
    fun aiChatbotComponent(): AIChatbotComponent? {
        if (aiChatbotComponent == null) {
            aiChatbotComponent = likeMindsChatComponent?.aiChatbotComponent()?.create()
        }

        return aiChatbotComponent
    }

    override fun onAccessTokenExpiredAndRefreshed(accessToken: String, refreshToken: String) {
        lmChatCoreCallback?.onAccessTokenExpiredAndRefreshed(accessToken, refreshToken)
    }

    override fun onRefreshTokenExpired(): Pair<String?, String?> {
        val apiKey = mChatClient.getAPIKey().data

        return if (!apiKey.isNullOrEmpty()) {
            runBlocking {
                val user = mChatClient.getLoggedInUser().data?.user
                if (user != null) {
                    val initiateUserRequest = InitiateUserRequest.Builder()
                        .apiKey(apiKey)
                        .userName(user.name)
                        .userId(user.sdkClientInfo?.uuid)
                        .build()
                    val response = mChatClient.initiateUser(initiateUserRequest)

                    LMAnalytics.track(
                        LMAnalytics.Events.SDK_INITIATE,
                        mapOf(
                            "success" to response.success.toString(),
                            LMAnalytics.Keys.UUID to user.sdkClientInfo?.uuid
                        )
                    )

                    if (response.success) {
                        val accessToken = response.data?.accessToken ?: ""
                        val refreshToken = response.data?.refreshToken ?: ""
                        Pair(accessToken, refreshToken)
                    } else {
                        Pair(null, null)
                    }
                } else {
                    Pair(null, null)
                }
            }
        } else {
            lmChatCoreCallback?.onRefreshTokenExpired() ?: Pair(null, null)
        }
    }
}