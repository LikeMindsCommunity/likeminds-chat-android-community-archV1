package com.likeminds.community.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.homefeed.view.CommunityChatFragment
import com.likeminds.chatmm.member.model.UserResponse
import com.likeminds.community.chat.auth.util.AuthPreferences

class CommunityChatActivity : AppCompatActivity() {

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Call before the DecorView is accessed in setContentView
        theme.applyStyle(com.likeminds.chatmm.R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)

        setContentView(R.layout.activity_community_chat)

        val successCallback = { userResponse: UserResponse ->
            replaceFragment()
        }

        val errorCallback = { error: String? ->

        }
        LMChatCore.showChat(
            this,
            apiKey = authPreferences.getApiKey(),
            userName = authPreferences.getUserName(),
            uuid = authPreferences.getUserId(),
            successCallback,
            errorCallback
        )
    }

    private fun replaceFragment() {
        val containerViewId = R.id.frame_layout

        val chatFragment = CommunityChatFragment.getInstance()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(containerViewId, chatFragment, containerViewId.toString())
        transaction.commit()
    }
}