package com.likeminds.community.chat.auth.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication.Companion.LOG_TAG
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.community.chat.CommunityChatActivity
import com.likeminds.community.chat.CommunityChatApplication
import com.likeminds.community.chat.auth.model.LoginExtras
import com.likeminds.community.chat.auth.util.AuthPreferences
import com.likeminds.community.chat.databinding.ActivityAfterLoginBinding
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.user.model.EditUserProfileRequest
import com.likeminds.likemindschat.user.model.LogoutRequest
import kotlinx.coroutines.*

class AfterLoginActivity : AppCompatActivity() {

    private var extra: LoginExtras? = null

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityAfterLoginBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val innerPadding = windowInsets.getInsets(
                // Notice we're using systemBars, not statusBar
                WindowInsetsCompat.Type.systemBars()
                        // Notice we're also accounting for the display cutouts
                        or WindowInsetsCompat.Type.displayCutout()
                        // If using EditText, also add
                        or WindowInsetsCompat.Type.ime()
            )
            // Apply the insets as padding to the view. Here, set all the dimensions
            // as appropriate to your layout. You can also update the view's margin if
            // more appropriate.
            view.setPadding(0, innerPadding.top, 0, innerPadding.bottom)

            // Return CONSUMED if you don't want the window insets to keep passing down
            // to descendant views.
            WindowInsetsCompat.CONSUMED
        }

        setContentView(binding.root)

        extra = ExtrasUtil.getParcelable(
            intent.extras,
            CommunityChatApplication.EXTRA_LOGIN,
            LoginExtras::class.java
        )

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnStartChat.setOnClickListener {
            val intent = Intent(this, CommunityChatActivity::class.java)
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

    fun editProfile() {
        CoroutineScope(Dispatchers.IO).launch {
            val lmChatClient = LMChatClient.getInstance()

            val editProfileRequest = EditUserProfileRequest.Builder()
                .name("EDITED_NAME")
                .imageUrl("NEW_IMAGE_URL")
                .build()

            val response = lmChatClient.editUserProfile(editProfileRequest)

            if (response.success) {
                //handle success of the edit profile request
            } else {
                //handle failure of the edit profile request
            }
        }
    }

    @SuppressLint("HardwareIds")
    fun deviceId(): String {
        return Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            ?: ""
    }
}