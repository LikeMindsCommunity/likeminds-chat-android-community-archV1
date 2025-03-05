package com.likeminds.chatmm.member.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.LmChatActivityCommunityMembersBinding
import com.likeminds.chatmm.member.model.CommunityMembersExtras
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class LMChatCommunityMembersActivity : BaseAppCompatActivity() {

    private lateinit var binding: LmChatActivityCommunityMembersBinding

    private var communityMembersExtra: CommunityMembersExtras? = null

    //Navigation
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    companion object {
        const val COMMUNITY_MEMBERS_EXTRAS = "COMMUNITY_MEMBERS_EXTRAS"

        fun start(context: Context, extras: CommunityMembersExtras) {
            val intent = Intent(context, LMChatCommunityMembersActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(COMMUNITY_MEMBERS_EXTRAS, extras)
            intent.putExtra("bundle", bundle)
            context.startActivity(intent)
        }

        fun getIntent(context: Context, extras: CommunityMembersExtras): Intent {
            val intent = Intent(context, LMChatCommunityMembersActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(COMMUNITY_MEMBERS_EXTRAS, extras)
            intent.putExtra("bundle", bundle)
            return intent
        }
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().memberComponent()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Call before the DecorView is accessed in setContentView
        theme.applyStyle(R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)

        binding = LmChatActivityCommunityMembersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.getBundleExtra("bundle")

        if (bundle != null) {
            communityMembersExtra = ExtrasUtil.getParcelable(
                bundle,
                COMMUNITY_MEMBERS_EXTRAS,
                CommunityMembersExtras::class.java
            )
            val args = Bundle().apply {
                putParcelable(COMMUNITY_MEMBERS_EXTRAS, communityMembersExtra)
            }

            //Navigation
            navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.setGraph(R.navigation.lm_chat_nav_graph_community_members, args)
        } else {
            redirectActivity(true)
        }
    }

    private fun redirectActivity(isError: Boolean) {
        if (isError) {
            ViewUtils.showShortToast(
                this,
                getString(R.string.lm_chat_something_went_wrong)
            )
        }
        supportFragmentManager.popBackStack()
        super.onBackPressed()
        overridePendingTransition(
            R.anim.lm_chat_slide_from_left,
            R.anim.lm_chat_slide_to_right
        )
    }
}