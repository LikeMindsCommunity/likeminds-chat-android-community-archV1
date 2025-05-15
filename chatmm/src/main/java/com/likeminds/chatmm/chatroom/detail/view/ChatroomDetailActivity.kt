package com.likeminds.chatmm.chatroom.detail.view

import android.app.NotificationManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.databinding.ActivityChatroomDetailBinding
import com.likeminds.chatmm.utils.ErrorUtil.emptyExtrasException
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.BaseAppCompatActivity

class ChatroomDetailActivity : BaseAppCompatActivity() {

    private lateinit var binding: ActivityChatroomDetailBinding

    private lateinit var chatroomDetailExtras: ChatroomDetailExtras

    //Navigation
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var navController: NavController

    companion object {
        const val CHATROOM_DETAIL_EXTRAS = "CHATROOM_DETAIL_EXTRAS"
        const val FRAGMENT_TAG_CHATROOM_DETAIL = "FRAGMENT_TAG_CHATROOM_DETAIL"
        const val TAG = "ChatroomDetailActivity"

        @JvmStatic
        fun start(
            context: Context,
            chatroomDetailExtras: ChatroomDetailExtras,
            clipData: ClipData? = null,
        ) {
            val intent = Intent(context, ChatroomDetailActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(CHATROOM_DETAIL_EXTRAS, chatroomDetailExtras)
            intent.putExtra("bundle", bundle)
            if (clipData != null) {
                intent.clipData = clipData
            }
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        }

        @JvmStatic
        fun getIntent(context: Context, chatroomDetailExtras: ChatroomDetailExtras): Intent {
            val intent = Intent(context, ChatroomDetailActivity::class.java)
            val bundle = Bundle()
            bundle.putParcelable(CHATROOM_DETAIL_EXTRAS, chatroomDetailExtras)
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

        binding = ActivityChatroomDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bundle = intent.getBundleExtra("bundle")

        if (bundle != null) {
            chatroomDetailExtras = ExtrasUtil.getParcelable(
                bundle,
                CHATROOM_DETAIL_EXTRAS,
                ChatroomDetailExtras::class.java
            ) ?: throw emptyExtrasException(TAG)

            val args = Bundle().apply {
                putParcelable(CHATROOM_DETAIL_EXTRAS, chatroomDetailExtras)
            }

            cancelNotification(chatroomDetailExtras.notificationId)

            //Navigation
            navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            navController = navHostFragment.navController
            navController.setGraph(R.navigation.lm_chat_nav_graph_chatroom_detail, args)
        } else {
            redirectActivity(true)
        }
    }

    // cancels the notification if screen is opened from notification
    private fun cancelNotification(notificationId: Int?) {
        if (notificationId != null) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(notificationId)
        }
    }

    override fun onStart() {
        super.onStart()
        val sdkApplication = SDKApplication.getInstance()
        sdkApplication.openedChatroomId = chatroomDetailExtras.chatroomId
    }

    override fun onPause() {
        val sdkApplication = SDKApplication.getInstance()
        sdkApplication.openedChatroomId = null
        super.onPause()
    }

    override fun onBackPressed() {
        val chatroomDetailFragment = getChatroomDetailFragment()

        when {
            ViewUtils.getFragmentVisible(chatroomDetailFragment) -> {
                when {
                    chatroomDetailFragment?.consumeTouch() == true -> {
                        return
                    }

                    chatroomDetailExtras.isFromSearchChatroom == true -> {
                        sendSearchChatroomClosedEvent()
                        redirectActivity(false)
                    }

                    chatroomDetailExtras.isFromSearchMessage == true -> {
                        sendSearchMessageClosedEvent()
                        redirectActivity(false)
                    }

                    else -> {
                        chatroomDetailFragment?.setChatroomDetailActivityResult()
                        redirectActivity(false)
                    }
                }
            }

            else -> {
                redirectActivity(false)
            }
        }
    }

    private fun getChatroomDetailFragment(): ChatroomDetailFragment? {
        val chatroomDetailFragment = supportFragmentManager.findFragmentByTag(
            FRAGMENT_TAG_CHATROOM_DETAIL
        )
        return if (chatroomDetailFragment != null && chatroomDetailFragment is ChatroomDetailFragment) {
            chatroomDetailFragment
        } else {
            null
        }
    }

    fun redirectActivity(isError: Boolean) {
        if (isError) {
            ViewUtils.showShortToast(
                this,
                getString(R.string.lm_chat_the_chatroom_link_is_either_tampered_or_invalid)
            )
        }

        if (isTaskRoot && SDKApplication.launcherIntent != null) {
            supportFragmentManager.popBackStack()
            startActivity(SDKApplication.launcherIntent)
            overridePendingTransition(
                R.anim.lm_chat_slide_from_left,
                R.anim.lm_chat_slide_to_right
            )
            finish()
        } else {
            supportFragmentManager.popBackStack()
            super.onBackPressed()
            overridePendingTransition(
                R.anim.lm_chat_slide_from_left,
                R.anim.lm_chat_slide_to_right
            )
        }
    }

    // triggers an event when chatroom search is closed
    private fun sendSearchChatroomClosedEvent() {
        LMAnalytics.track(
            LMAnalytics.Events.CHATROOM_SEARCH_CLOSED,
            mapOf(
                LMAnalytics.Keys.CHATROOM_ID to chatroomDetailExtras.chatroomId,
                LMAnalytics.Keys.COMMUNITY_ID to chatroomDetailExtras.communityId
            )
        )
    }

    // triggers an event when message search is closed
    private fun sendSearchMessageClosedEvent() {
        LMAnalytics.track(
            LMAnalytics.Events.MESSAGE_SEARCH_CLOSED,
            mapOf(
                LMAnalytics.Keys.CHATROOM_ID to chatroomDetailExtras.chatroomId,
                LMAnalytics.Keys.COMMUNITY_ID to chatroomDetailExtras.communityId
            )
        )
    }
}