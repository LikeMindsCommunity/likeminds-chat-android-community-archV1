package com.likeminds.networking.chat.auth.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.SDKApplication.Companion.LOG_TAG
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.likemindschat.user.model.LogoutRequest
import com.likeminds.networking.chat.NetworkingChatActivity
import com.likeminds.networking.chat.NetworkingChatApplication
import com.likeminds.networking.chat.auth.model.LoginExtras
import com.likeminds.networking.chat.auth.util.AuthPreferences
import com.likeminds.networking.chat.databinding.ActivityAfterLoginBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AfterLoginActivity : AppCompatActivity() {

    private var extra: LoginExtras? = null

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityAfterLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        extra = ExtrasUtil.getParcelable(
            intent.extras,
            NetworkingChatApplication.EXTRA_LOGIN,
            LoginExtras::class.java
        )

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnStartChat.setOnClickListener {
            val intent = Intent(this, NetworkingChatActivity::class.java)
            startActivity(intent)
        }

    }

    private fun logout() {
        CoroutineScope(Dispatchers.IO).launch {
            val logoutResponse = LMChatCore.logoutUser(
                this@AfterLoginActivity,
                LogoutRequest.Builder()
                    .deviceId(deviceId())
                    .build()
            )
            if (logoutResponse.success) {
                authPreferences.clearPrefs()
                val intent = Intent(this@AfterLoginActivity, AuthActivity::class.java)
                finish()
                startActivity(intent)
            } else {
                Log.e(LOG_TAG, "logout failed error: ${logoutResponse.errorMessage}")
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun deviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: ""
    }
}