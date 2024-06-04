package com.likeminds.chatmm.member.view

import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.databinding.LmChatFragmentCommunityMembersBinding
import com.likeminds.chatmm.dm.model.CheckDMLimitViewData
import com.likeminds.chatmm.dm.model.DMLimitExceededDialogExtras
import com.likeminds.chatmm.dm.view.DMFeedFragment.Companion.COMMUNITY_MEMBERS_RESULT
import com.likeminds.chatmm.dm.view.DMLimitExceededDialogFragment
import com.likeminds.chatmm.member.model.CommunityMembersExtras
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.member.view.LMChatCommunityMembersActivity.Companion.COMMUNITY_MEMBERS_EXTRAS
import com.likeminds.chatmm.member.view.adapter.CommunityMembersAdapter
import com.likeminds.chatmm.member.view.adapter.CommunityMembersAdapterListener
import com.likeminds.chatmm.member.viewmodel.CommunityMembersViewModel
import com.likeminds.chatmm.search.util.CustomSearchBar
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.ViewUtils.show
import com.likeminds.chatmm.utils.customview.BaseFragment
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class LMChatCommunityMembersFragment :
    BaseFragment<LmChatFragmentCommunityMembersBinding, CommunityMembersViewModel>(),
    CommunityMembersAdapterListener {

    @Inject
    lateinit var userPreferences: UserPreferences

    override fun getViewModelClass(): Class<CommunityMembersViewModel> {
        return CommunityMembersViewModel::class.java
    }

    override fun getViewBinding(): LmChatFragmentCommunityMembersBinding {
        return LmChatFragmentCommunityMembersBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().memberComponent()?.inject(this)
    }

    companion object {
        const val TAG = "CommunityMembersFragment"
    }

    private lateinit var mAdapter: CommunityMembersAdapter

    private lateinit var scrollListener: EndlessRecyclerScrollListener
    private lateinit var extras: CommunityMembersExtras
    private var searchKeyword: String? = null

    override fun receiveExtras() {
        super.receiveExtras()
        val bundle = requireActivity().intent?.getBundleExtra("bundle")
        extras = ExtrasUtil.getParcelable(
            bundle,
            COMMUNITY_MEMBERS_EXTRAS,
            CommunityMembersExtras::class.java
        ) ?: throw ErrorUtil.emptyExtrasException(TAG)
    }

    override fun setUpViews() {
        super.setUpViews()
        setupMenu()
        initializeClickListener()
        initRecyclerView()
        initData()
    }

    override fun observeData() {
        super.observeData()
        viewModel.membersResponse.observe(viewLifecycleOwner) { response ->
            val membersCount = response.second
            val members = response.first

            setMembersCount(membersCount)
            setMembers(members)
        }

        viewModel.checkDMLimitResponse.observe(viewLifecycleOwner) { response ->
            val checkDMLimitViewData = response.first
            val uuidSelected = response.second
            onDMLimitReceived(checkDMLimitViewData, uuidSelected)
        }

        viewModel.dmChatroomId.observe(viewLifecycleOwner) { chatroomId ->
            createActivityResultAndFinish(chatroomId)
        }

        viewModel.errorEventFlow.onEach { response ->
            when (response) {
                is CommunityMembersViewModel.ErrorMessageEvent.CheckDMLimit -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is CommunityMembersViewModel.ErrorMessageEvent.CreateDMChatroom -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is CommunityMembersViewModel.ErrorMessageEvent.GetAllMembers -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }

                is CommunityMembersViewModel.ErrorMessageEvent.SearchMembers -> {
                    val errorMessage = response.errorMessage
                    ViewUtils.showErrorMessageToast(requireContext(), errorMessage)
                }
            }

        }.observeInLifecycle(viewLifecycleOwner)
    }

    override fun onMemberSelected(member: MemberViewData) {
        val uuid = member.sdkClientInfo.uuid
        viewModel.checkDMLimit(uuid)
    }

    private fun initData() {
        Log.d("PUI", "initData: ")
        viewModel.getAllMembers(extras.showList, 1)
    }

    private fun initRecyclerView() {
        //create layout manager
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

        //create scroll listener
        scrollListener = object : EndlessRecyclerScrollListener(linearLayoutManager) {
            override fun onLoadMore(currentPage: Int) {
                if (currentPage > 0) {
                    if (searchKeyword == null) {
                        Log.d("PUI", "scrollListener: searchKeyword nulllll")

                        viewModel.getAllMembers(extras.showList, currentPage)
                    } else {
                        Log.d("PUI", "scrollListener: searchKeyword not")
                        viewModel.searchMembers(extras.showList, currentPage, searchKeyword ?: "")
                    }
                }
            }
        }

        //attach it to recycler view
        binding.rvMembers.apply {
            mAdapter = CommunityMembersAdapter(this@LMChatCommunityMembersFragment, userPreferences)
            adapter = mAdapter
            layoutManager = linearLayoutManager
            addOnScrollListener(scrollListener)
        }
    }

    private fun setupMenu() {
        val fragmentActivity = activity as AppCompatActivity?
        fragmentActivity?.setSupportActionBar(binding.toolbar)

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.lm_chat_dm_all_member_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    R.id.menu_search -> {
                        showSearchToolbar()
                    }

                    else -> return false
                }
                return true
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun initializeClickListener() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }
    }

    private fun setMembersCount(membersCount: Int) {
        if (membersCount == 0) {
            return
        }
        binding.tvMemberCount.apply {
            show()
            text = resources.getQuantityString(
                R.plurals.lm_chat_number_of_member,
                membersCount,
                membersCount,
                ""
            )
        }
    }

    private fun setMembers(members: List<MemberViewData>) {
        mAdapter.addAll(members)

        binding.apply {
            if (mAdapter.itemCount == 0) {
                layoutNoResults.root.show()
                tvMemberCount.hide()
            } else {
                layoutNoResults.root.hide()
            }
        }
    }

    private fun onDMLimitReceived(dmLimitData: CheckDMLimitViewData?, uuidSelected: String) {
        if (dmLimitData == null) return
        when {
            (dmLimitData.chatroomId != null) -> {
                createActivityResultAndFinish(dmLimitData.chatroomId)
            }

            (dmLimitData.isRequestDMLimitExceeded == false) -> {
                viewModel.createDMChatroom(uuidSelected)
            }

            else -> {
                val dmLimitExceededDialogExtras = DMLimitExceededDialogExtras.Builder()
                    .newRequestDMTimestamp(dmLimitData.newRequestDMTimestamp)
                    .duration(dmLimitData.duration)
                    .numberInDuration(dmLimitData.numberInDuration)
                    .build()
                DMLimitExceededDialogFragment.showDialog(
                    childFragmentManager,
                    dmLimitExceededDialogExtras
                )
            }
        }
    }

    private fun createActivityResultAndFinish(chatroomId: String) {
        val result = viewModel.createDMAllMemberResult(chatroomId)
        val intent = Intent().apply {
            putExtra(COMMUNITY_MEMBERS_RESULT, result)
        }
        requireActivity().setResult(Activity.RESULT_OK, intent)
        requireActivity().finish()
    }

    private fun showSearchToolbar() {
        binding.searchBar.apply {
            show()
            post {
                initializeSearchView()
                openSearch()
            }
        }
    }

    private fun initializeSearchView() {
        binding.searchBar.apply {
            this.initialize(lifecycleScope)

            setSearchViewListener(object : CustomSearchBar.SearchViewListener {
                override fun onSearchViewClosed() {
                    Log.d("PUI", "onSearchViewClosed: ")

                    hide()
                    clearMemberSearch()
                }

                override fun crossClicked() {
                    Log.d("PUI", "crossClicked: ")

                    clearMemberSearch()
                }

                override fun keywordEntered(keyword: String) {
                    scrollListener.resetData()
                    mAdapter.clearAndNotify()
                    searchKeyword = keyword
                    viewModel.searchMembers(extras.showList, 1, searchKeyword ?: "")
                }

                override fun emptyKeywordEntered() {
                    Log.d("PUI", "emptyKeywordEntered: ")

                    clearMemberSearch()
                }
            })
            observeSearchView(true)
        }
    }

    private fun clearMemberSearch() {
        scrollListener.resetData()
        mAdapter.clearAndNotify()
        searchKeyword = null
        viewModel.getAllMembers(extras.showList, 1)
    }
}