package com.likeminds.chatmm.chat.view

import android.Manifest
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayoutMediator
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.chat.adapter.ChatPagerAdapter
import com.likeminds.chatmm.chat.viewmodel.ChatViewModel
import com.likeminds.chatmm.databinding.FragmentCommunityHybridChatBinding
import com.likeminds.chatmm.dm.model.CheckDMTabViewData
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.MemberImageUtil
import com.likeminds.chatmm.search.view.LMChatSearchActivity
import com.likeminds.chatmm.theme.model.LMChatAppearance
import com.likeminds.chatmm.utils.connectivity.ConnectivityBroadcastReceiver
import com.likeminds.chatmm.utils.connectivity.ConnectivityReceiverListener
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.snackbar.CustomSnackBar
import javax.inject.Inject
import kotlin.math.roundToInt

class CommunityHybridChatFragment :
    BaseFragment<FragmentCommunityHybridChatBinding, ChatViewModel>(),
    ConnectivityReceiverListener {

    var dmMeta: CheckDMTabViewData? = null
    private val connectivityBroadcastReceiver by lazy {
        ConnectivityBroadcastReceiver()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    @Inject
    lateinit var snackBar: CustomSnackBar

    private var wasNetworkGone = false

    companion object {
        const val CHAT_EXTRAS = "chat_extras"
        const val TAG = "ChatFragment"

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS

        fun getInstance(): CommunityHybridChatFragment {
            val fragment = CommunityHybridChatFragment()
            return fragment
        }
    }

    private lateinit var pagerAdapter: ChatPagerAdapter

    override fun getViewModelClass(): Class<ChatViewModel> {
        return ChatViewModel::class.java
    }

    override fun getViewBinding(): FragmentCommunityHybridChatBinding {
        return FragmentCommunityHybridChatBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().chatComponent()?.inject(this)
    }

    override fun setUpViews() {
        super.setUpViews()
        checkForNotificationPermission()
        initTabLayout()
        setTheme()
        setupReceivers()
        initToolbar()
        initPagerAdapter()
        initData()
    }

    override fun observeData() {
        super.observeData()
        // observes [userData] ;ive data
        viewModel.userData.observe(viewLifecycleOwner) { user ->
            observeUserData(user)
        }

        // observes [checkDMTabResponse] live data
        viewModel.checkDMTabResponse.observe(viewLifecycleOwner) { response ->
            if (response != null) {
                setDMMeta(response)
//                updateUnreadDMCount(response.unreadDMCount)
            }
        }
    }

    //check permission for Post Notifications
    private fun checkForNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationManagerCompat.from(requireContext()).areNotificationsEnabled()) {
                if (activity?.checkSelfPermission(POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    notificationPermissionLauncher.launch(POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun initTabLayout() {
        binding.tabChat.apply {
            setSelectedTabIndicatorColor(LMChatAppearance.getButtonsColor())
            setTabTextColors(Color.GRAY, LMChatAppearance.getButtonsColor())
        }
    }

    private fun setTheme() {
        binding.apply {
            toolbarColor = LMChatAppearance.getToolbarColor()
        }
    }

    //register receivers to the activity
    private fun setupReceivers() {
        connectivityBroadcastReceiver.setListener(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity?.registerReceiver(
                connectivityBroadcastReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION),
                Context.RECEIVER_EXPORTED
            )
        } else {
            activity?.registerReceiver(
                connectivityBroadcastReceiver,
                IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            )
        }
    }

    private fun initToolbar() {
        binding.apply {
            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

            //if user is guest user hide, profile icon from toolbar
            memberImage.isVisible = !isGuestUser

            //get user from local db
            viewModel.getUserFromLocalDb()

            ivSearch.setOnClickListener {
                LMChatSearchActivity.start(requireContext())
                Log.d(SDKApplication.LOG_TAG, "search started")
            }
        }
    }

    // calls api to initiate data
    private fun initData() {
        viewModel.checkDMTab()
        initToolbar()
    }

    //init tab adapter and perform operations are per selected tab
    private fun initPagerAdapter() {
        binding.viewPager.apply {
            (getChildAt(0) as? RecyclerView)?.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
            pagerAdapter = ChatPagerAdapter(this@CommunityHybridChatFragment)
            adapter = pagerAdapter
        }

        TabLayoutMediator(binding.tabChat, binding.viewPager) { tab, position ->
            when (position) {
                0 -> {
                    tab.apply {
                        text = getString(R.string.lm_chat_groups)
                        removeBadge()
                    }
                }

                1 -> {
                    tab.apply {
                        text = getString(R.string.lm_chat_dms)
                        removeBadge()
//                        val unreadDMCount = dmMeta?.unreadDMCount ?: 0
//                        if (unreadDMCount > 0) {
//                            val badge = orCreateBadge
//                            badge.apply {
//                                horizontalOffset =
//                                    resources.getDimension(R.dimen.lm_chat_dm_badge_horizontal_margin)
//                                        .roundToInt()
//                                verticalOffset =
//                                    resources.getDimension(R.dimen.lm_chat_dm_badge_vertical_margin)
//                                        .roundToInt()
//
//                                number = unreadDMCount
//                                maxCharacterCount = 2
//                                backgroundColor = LMTheme.getButtonsColor()
//
//                                badgeTextColor =
//                                    ContextCompat.getColor(requireContext(), R.color.lm_chat_white)
//                            }
//                        } else {
//                            removeBadge()
//                        }
                    }
                }

                else -> {
                    throw IndexOutOfBoundsException()
                }
            }
        }.attach()
    }

    private fun setDMMeta(checkDMTabViewData: CheckDMTabViewData) {
        dmMeta = checkDMTabViewData
    }

    //observe user data
    private fun observeUserData(user: MemberViewData?) {
        if (user != null) {
            MemberImageUtil.setImage(
                user.imageUrl,
                user.name,
                user.sdkClientInfo.uuid,
                binding.memberImage,
                showRoundImage = true,
                objectKey = user.updatedAt
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkDMTab()
    }

    //update the unread count on dm tab
    private fun updateUnreadDMCount(unreadDMCount: Int) {
        val dmTab = binding.tabChat.getTabAt(1)
        dmTab?.let {
            if (unreadDMCount > 0) {
                val badge = it.orCreateBadge
                badge.apply {
                    horizontalOffset =
                        resources.getDimension(R.dimen.lm_chat_dm_badge_horizontal_margin)
                            .roundToInt()
                    verticalOffset =
                        resources.getDimension(R.dimen.lm_chat_dm_badge_vertical_margin)
                            .roundToInt()
                    number = unreadDMCount
                    maxCharacterCount = 2
                    backgroundColor = LMChatAppearance.getButtonsColor()
                    badgeTextColor =
                        ContextCompat.getColor(requireContext(), R.color.lm_chat_white)
                }
            } else {
                it.removeBadge()
            }
        }
    }

    override fun onNetworkConnectionChanged(isConnected: Boolean) {
        val parentView = activity?.findViewById<ViewGroup>(android.R.id.content) ?: return
        if (parentView.childCount > 0) {
            parentView.getChildAt(0)?.let { view ->
                if (isConnected && wasNetworkGone) {
                    wasNetworkGone = false
                    snackBar.showMessage(
                        view,
                        getString(R.string.lm_chat_internet_connection_restored),
                        true
                    )
                }
                if (!isConnected) {
                    wasNetworkGone = true
                    snackBar.showNoInternet(view)
                }
            }
        }
    }
}