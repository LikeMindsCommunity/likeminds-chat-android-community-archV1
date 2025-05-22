package com.likeminds.chatmm.chatroom.detail.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.chatroom.detail.model.ViewParticipantsExtras
import com.likeminds.chatmm.databinding.ActivityViewParticipantsBinding
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class ViewParticipantsActivity : BaseAppCompatActivity() {

    private lateinit var binding: ActivityViewParticipantsBinding

    private var viewParticipantsExtras: ViewParticipantsExtras? = null

    //Navigation
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    companion object {
        const val VIEW_PARTICIPANTS_EXTRAS = "VIEW_PARTICIPANTS_EXTRAS"

        @JvmStatic
        fun start(context: Context, extra: ViewParticipantsExtras) {
            val intent = Intent(context, ViewParticipantsActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(VIEW_PARTICIPANTS_EXTRAS, extra)
            intent.putExtra("bundle", bundle)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(context: Context, extra: ViewParticipantsExtras): Intent {
            val intent = Intent(context, ViewParticipantsActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(VIEW_PARTICIPANTS_EXTRAS, extra)
            intent.putExtra("bundle", bundle)
            return intent
        }
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().chatroomDetailComponent()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityViewParticipantsBinding.inflate(layoutInflater)

        ViewCompat.setOnApplyWindowInsetsListener(binding.navHostFragment) { view, windowInsets ->
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
            viewParticipantsExtras = ExtrasUtil.getParcelable(
                bundle,
                VIEW_PARTICIPANTS_EXTRAS,
                ViewParticipantsExtras::class.java
            )
            val args = Bundle().apply {
                putParcelable(VIEW_PARTICIPANTS_EXTRAS, viewParticipantsExtras)
            }

            //Navigation
            navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.setGraph(R.navigation.lm_chat_nav_graph_view_participants, args)
        } else {
            redirectActivity(true)
        }
    }

    private fun redirectActivity(isError: Boolean) {
        if (isError) {
            ViewUtils.showShortToast(this, "The chatroom link is either tampered or invalid")
        }
        supportFragmentManager.popBackStack()
        super.onBackPressed()
        overridePendingTransition(
            R.anim.lm_chat_slide_from_left,
            R.anim.lm_chat_slide_to_right
        )
    }
}