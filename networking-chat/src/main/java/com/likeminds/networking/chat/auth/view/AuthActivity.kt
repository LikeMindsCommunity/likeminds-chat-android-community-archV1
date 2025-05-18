package com.likeminds.networking.chat.auth.view

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog
import com.github.dhaval2404.colorpicker.model.ColorShape
import com.github.dhaval2404.colorpicker.model.ColorSwatch
import com.likeminds.chatmm.LMChatCore
import com.likeminds.chatmm.theme.model.LMChatAppearanceRequest
import com.likeminds.chatmm.theme.model.LMFonts
import com.likeminds.chatmm.utils.Route
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.networking.chat.NetworkingChatApplication
import com.likeminds.networking.chat.R
import com.likeminds.networking.chat.auth.model.LoginExtras
import com.likeminds.networking.chat.auth.util.AuthPreferences
import com.likeminds.networking.chat.databinding.ActivityAuthBinding

class AuthActivity : AppCompatActivity() {

    private val authPreferences: AuthPreferences by lazy {
        AuthPreferences(this)
    }

    private lateinit var binding: ActivityAuthBinding

    private var headerColor = DEFAULT_HEADER_COLOR
    private var buttonColor = DEFAULT_BUTTON_COLOR
    private var textLinkColor = DEFAULT_TEXT_LINK

    companion object {
        const val DEFAULT_HEADER_COLOR = "#FFFFFF"
        const val DEFAULT_BUTTON_COLOR = "#00897B"
        const val DEFAULT_TEXT_LINK = "#007AFF"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAuthBinding.inflate(layoutInflater)

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

        val isLoggedIn = authPreferences.getIsLoggedIn()

        if (isLoggedIn) {
            // user already logged in, navigate using deep linking or to [MainActivity]
            if (intent.data != null) {
                parseDeepLink()
            } else {
                navigateToAfterLoginActivity()
            }
        } else {
            // user is not logged in, ask login details.
            loginUser()
        }
    }

    private fun navigateToAfterLoginActivity() {
        val intent = Intent(this, AfterLoginActivity::class.java)
        val extras = LoginExtras(
            false,
            authPreferences.getUserName(),
            authPreferences.getUserId(),
        )
        intent.putExtra(NetworkingChatApplication.EXTRA_LOGIN, extras)
        startActivity(intent)
        finish()
    }

    // parses deep link to start corresponding activity
    private fun parseDeepLink() {
        //get intent for route
        val intent = Route.handleDeepLink(
            this,
            intent.data.toString()
        )
        startActivity(intent)
        finish()
    }

    // validates user input and save login details
    private fun loginUser() {
        binding.apply {
            val context = root.context

            ivBrandingButton.setOnClickListener {
                showColorDialog { colorRes, colorHex ->
                    buttonColor = colorHex
                    ivBrandingButton.backgroundTintList = ColorStateList.valueOf(colorRes)
                }
            }

            ivBrandingHeader.setOnClickListener {
                showColorDialog { colorRes, colorHex ->
                    headerColor = colorHex
                    ivBrandingHeader.backgroundTintList = ColorStateList.valueOf(colorRes)
                }
            }

            ivBrandingTextLink.setOnClickListener {
                showColorDialog { colorRes, colorHex ->
                    textLinkColor = colorHex
                    ivBrandingTextLink.backgroundTintList = ColorStateList.valueOf(colorRes)
                }
            }

            btnLogin.setOnClickListener {
                val apiKey = etApiKey.text.toString().trim()
                val userName = etUserName.text.toString().trim()
                val userId = etUserId.text.toString().trim()

                if (apiKey.isEmpty()) {
                    ViewUtils.showShortToast(
                        context,
                        getString(R.string.enter_api_key)
                    )
                    return@setOnClickListener
                }

                if (userName.isEmpty()) {
                    ViewUtils.showShortToast(
                        context,
                        getString(R.string.enter_user_name)
                    )
                    return@setOnClickListener
                }

                if (userId.isEmpty()) {
                    ViewUtils.showShortToast(
                        context,
                        getString(R.string.enter_user_id)
                    )
                    return@setOnClickListener
                }

                // save login details to auth prefs
                authPreferences.saveIsLoggedIn(true)
                authPreferences.saveApiKey(apiKey)
                authPreferences.saveUserName(userName)
                authPreferences.saveUserId(userId)

                authPreferences.saveHeaderColor(headerColor)
                authPreferences.saveButtonColor(buttonColor)
                authPreferences.saveTextLinkColor(textLinkColor)

                val brandingRequest = LMChatAppearanceRequest.Builder()
                    .headerColor(headerColor)
                    .buttonsColor(buttonColor)
                    .textLinkColor(textLinkColor)
                    .fonts(
                        LMFonts.Builder()
                            .bold(com.likeminds.chatmm.R.font.lm_chat_roboto_bold)
                            .medium(com.likeminds.chatmm.R.font.lm_chat_roboto_regular)
                            .regular(com.likeminds.chatmm.R.font.lm_chat_roboto_regular)
                            .build()
                    )
                    .build()

                LMChatCore.setTheme(brandingRequest)
                navigateToAfterLoginActivity()
            }
        }
    }

    private fun showColorDialog(cb: (Int, String) -> Unit) {
        MaterialColorPickerDialog
            .Builder(this)
            .setTitle("Pick Theme")
            .setColorShape(ColorShape.SQAURE)
            .setColorSwatch(ColorSwatch._300)
            .setColorListener { colorRes, colorHex ->
                cb(colorRes, colorHex)
            }
            .show()
    }
}