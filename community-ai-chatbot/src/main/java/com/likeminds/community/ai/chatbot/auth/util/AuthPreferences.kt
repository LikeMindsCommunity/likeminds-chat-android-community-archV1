package com.likeminds.community.ai.chatbot.auth.util

import android.content.Context
import android.content.SharedPreferences
import com.likeminds.community.ai.chatbot.auth.view.AuthActivity
import javax.inject.Singleton

@Singleton
class AuthPreferences(context: Context) {

    companion object {
        const val AUTH_PREFS = "auth_prefs"
        const val API_KEY = "api_key"
        const val USER_NAME = "user_name"
        const val USER_ID = "user_id"
        const val IS_LOGGED_IN = "is_logged_in"
        const val HEADER_COLOR = "header_color"
        const val BUTTON_COLOR = "button_color"
        const val TEXT_LINK_COLOR = "text_link_color"
    }

    private val preferences: SharedPreferences =
        context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)

    fun getApiKey(): String {
        return preferences.getString(API_KEY, "") ?: ""
    }

    fun saveApiKey(apiKey: String) {
        preferences.edit().putString(API_KEY, apiKey).apply()
    }

    fun getUserName(): String {
        return preferences.getString(USER_NAME, "") ?: ""
    }

    fun saveUserName(userName: String) {
        preferences.edit().putString(USER_NAME, userName).apply()
    }

    fun getUserId(): String {
        return preferences.getString(USER_ID, "") ?: ""
    }

    fun saveUserId(userId: String) {
        preferences.edit().putString(USER_ID, userId).apply()
    }

    fun getIsLoggedIn(): Boolean {
        return preferences.getBoolean(IS_LOGGED_IN, false)
    }

    fun saveIsLoggedIn(isLoggedIn: Boolean) {
        preferences.edit().putBoolean(IS_LOGGED_IN, isLoggedIn).apply()
    }

    fun saveHeaderColor(headerColor: String) {
        preferences.edit().putString(HEADER_COLOR, headerColor).apply()
    }

    fun getHeaderColor(): String {
        return preferences.getString(HEADER_COLOR, AuthActivity.DEFAULT_HEADER_COLOR) ?: ""
    }

    fun saveButtonColor(buttonColor: String) {
        preferences.edit().putString(BUTTON_COLOR, buttonColor).apply()
    }

    fun getButtonColor(): String {
        return preferences.getString(BUTTON_COLOR, AuthActivity.DEFAULT_BUTTON_COLOR) ?: ""
    }

    fun saveTextLinkColor(textLinkColor: String) {
        preferences.edit().putString(TEXT_LINK_COLOR, textLinkColor).apply()
    }

    fun getTextLinkColor(): String {
        return preferences.getString(TEXT_LINK_COLOR, AuthActivity.DEFAULT_TEXT_LINK) ?: ""
    }

    fun clearPrefs() {
        saveApiKey("")
        saveUserName("")
        saveUserId("")
        saveIsLoggedIn(false)
    }
}