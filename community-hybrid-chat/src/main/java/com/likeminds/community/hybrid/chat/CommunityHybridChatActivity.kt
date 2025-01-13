package com.likeminds.community.hybrid.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.chat.view.CommunityHybridChatFragment
import com.likeminds.chatmm.member.model.UserResponse
import com.likeminds.community.hybrid.chat.auth.util.AuthPreferences

class CommunityHybridChatActivity : AppCompatActivity() {

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_community_hybrid_chat)

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

        val chatFragment = CommunityHybridChatFragment.getInstance()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(containerViewId, chatFragment, containerViewId.toString())
        transaction.commit()
    }
}