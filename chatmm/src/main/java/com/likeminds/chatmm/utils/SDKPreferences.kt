package com.likeminds.chatmm.utils

import android.content.Context
import com.likeminds.chatmm.utils.sharedpreferences.BasePreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SDKPreferences @Inject constructor(
    context: Context
) : BasePreferences(SDK_PREFS, context) {
    companion object {
        const val SDK_PREFS = "sdk_prefs"

        private const val API_KEY = "api_key"
        private const val GIF_SUPPORT_ENABLED = "gif_support_enabled"
        private const val MICRO_POLLS_ENABLED = "micro_polls_enabled"
        private const val AUDIO_SUPPORT_ENABLED = "audio_support_enabled"
        private const val VOICE_NOTE_ENABLED = "voice_note_enabled"
        private const val SLIDE_UP_VOICE_NOTE_TOAST = "SLIDE_UP_VOICE_NOTE_TOAST"
        private const val WIDGET_ENABLED = "widget_enabled"
        private const val REPLY_PRIVATELY_ALLOWED_SCOPE = "reply_private_allowed_scope"
    }

    fun setAPIKey(apiKey: String) {
        putPreference(API_KEY, apiKey)
    }

    fun getAPIKey(): String {
        return getPreference(API_KEY, "") ?: ""
    }

    fun setGifSupportEnabled(value: Boolean?) {
        putPreference(GIF_SUPPORT_ENABLED, value ?: false)
    }

    fun isGifSupportEnabled(): Boolean {
        return getPreference(GIF_SUPPORT_ENABLED, false)
    }

    fun setMicroPollsEnabled(value: Boolean?) {
        putPreference(MICRO_POLLS_ENABLED, value ?: false)
    }

    fun isMicroPollsEnabled(): Boolean {
        return getPreference(MICRO_POLLS_ENABLED, false)
    }

    fun setAudioSupportEnabled(value: Boolean?) {
        putPreference(AUDIO_SUPPORT_ENABLED, value ?: false)
    }

    fun isAudioSupportEnabled(): Boolean {
        return getPreference(AUDIO_SUPPORT_ENABLED, false)
    }

    fun setVoiceNoteSupportEnabled(value: Boolean?) {
        putPreference(VOICE_NOTE_ENABLED, value ?: false)
    }

    fun isVoiceNoteSupportEnabled(): Boolean {
        return getPreference(VOICE_NOTE_ENABLED, false)
    }

    fun getSlideUpVoiceNoteToast(): Boolean {
        return getPreference(SLIDE_UP_VOICE_NOTE_TOAST, true)
    }

    fun setSlideUpVoiceNoteToast(value: Boolean) {
        putPreference(SLIDE_UP_VOICE_NOTE_TOAST, value)
    }

    fun setIsWidgetEnabled(enabled: Boolean) {
        putPreference(WIDGET_ENABLED, enabled)
    }

    fun getIsWidgetEnabled(): Boolean {
        return getPreference(WIDGET_ENABLED, false)
    }

    fun setReplyPrivatelyAllowedScope(replyPrivatelyAllowedScope: String) {
        putPreference(REPLY_PRIVATELY_ALLOWED_SCOPE, replyPrivatelyAllowedScope)
    }

    fun getReplyPrivatelyAllowedScope(): String {
        return getPreference(REPLY_PRIVATELY_ALLOWED_SCOPE, "") ?: ""
    }

    fun setDefaultConfigPrefs() {
        setMicroPollsEnabled(true)
        setGifSupportEnabled(true)
        setAudioSupportEnabled(true)
        setVoiceNoteSupportEnabled(true)
    }
}