package com.likeminds.networking.chat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.dm.view.NetworkingChatFragment
import com.likeminds.chatmm.member.model.UserResponse
import com.likeminds.networking.chat.auth.util.AuthPreferences

class NetworkingChatActivity : AppCompatActivity() {

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_networking_chat)

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

        val chatFragment = NetworkingChatFragment.getInstance()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(containerViewId, chatFragment, containerViewId.toString())
        transaction.commit()
    }
}