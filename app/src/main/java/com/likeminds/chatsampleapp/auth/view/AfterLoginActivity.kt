package com.likeminds.chatsampleapp.auth.view

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.chat.view.LMChatFragment
import com.likeminds.chatmm.member.model.UserResponse
import com.likeminds.chatsampleapp.ChatMMApplication
import com.likeminds.chatsampleapp.R
import com.likeminds.chatsampleapp.auth.model.LoginExtras
import com.likeminds.chatsampleapp.auth.util.AuthPreferences
import com.likeminds.chatsampleapp.databinding.ActivityAfterLoginBinding

class AfterLoginActivity : AppCompatActivity() {

    private var extra: LoginExtras? = null

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAfterLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extra = intent.getParcelableExtra(ChatMMApplication.EXTRA_LOGIN)
        if (extra == null) {
            finish()
        }

        initCommunityTab()
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.community_tab -> {
                    initCommunityTab()
                }

                R.id.user -> {
                    initUserFragment()
                }
            }
            return@setOnItemSelectedListener true
        }
    }

    private fun initUserFragment() {
        val fragment = UserFragment.getInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment, "UserFragment")
            .commit()
    }

    private fun initCommunityTab() {
        val successCallback = { userResponse: UserResponse ->
            Log.d("PUI", "${userResponse.user?.id}")
            replaceFragment()
        }

        val errorCallback = { error: String? ->
            Log.d("PUI", "$error")
            Unit
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
        val containerViewId = R.id.frameLayout

        val chatFragment = LMChatFragment.getInstance()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(containerViewId, chatFragment, containerViewId.toString())
        transaction.commit()
    }
}