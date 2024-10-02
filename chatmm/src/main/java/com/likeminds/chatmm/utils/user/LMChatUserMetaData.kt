package com.likeminds.chatmm.utils.user

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.community.utils.LMChatCommunitySettingsUtil
import com.likeminds.chatmm.member.model.UserResponse
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.utils.SDKPreferences
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.community.model.CommunitySetting
import com.likeminds.likemindschat.community.model.ConfigurationType
import com.likeminds.likemindschat.user.model.RegisterDeviceRequest
import kotlinx.coroutines.*

class LMChatUserMetaData {
    private var domain: String? = null
    private var enablePushNotifications: Boolean = false
    var deviceId: String? = null

    private lateinit var mChatClient: LMChatClient

    companion object {
        private var instance: LMChatUserMetaData? = null
        const val WIDGET_MESSAGE_KEY = "message"

        fun getInstance(): LMChatUserMetaData {
            if (instance == null) {
                instance = LMChatUserMetaData()
            }
            return instance!!
        }
    }

    //init user meta data
    fun init(
        domain: String?,
        enablePushNotifications: Boolean,
        deviceId: String?
    ) {
        this.domain = domain
        this.enablePushNotifications = enablePushNotifications
        this.deviceId = deviceId

        mChatClient = LMChatClient.getInstance()
    }

    //perform post user session init
    fun onPostUserSessionInit(
        context: Context,
        userResponse: UserResponse
    ) {
        val userName = userResponse.user?.name
        val uuid = userResponse.user?.sdkClientInfo?.uuid
        val memberId = userResponse.user?.id
        val communitySettings = userResponse.community?.communitySettings ?: listOf()

        saveUserPreferences(context, userName, uuid, memberId)
        getConfig(context)
        pushToken()
        getCommunityConfiguration(context)
        saveCommunitySettings(communitySettings)
    }

    //save community settings
    private fun saveCommunitySettings(communitySettings: List<CommunitySetting>) {
        LMChatCommunitySettingsUtil.setCommunitySettings(communitySettings)
    }

    //get config
    private fun getConfig(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val getConfigResponse = mChatClient.getConfig()

            val sdkPreferences = SDKPreferences(context)

            if (getConfigResponse.success) {
                val data = getConfigResponse.data
                if (data != null) {
                    sdkPreferences.setMicroPollsEnabled(data.enableMicroPolls)
                    sdkPreferences.setGifSupportEnabled(data.enableGifs)
                    sdkPreferences.setAudioSupportEnabled(data.enableAudio)
                    sdkPreferences.setVoiceNoteSupportEnabled(data.enableVoiceNote)
                }
            } else {
                Log.d(
                    SDKApplication.LOG_TAG,
                    "config api failed: ${getConfigResponse.errorMessage}"
                )
                // sets default values to config prefs
                sdkPreferences.setDefaultConfigPrefs()
            }
        }
    }

    // get community configuration
    private fun getCommunityConfiguration(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val communityConfigurationResponse = mChatClient.getCommunityConfigurations()
            if (communityConfigurationResponse.success) {
                val widgetConfiguration =
                    communityConfigurationResponse.data?.configurations?.find {
                        it.type == ConfigurationType.WIDGET_METADATA
                    } ?: return@launch

                val value = widgetConfiguration.value

                val sdkPreferences = SDKPreferences(context)
                if (value.has(WIDGET_MESSAGE_KEY)) {
                    val isEnabled = value.getBoolean(WIDGET_MESSAGE_KEY)
                    sdkPreferences.setIsWidgetEnabled(isEnabled)
                } else {
                    sdkPreferences.setIsWidgetEnabled(false)
                }
            }
        }
    }

    //register device for the notification
    private fun pushToken() {
        if (enablePushNotifications && !deviceId.isNullOrEmpty()) {
            try {
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(
                            SDKApplication.LOG_TAG,
                            "Fetching FCM registration token failed",
                            task.exception
                        )
                        return@addOnCompleteListener
                    }

                    val token = task.result.toString()
                    registerDevice(token, deviceId ?: "")
                }
            } catch (e: Exception) {
                Log.w(
                    SDKApplication.LOG_TAG,
                    "Please add firebase to your project to enable notifications"
                )
            }
        }
    }

    //call register device api
    private fun registerDevice(token: String, id: String) {
        CoroutineScope(Dispatchers.IO).launch {
            //create request
            val request = RegisterDeviceRequest.Builder()
                .deviceId(id)
                .token(token)
                .build()

            //call api
            mChatClient.registerDevice(request)
        }
    }

    //save details in user preferences
    private fun saveUserPreferences(
        context: Context,
        userName: String?,
        uuid: String?,
        memberId: String?
    ) {
        val userPreferences = UserPreferences(context)
        userPreferences.apply {
            setMemberName(userName ?: "")
            setUUID(uuid ?: "")
            setMemberId(memberId ?: "")
        }
    }
}