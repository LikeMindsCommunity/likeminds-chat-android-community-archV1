package com.likeminds.chatmm.homefeed.view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import com.likeminds.chatmm.*
import com.likeminds.chatmm.SDKApplication.Companion.LOG_TAG
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.model.ChatroomViewData
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailFragment
import com.likeminds.chatmm.chatroom.explore.view.ChatroomExploreActivity
import com.likeminds.chatmm.community.utils.LMChatCommunitySettingsUtil
import com.likeminds.chatmm.databinding.FragmentHomeFeedBinding
import com.likeminds.chatmm.homefeed.model.*
import com.likeminds.chatmm.homefeed.util.HomeFeedPreferences
import com.likeminds.chatmm.homefeed.view.adapter.HomeFeedAdapter
import com.likeminds.chatmm.homefeed.view.adapter.HomeFeedAdapterListener
import com.likeminds.chatmm.homefeed.viewmodel.HomeFeedViewModel
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.MemberImageUtil
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.pushnotification.viewmodel.LMNotificationViewModel
import com.likeminds.chatmm.search.view.LMChatSearchActivity
import com.likeminds.chatmm.theme.model.LMTheme
import com.likeminds.chatmm.utils.ValueUtils.isValidIndex
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.connectivity.ConnectivityReceiverListener
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.observeInLifecycle
import com.likeminds.chatmm.utils.snackbar.CustomSnackBar
import com.likeminds.likemindschat.chatroom.model.ChannelInviteStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Timer
import javax.inject.Inject
import kotlin.concurrent.schedule

class HomeFeedFragment : BaseFragment<FragmentHomeFeedBinding, HomeFeedViewModel>(),
    HomeFeedAdapterListener,
    ConnectivityReceiverListener,
    JoinChatroomInviteDialogListener {

    private lateinit var homeFeedAdapter: HomeFeedAdapter

    @Inject
    lateinit var snackBar: CustomSnackBar

    @Inject
    lateinit var lmNotificationViewModel: LMNotificationViewModel

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var homeFeedPreferences: HomeFeedPreferences

    private var wasNetworkGone = false

    private var communityId: String = ""
    private var communityName: String = ""

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    companion object {
        const val TAG = "HomeFeedFragment"
        private const val BUNDLE_HOME_FRAGMENT = "bundle of home fragment"

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val POST_NOTIFICATIONS = Manifest.permission.POST_NOTIFICATIONS

        /**
         * creates a instance of fragment
         **/
        @JvmStatic
        fun getInstance(
        ): HomeFeedFragment {
            val fragment = HomeFeedFragment()
            return fragment
        }
    }

    override fun getViewModelClass(): Class<HomeFeedViewModel> {
        return HomeFeedViewModel::class.java
    }

    override fun getViewBinding(): FragmentHomeFeedBinding {
        return FragmentHomeFeedBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().homeFeedComponent()?.inject(this)
    }

    override fun setUpViews() {
        super.setUpViews()
        checkForNotificationPermission()

        Log.d(
            "PUI", """
            home feed preferences
            SHOW_HOME_FEED_SHIMMER:${homeFeedPreferences.getShowHomeFeedShimmer()}
        """.trimIndent()
        )
        setTheme()
        initData()
        initRecyclerView()
        initToolbar()
        fetchData()
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

    override fun observeData() {
        super.observeData()

        observeErrors()

        viewModel.userData.observe(viewLifecycleOwner) { user ->
            observeUserData(user)
        }

        viewModel.updateChannelInviteStatus.observe(viewLifecycleOwner) { channelInviteStatus ->
            updateChannelInviteStatus(channelInviteStatus)
        }

        viewModel.homeEventsFlow.onEach { homeEvent ->
            when (homeEvent) {
                is HomeFeedViewModel.HomeEvent.UpdateChatrooms -> {
                    updateChatrooms()
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)
    }

    // observes error message
    private fun observeErrors() {
        viewModel.errorMessageEventFlow.onEach { response ->
            when (response) {
                is HomeFeedViewModel.ErrorMessageEvent.GetChatroom -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is HomeFeedViewModel.ErrorMessageEvent.GetExploreTabCount -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is HomeFeedViewModel.ErrorMessageEvent.GetChannelInvites -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is HomeFeedViewModel.ErrorMessageEvent.UpdateChannelInvite -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)
    }

    //observe user data
    private fun observeUserData(user: MemberViewData?) {
        if (user != null) {
            communityId = user.communityId ?: ""
            communityName = user.communityName ?: ""

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

    // initializes home feed data
    private fun initData() {
        initToolbar()
        fetchData()
        startSync()

        //only get invites, when setting is enabled.
        if (LMChatCommunitySettingsUtil.isSecretChatroomInviteEnabled()) {
            viewModel.getChannelInvites()
        }

        viewModel.getExploreTabCount()
        viewModel.sendCommunityTabClicked(communityId, communityName)
        viewModel.sendHomeScreenOpenedEvent(LMAnalytics.Source.COMMUNITY_TAB)
    }

    // shows invalid access error and logs out invalid user
    private fun showInvalidAccess() {
        binding.apply {
            rvHomeFeed.hide()
            layoutRemoveAccess.root.show()
            memberImage.hide()
            ivSearch.hide()
        }
    }

    private fun updateChatrooms() {
        homeFeedAdapter.setItemsViaDiffUtilForHome(
            viewModel.getHomeFeedList(requireContext())
        )
    }

    // shows the toast message as per update in channel invite and remove the invite chatroom
    private fun updateChannelInviteStatus(channelInviteStatus: Pair<String, ChannelInviteStatus>) {
        // remove the channel invitation from the adapter
        val channelId = channelInviteStatus.first
        val index = homeFeedAdapter.items().indexOfFirst {
            it is ChannelInviteViewData && it.invitedChatroom.id == channelId
        }
        if (index.isValidIndex(homeFeedAdapter.items())) {
            homeFeedAdapter.removeIndex(index)
        }

        // show the appropriate toast message
        val toastMessage = if (channelInviteStatus.second == ChannelInviteStatus.ACCEPTED) {
            // start sync 2 seconds after the invitation is accepted
            Timer().schedule(2000) {
                startSync()
            }
            getString(R.string.lm_chat_joined_the_group)
        } else {
            getString(R.string.lm_chat_invitation_rejected)
        }
        ViewUtils.showShortToast(requireContext(), toastMessage)
    }

    override fun onResume() {
        super.onResume()
        if (!viewModel.isDBEmpty() && !userPreferences.getIsGuestUser()) {
            startSync()
        }
        viewModel.observeLiveHomeFeed(requireContext())
    }

    override fun onPause() {
        super.onPause()
        viewModel.removeLiveHomeFeedListener()
    }

    private fun startSync() {
        val pairOfObservers = viewModel.syncChatrooms(requireContext())

        val firstTimeObserver = pairOfObservers?.first
        val appConfigObserver = pairOfObservers?.second
        lifecycleScope.launch(Dispatchers.Main) {
            when {
                firstTimeObserver != null -> {
                    val syncStartedAt = System.currentTimeMillis()
                    firstTimeObserver.observe(this@HomeFeedFragment, Observer { workInfoList ->
                        workInfoList.forEach { workInfo ->
                            if (workInfo.state != WorkInfo.State.SUCCEEDED) {
                                return@Observer
                            }
                        }
                        val timeTaken = (System.currentTimeMillis() - syncStartedAt) / 1000f
                        viewModel.setWasChatroomFetched(true)
                        viewModel.refetchChatrooms()
                        viewModel.sendSyncCompleteEvent(timeTaken)
                    })
                }

                appConfigObserver != null -> {
                    appConfigObserver.observe(this@HomeFeedFragment, Observer { workInfoList ->
                        workInfoList.forEach { workInfo ->
                            if (workInfo.state != WorkInfo.State.SUCCEEDED) {
                                return@Observer
                            }
                        }
                        viewModel.refetchChatrooms()
                    })
                }
            }
        }
    }

    private fun setTheme() {
        binding.apply {
            toolbarColor = LMTheme.getToolbarColor()
        }
    }

    private fun initRecyclerView() {
        homeFeedAdapter = HomeFeedAdapter(userPreferences, this)
        binding.rvHomeFeed.apply {
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
            adapter = homeFeedAdapter
            show()
        }
        binding.layoutRemoveAccess.root.hide()
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
                Log.d(LOG_TAG, "search started")
            }
        }
    }

    private fun fetchData() {
        viewModel.observeChatrooms()
    }

    override fun onChatRoomClicked(homeFeedItemViewData: HomeFeedItemViewData) {
        val chatroom = homeFeedItemViewData.chatroom
        openChatroom(chatroom)
    }

    private fun openChatroom(chatroom: ChatroomViewData) {
        val extra = ChatroomDetailExtras.Builder()
            .chatroomId(chatroom.id)
            .communityId(chatroom.communityId)
            .communityName(chatroom.communityName)
            .source(ChatroomDetailFragment.SOURCE_HOME_FEED)
            .build()
        ChatroomDetailActivity.start(requireContext(), extra)
    }

    override fun homeFeedClicked() {
        ChatroomExploreActivity.start(requireContext())
        viewModel.sendCommunityFeedClickedEvent(communityId, communityName)
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

    override fun onAcceptChannelInviteClicked(
        position: Int,
        channelInviteViewData: ChannelInviteViewData
    ) {
        JoinChatroomInviteDialogFragment.showDialog(
            childFragmentManager,
            ChatroomInviteDialogExtras.Builder()
                .chatroomInviteDialogTitle(getString(R.string.lm_chat_join_this_chatroom_question))
                .chatroomInviteDialogSubtitle(getString(R.string.lm_chat_join_this_chatroom_description))
                .channelInviteStatus(ChannelInviteStatus.ACCEPTED)
                .chatroomId(channelInviteViewData.invitedChatroom.id)
                .build()
        )
    }

    override fun onRejectChannelInviteClicked(
        position: Int,
        channelInviteViewData: ChannelInviteViewData
    ) {
        JoinChatroomInviteDialogFragment.showDialog(
            childFragmentManager,
            ChatroomInviteDialogExtras.Builder()
                .chatroomInviteDialogTitle(getString(R.string.lm_chat_reject_invitation_question))
                .chatroomInviteDialogSubtitle(getString(R.string.lm_chat_reject_invitation_description))
                .channelInviteStatus(ChannelInviteStatus.REJECTED)
                .chatroomId(channelInviteViewData.invitedChatroom.id)
                .build()
        )
    }

    override fun onChatroomInviteDialogConfirmed(
        invitedChatroomId: String,
        channelInviteStatus: ChannelInviteStatus
    ) {
        viewModel.updateChannelInvite(
            invitedChatroomId,
            channelInviteStatus
        )
    }
}