package com.likeminds.chatmm.dm.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.WorkInfo
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.databinding.FragmentDmFeedBinding
import com.likeminds.chatmm.dm.model.CheckDMTabViewData
import com.likeminds.chatmm.dm.view.adapter.DMAdapter
import com.likeminds.chatmm.dm.view.adapter.DMAdapterListener
import com.likeminds.chatmm.dm.viewmodel.DMFeedViewModel
import com.likeminds.chatmm.homefeed.model.HomeFeedItemViewData
import com.likeminds.chatmm.member.model.*
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.member.view.LMChatCommunityMembersActivity
import com.likeminds.chatmm.theme.model.LMTheme
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.BaseFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

class DMFeedFragment : BaseFragment<FragmentDmFeedBinding, DMFeedViewModel>(),
    DMAdapterListener {

    private var dmMetaExtras: CheckDMTabViewData? = null
    private var showList: Int = CommunityMembersFilter.ALL_MEMBERS.value

    @Inject
    lateinit var userPreferences: UserPreferences

    private val communityMembersLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val resultExtras = ExtrasUtil.getParcelable(
                    extras,
                    COMMUNITY_MEMBERS_RESULT,
                    CommunityMembersResultExtras::class.java
                ) ?: return@registerForActivityResult

                openDMChatroom(resultExtras.chatroomId)
            }
        }

    companion object {
        const val DM_META_EXTRAS = "DM_META_EXTRAS"
        const val COMMUNITY_MEMBERS_RESULT = "COMMUNITY_MEMBERS_RESULT"
        const val TAG = "DMFeedFragment"
        const val QUERY_SHOW_LIST = "show_list"

        fun getInstance(dmMeta: CheckDMTabViewData?): DMFeedFragment {
            val fragment = DMFeedFragment()
            val bundle = Bundle()
            bundle.putParcelable(DM_META_EXTRAS, dmMeta)
            fragment.arguments = bundle
            return fragment
        }
    }

    private lateinit var mAdapter: DMAdapter

    override fun receiveExtras() {
        super.receiveExtras()
        dmMetaExtras = ExtrasUtil.getParcelable(
            arguments,
            DM_META_EXTRAS,
            CheckDMTabViewData::class.java
        )
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().dmComponent()?.inject(this)
    }

    override fun getViewModelClass(): Class<DMFeedViewModel> {
        return DMFeedViewModel::class.java
    }

    override fun getViewBinding(): FragmentDmFeedBinding {
        return FragmentDmFeedBinding.inflate(layoutInflater)
    }

    override fun setUpViews() {
        super.setUpViews()
        setTheme()
        initRecyclerView()
        checkForHideDMTab()
        initApis()
        initFABListener()
    }

    override fun observeData() {
        super.observeData()
        viewModel.dmFeedFlow.onEach { dmFeedEvent ->
            when (dmFeedEvent) {
                DMFeedViewModel.DMFeedEvent.ShowDMFeedData -> {
                    fetchDMChatrooms()
                }
            }
        }.observeInLifecycle(viewLifecycleOwner)

        viewModel.checkDMResponse.observe(viewLifecycleOwner) { response ->
            toggleDMFab(response.showDM)
            handleCTA(response.cta)
        }

        viewModel.errorMessageEventFlow.onEach { response ->
            when (response) {
                is DMFeedViewModel.ErrorMessageEvent.GetDMChatroom -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }

                is DMFeedViewModel.ErrorMessageEvent.CheckDMStatus -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }
            }
        }
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
        val pairOfObservers = viewModel.syncDMChatrooms(requireContext())

        val firstTimeObserver = pairOfObservers?.first
        val appConfigObserver = pairOfObservers?.second
        lifecycleScope.launch(Dispatchers.Main) {
            when {
                firstTimeObserver != null -> {
                    firstTimeObserver.observe(this@DMFeedFragment, Observer { workInfoList ->
                        workInfoList.forEach { workInfo ->
                            if (workInfo.state != WorkInfo.State.SUCCEEDED) {
                                return@Observer
                            }
                        }
                        viewModel.setWasChatroomFetched(true)
                        viewModel.refetchDMChatrooms()
                    })
                }

                appConfigObserver != null -> {
                    appConfigObserver.observe(this@DMFeedFragment, Observer { workInfoList ->
                        workInfoList.forEach { workInfo ->
                            if (workInfo.state != WorkInfo.State.SUCCEEDED) {
                                return@Observer
                            }
                        }
                        viewModel.refetchDMChatrooms()
                    })
                }
            }
        }
    }

    private fun setTheme() {
        binding.buttonColor = LMTheme.getButtonsColor()
    }

    //init recycler view and handles all recycler view operation
    private fun initRecyclerView() {
        binding.rvDmChatrooms.apply {
            mAdapter = DMAdapter(this@DMFeedFragment, userPreferences)
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            adapter = mAdapter
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val isExtended = binding.fabNewDm.isExtended

                    // Scroll down
                    if (dy > 20 && isExtended) {
                        binding.fabNewDm.shrink()
                    }

                    // Scroll up
                    if (dy < -20 && !isExtended) {
                        binding.fabNewDm.extend()
                    }

                    // At the top
                    if (!recyclerView.canScrollVertically(-1)) {
                        binding.fabNewDm.extend()
                    }
                }
            })
        }
    }

    //if dm is not enabled, perform the following
    private fun checkForHideDMTab() {
        binding.apply {
            if (dmMetaExtras?.hideDMTab == true) {
                layoutDmDisabled.root.show()
                rvDmChatrooms.hide()
            } else {
                layoutDmDisabled.root.hide()
                rvDmChatrooms.show()

                val hideDMText = dmMetaExtras?.hideDMText
                if (!hideDMText.isNullOrEmpty()) {
                    ViewUtils.showShortToast(requireContext(), hideDMText)
                }
            }
        }
    }

    //calls the initial APIs
    private fun initApis() {
        startSync()
        viewModel.observeDMChatrooms()
        viewModel.checkDMStatus()
    }

    // initializes click listener on new DM fab
    private fun initFABListener() {
        binding.fabNewDm.setOnClickListener {
            val extras = CommunityMembersExtras.Builder()
                .showList(showList)
                .build()

            val intent = LMChatCommunityMembersActivity.getIntent(requireContext(), extras)
            communityMembersLauncher.launch(intent)
        }
    }

    //fetches dm chatrooms from viewmodel and inflate to adapter
    private fun fetchDMChatrooms() {
        val list = viewModel.fetchDMChatrooms()
        mAdapter.setItemsViaDiffUtilForHome(list)
    }

    // toggles fab
    private fun toggleDMFab(showDM: Boolean) {
        binding.fabNewDm.isVisible = showDM
    }

    private fun handleCTA(cta: String?) {
        if (cta == null) {
            return
        }
        val route = Uri.parse(cta)
        showList = route.getQueryParameter(QUERY_SHOW_LIST)?.toInt()
            ?: CommunityMembersFilter.ALL_MEMBERS.value
    }

    //opens dm chatroom by creating route
    private fun openDMChatroom(chatroomId: String) {
        //create route
        val route = Route.createDirectMessageRoute(chatroomId)

        //create intent
        val intent = Route.getRouteIntent(
            requireContext(),
            route,
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        ) ?: return

        //start activity
        startActivity(intent)
    }

    override fun dmChatroomClicked(homeFeedItemViewData: HomeFeedItemViewData) {
        startActivity(
            ChatroomDetailActivity.getIntent(
                requireContext(),
                ChatroomDetailExtras.Builder()
                    .chatroomId(homeFeedItemViewData.chatroom.id)
                    .communityId(homeFeedItemViewData.chatroom.communityId)
                    .communityName(homeFeedItemViewData.chatroom.communityName)
                    .source(LMAnalytics.Source.DIRECT_MESSAGES_SCREEN)
                    .build()
            )
        )
    }
}