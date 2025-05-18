package com.likeminds.chatmm.aichatbot.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.LmChatAiBotInitiationActivityBinding
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class LMChatAIBotInitiationActivity : BaseAppCompatActivity() {

    private lateinit var binding: LmChatAiBotInitiationActivityBinding

    //Navigation
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    companion object {
        const val TAG = "LMChatAIBotInitiationActivity"

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, LMChatAIBotInitiationActivity::class.java)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(context: Context): Intent {
            val intent = Intent(context, LMChatAIBotInitiationActivity::class.java)
            return intent
        }
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().aiChatbotComponent()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LmChatAiBotInitiationActivityBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val innerPadding = windowInsets.getInsets(
                // Notice we're using systemBars, not statusBar
                WindowInsetsCompat.Type.systemBars()
                        // Notice we're also accounting for the display cutouts
                        or WindowInsetsCompat.Type.displayCutout()
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

        //Navigation
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        navController.setGraph(R.navigation.lm_chat_nav_graph_ai_bot_initiation)
    }
}