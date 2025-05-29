package com.likeminds.chatmm.search.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.LmChatActivitySearchBinding
import com.likeminds.chatmm.search.model.LMChatSearchExtras
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class LMChatSearchActivity : BaseAppCompatActivity() {

    private lateinit var binding: LmChatActivitySearchBinding

    private var searchExtras: LMChatSearchExtras? = null

    //Navigation
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    companion object {
        const val LM_CHAT_SEARCH_EXTRAS = "LM_CHAT_SEARCH_EXTRAS"
        const val LM_CHAT_SEARCH_RESULT = "LM_CHAT_SEARCH_RESULT"

        fun start(context: Context, searchExtras: LMChatSearchExtras? = null) {
            val intent = Intent(context, LMChatSearchActivity::class.java)
            if (searchExtras != null) {
                val bundle = Bundle()
                bundle.putParcelable(LM_CHAT_SEARCH_EXTRAS, searchExtras)
                intent.putExtra("bundle", bundle)
            }
            context.startActivity(intent)
        }

        fun getIntent(context: Context, searchExtras: LMChatSearchExtras? = null): Intent {
            val intent = Intent(context, LMChatSearchActivity::class.java)
            if (searchExtras != null) {
                val bundle = Bundle()
                bundle.putParcelable(LM_CHAT_SEARCH_EXTRAS, searchExtras)
                intent.putExtra("bundle", bundle)
            }
            return intent
        }
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().searchComponent()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        SDKApplication.getInstance().searchComponent()?.inject(this)
        super.onCreate(savedInstanceState)

        binding = LmChatActivitySearchBinding.inflate(layoutInflater)

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

        val bundle = intent.getBundleExtra("bundle")

        if (bundle != null) {
            searchExtras = ExtrasUtil.getParcelable(
                bundle,
                LM_CHAT_SEARCH_EXTRAS,
                LMChatSearchExtras::class.java
            )

            val args = Bundle().apply {
                putParcelable(LM_CHAT_SEARCH_EXTRAS, searchExtras)
            }

            //Navigation
            navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.setGraph(R.navigation.lm_chat_nav_search, args)
        } else {
            //Navigation
            navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.setGraph(R.navigation.lm_chat_nav_search)
        }
    }
}