package com.likeminds.chatmm.chatroom.explore.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.ActivityChatroomExploreBinding
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class ChatroomExploreActivity : BaseAppCompatActivity() {

    private lateinit var binding: ActivityChatroomExploreBinding

    //Navigation
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, ChatroomExploreActivity::class.java)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(context: Context): Intent {
            return Intent(context, ChatroomExploreActivity::class.java)
        }
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().exploreComponent()?.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Call before the DecorView is accessed in setContentView
        theme.applyStyle(R.style.OptOutEdgeToEdgeEnforcement, /* force */ false)

        binding = ActivityChatroomExploreBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Navigation
        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        navController.setGraph(R.navigation.lm_chat_nav_graph_chatroom_explore, intent.extras)
    }
}