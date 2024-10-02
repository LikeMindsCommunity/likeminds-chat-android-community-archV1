package com.likeminds.chatmm.chatroom.explore.view

import android.app.Activity
import android.view.Gravity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.likeminds.chatmm.LMAnalytics.Source.COMMUNITY_FEED
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.theme.model.LMTheme
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailResultExtras
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailFragment.Companion.ARG_CHATROOM_DETAIL_RESULT_EXTRAS
import com.likeminds.chatmm.chatroom.explore.model.ExploreViewData
import com.likeminds.chatmm.chatroom.explore.view.adapter.ChatroomExploreAdapter
import com.likeminds.chatmm.chatroom.explore.view.adapter.ChatroomExploreAdapterListener
import com.likeminds.chatmm.chatroom.explore.viewmodel.ExploreViewModel
import com.likeminds.chatmm.databinding.FragmentChatroomExploreBinding
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.overflowmenu.model.OverflowMenuItemViewData
import com.likeminds.chatmm.overflowmenu.view.OverflowMenuPopup
import com.likeminds.chatmm.overflowmenu.view.adapter.OverflowMenuAdapterListener
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.ValueUtils.isValidIndex
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.BaseFragment
import com.likeminds.chatmm.utils.model.BaseViewType
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ChatroomExploreFragment :
    BaseFragment<FragmentChatroomExploreBinding, ExploreViewModel>(),
    OverflowMenuAdapterListener,
    ChatroomExploreAdapterListener {

    private lateinit var endlessRecyclerScrollListenerCommunities: EndlessRecyclerScrollListener

    @Inject
    lateinit var sdkPreferences: SDKPreferences

    @Inject
    lateinit var userPreferences: UserPreferences

    private val overflowMenu: OverflowMenuPopup by lazy {
        OverflowMenuPopup.create(requireContext(), this)
    }

    private val chatroomExploreAdapter: ChatroomExploreAdapter by lazy {
        ChatroomExploreAdapter(this)
    }

    //Launcher to start Activity for result
    private val chatroomUpdateLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.extras?.let { bundle ->
                    val chatroomDetailResultExtras = ExtrasUtil.getParcelable(
                        bundle,
                        ARG_CHATROOM_DETAIL_RESULT_EXTRAS,
                        ChatroomDetailResultExtras::class.java
                    )
                    if (chatroomDetailResultExtras != null) {
                        if (chatroomDetailResultExtras.isChatroomDeleted == true) {
                            deleteChatroom(chatroomDetailResultExtras.chatroomId)
                        } else {
                            if (chatroomDetailResultExtras.isChatroomMutedChanged == true
                                || chatroomDetailResultExtras.isChatroomFollowChanged == true
                                || chatroomDetailResultExtras.isChatroomNameChanged == true
                            ) {
                                updateChatroomDetails(chatroomDetailResultExtras)
                            }
                        }
                    }
                }
            }
        }

    override fun getViewModelClass(): Class<ExploreViewModel> {
        return ExploreViewModel::class.java
    }

    override fun getViewBinding(): FragmentChatroomExploreBinding {
        return FragmentChatroomExploreBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().exploreComponent()?.inject(this)
    }

    override fun setUpViews() {
        super.setUpViews()
        fetchExploreChatrooms()
        initToolbar()
        initExploreRecyclerView()
        initClickListeners()
    }

    private fun initClickListeners() {
        binding.apply {
            tvMenu.setOnClickListener {
                showOverflowMenu()
            }
            ivPinned.setOnClickListener {
                showPinnedChatrooms()
            }
            tvPinned.setOnClickListener {
                showUnpinnedChatrooms()
            }
        }
    }

    override fun observeData() {
        super.observeData()

        viewModel.menuActions.observe(this) { menuItems ->
            if (!menuItems.isNullOrEmpty()) {
                overflowMenu.setItems(menuItems)
            }
        }

        viewModel.selectedOrder.observe(viewLifecycleOwner) {
            binding.tvMenu.text = it
        }

        viewModel.showPinnedIcon.observe(viewLifecycleOwner) { showPinnedIcon ->
            if (showPinnedIcon) {
                binding.ivPinned.show()
            } else {
                binding.ivPinned.hide()
            }
        }

        viewModel.dataList.observe(viewLifecycleOwner) { list ->
            ProgressHelper.hideProgress(binding.progressBar)
            if (list.isNotEmpty()) {
                chatroomExploreAdapter.addAll(list as List<BaseViewType>)
            }
        }

        //First -> ViewData and Second -> Position to be updated
        viewModel.followStatus.observe(viewLifecycleOwner) { pair ->
            binding.rvExplore.post {
                if (pair.second.isValidIndex(chatroomExploreAdapter.items())) {
                    chatroomExploreAdapter.update(
                        pair.second,
                        pair.first
                    )
                }
            }
        }

        viewModel.errorMessageFlow.onEach { response ->
            when (response) {
                is ExploreViewModel.ErrorMessageEvent.ChatroomFollow -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is ExploreViewModel.ErrorMessageEvent.ExploreFeed -> {
                    ProgressHelper.hideProgress(binding.progressBar)
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                    requireActivity().finish()
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)
    }

    //fetch explore feed open explore tab is clicked
    private fun fetchExploreChatrooms() {
        ProgressHelper.showProgress(binding.progressBar)
        viewModel.getInitialExploreFeed()
    }

    // initializes the toolbar
    private fun initToolbar() {
        binding.apply {
            toolbarColor = LMTheme.getToolbarColor()

            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

            ivBack.setOnClickListener {
                requireActivity().finish()
            }
        }
    }

    //init the recycler and attach scroll listener for pagination
    private fun initExploreRecyclerView() {
        binding.apply {
            val linearLayoutManager = LinearLayoutManager(requireContext())
            linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
            rvExplore.layoutManager = linearLayoutManager
            rvExplore.adapter = chatroomExploreAdapter
            attachPagination(
                rvExplore,
                linearLayoutManager
            )
        }
    }

    //attach scroll listener for pagination
    private fun attachPagination(recyclerView: RecyclerView, layoutManager: LinearLayoutManager) {
        endlessRecyclerScrollListenerCommunities =
            object : EndlessRecyclerScrollListener(layoutManager) {
                override fun onLoadMore(currentPage: Int) {
                    if (currentPage > 0) {
                        if (viewModel.showPinnedChatroomOnly) {
                            viewModel.getExploreFeed(currentPage, true)
                        } else {
                            viewModel.getExploreFeed(currentPage)
                        }
                    }
                }
            }
        recyclerView.addOnScrollListener(endlessRecyclerScrollListenerCommunities)
    }

    /**
     * This functions clears the list in recycler view and reattach scroll listener for pagination
     * When any filter is click i.e. Newest, Recently active, Most participants, Most messages and
     * Pinned chatrooms are shown.
     **/
    private fun resetRecyclerView() {
        initExploreRecyclerView()
        chatroomExploreAdapter.clearAndNotify()
    }

    //to show the filter of Explore feed
    private fun showOverflowMenu() {
        overflowMenu.showAsDropDown(
            binding.tvMenu,
            -ViewUtils.dpToPx(16),
            -binding.tvMenu.height / 2,
            Gravity.START
        )
    }

    //shows pinned chatrooms when user clicks pin icon
    private fun showPinnedChatrooms() {
        binding.apply {
            ProgressHelper.showProgress(progressBar)
            ivPinned.hide()
            tvPinned.show()
            viewModel.setShowPinnedChatroomsOnly(true)
            resetRecyclerView()
            viewModel.getExploreFeed(1, true)
        }
    }

    //shows all the chatrooms whether unpinned or pinned
    private fun showUnpinnedChatrooms() {
        binding.apply {
            ProgressHelper.showProgress(progressBar)
            ivPinned.show()
            tvPinned.hide()
            resetRecyclerView()
            viewModel.setShowPinnedChatroomsOnly(false)
            viewModel.getExploreFeed(1)
        }
    }

    private fun deleteChatroom(chatroomId: String?) {
        if (chatroomId.isNullOrEmpty()) return
        var positionToDelete = -1
        for ((position, baseViewType) in chatroomExploreAdapter.items().withIndex()) {
            if (getChatroomExploreViewData(baseViewType) == null) {
                continue
            }
            val exploreViewData = baseViewType as ExploreViewData
            if (exploreViewData.id == chatroomId) {
                positionToDelete = position
                break
            }
        }
        if (positionToDelete >= 0 && positionToDelete < chatroomExploreAdapter.itemCount) {
            chatroomExploreAdapter.removeIndex(positionToDelete)
        }
    }

    private fun updateChatroomDetails(chatroomDetailResultExtras: ChatroomDetailResultExtras) {
        for ((position, baseViewType) in chatroomExploreAdapter.items().withIndex()) {
            if (getChatroomExploreViewData(baseViewType) == null) {
                continue
            }
            val exploreViewData = baseViewType as ExploreViewData
            if (exploreViewData.id == chatroomDetailResultExtras.chatroomId) {
                var chatroomViewDataBuilder = exploreViewData.chatroomViewData?.toBuilder()
                if (chatroomDetailResultExtras.isChatroomMutedChanged == true) {
                    chatroomViewDataBuilder =
                        chatroomViewDataBuilder?.muteStatus(
                            chatroomDetailResultExtras.updatedMuteStatus ?: false
                        )
                }
                if (chatroomDetailResultExtras.isChatroomFollowChanged == true) {
                    chatroomViewDataBuilder =
                        chatroomViewDataBuilder?.followStatus(
                            chatroomDetailResultExtras.updatedFollowStatus ?: false
                        )
                }
                if (chatroomDetailResultExtras.isChatroomNameChanged == true) {
                    chatroomViewDataBuilder =
                        chatroomViewDataBuilder?.header(chatroomDetailResultExtras.updatedChatroomName)
                }

                chatroomExploreAdapter.update(
                    position, exploreViewData.toBuilder().chatroomViewData(
                        chatroomViewDataBuilder?.build()
                    ).build()
                )
                break
            }
        }
    }

    private fun getChatroomExploreViewData(baseViewType: BaseViewType?): ExploreViewData? {
        if (baseViewType !is ExploreViewData) {
            return null
        }
        return baseViewType
    }

    //Handles chatroom click and show chatroom detail screen
    override fun onChatroomClick(exploreViewData: ExploreViewData, position: Int) {
        val chatroomViewData = exploreViewData.chatroomViewData ?: return

        //Update External seen
        if (exploreViewData.externalSeen == false) {
            binding.rvExplore.post {
                if (position >= 0 && position < chatroomExploreAdapter.itemCount) {
                    chatroomExploreAdapter.update(
                        position,
                        exploreViewData.toBuilder().externalSeen(true).build()
                    )
                }
            }
        }

        //Start activity
        val intent = ChatroomDetailActivity.getIntent(
            requireContext(), ChatroomDetailExtras.Builder()
                .chatroomId(chatroomViewData.id)
                .communityId(chatroomViewData.communityId)
                .communityName(chatroomViewData.communityName)
                .source(COMMUNITY_FEED)
                .build()
        )
        chatroomUpdateLauncher.launch(intent)
    }

    //Handles Join/Joined button clicks
    override fun onJoinClick(follow: Boolean, position: Int, exploreViewData: ExploreViewData) {
        //Check for guest flow
        if (userPreferences.getIsGuestUser()) {
            //User is Guest
            SDKApplication.getLikeMindsCallback()?.login()
            activity?.finish()
        } else {
            //User is logged in

            //Check whether logged in user is creator of the chatroom
            //Don't allow creator to leave the chatroom
            if (!follow && exploreViewData.isCreator) {
                ViewUtils.showShortSnack(
                    binding.root,
                    getString(R.string.lm_chat_creator_cant_leave_message)
                )
                return
            }
            viewModel.followChatroom(follow, exploreViewData, position)
        }
    }

    //Handles click of filter and show list accordingly
    override fun onMenuItemClicked(menu: OverflowMenuItemViewData) {
        overflowMenu.dismiss()
        ProgressHelper.showProgress(binding.progressBar)
        resetRecyclerView()
        viewModel.setSelectedOrder(menu.title)
        fetchExploreChatrooms()
    }
}