package com.likeminds.chatmm.chatroom.detail.view

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.likeminds.chatmm.R
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.branding.model.LMBranding
import com.likeminds.chatmm.chatroom.detail.model.ViewParticipantsExtras
import com.likeminds.chatmm.chatroom.detail.view.adapter.ViewParticipantsAdapter
import com.likeminds.chatmm.chatroom.detail.view.adapter.ViewParticipantsAdapterListener
import com.likeminds.chatmm.chatroom.detail.viewmodel.ViewParticipantsViewModel
import com.likeminds.chatmm.databinding.FragmentViewParticipantsBinding
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.search.util.CustomSearchBar
import com.likeminds.chatmm.utils.*
import com.likeminds.chatmm.utils.ErrorUtil.emptyExtrasException
import com.likeminds.chatmm.utils.ViewUtils.hide
import com.likeminds.chatmm.utils.customview.BaseFragment
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

class ViewParticipantsFragment :
    BaseFragment<FragmentViewParticipantsBinding, ViewParticipantsViewModel>(),
    ViewParticipantsAdapterListener {

    private lateinit var extras: ViewParticipantsExtras
    private lateinit var mAdapter: ViewParticipantsAdapter

    private lateinit var scrollListener: EndlessRecyclerScrollListener
    private var searchKeyword: String? = null

    @Inject
    lateinit var sdkPreferences: SDKPreferences

    override fun getViewModelClass(): Class<ViewParticipantsViewModel> {
        return ViewParticipantsViewModel::class.java
    }

    override fun getViewBinding(): FragmentViewParticipantsBinding {
        return FragmentViewParticipantsBinding.inflate(layoutInflater)
    }

    companion object {
        const val TAG = "ViewParticipantsFragment"
    }

    override fun receiveExtras() {
        super.receiveExtras()

        extras = activity?.intent?.getBundleExtra("bundle")
            ?.getParcelable(ViewParticipantsActivity.VIEW_PARTICIPANTS_EXTRAS)
            ?: throw emptyExtrasException(TAG)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().chatroomDetailComponent()?.inject(this)
    }

    override fun setUpViews() {
        super.setUpViews()
        initData()
        initToolbar()
        initRecyclerView()
        initializeSearchView()
        initSearch()
    }

    // fetches initial data
    private fun initData() {
        viewModel.fetchParticipants(
            extras.isSecretChatroom,
            extras.chatroomId,
            1,
            null
        )
    }

    // initializes the toolbar
    private fun initToolbar() {
        binding.apply {
            toolbarColor = LMBranding.getToolbarColor()

            (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

            ivBack.setOnClickListener {
                requireActivity().finish()
            }
            setHasOptionsMenu(true)
        }
    }

    // initialized the recycler view
    private fun initRecyclerView() {
        //create adapter
        mAdapter = ViewParticipantsAdapter(this)

        //create layout manager
        val linearLayoutManager = LinearLayoutManager(requireContext())
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL

        //create scroll listener
        scrollListener = object : EndlessRecyclerScrollListener(linearLayoutManager) {
            override fun onLoadMore(currentPage: Int) {
                if (currentPage > 0) {
                    viewModel.fetchParticipants(
                        extras.isSecretChatroom,
                        extras.chatroomId,
                        currentPage,
                        searchKeyword
                    )
                }
            }
        }

        //attach above to recycler view
        binding.rvParticipants.apply {
            adapter = mAdapter
            layoutManager = linearLayoutManager
            addOnScrollListener(scrollListener)
        }
    }

    private fun initializeSearchView() {
        binding.searchBar.apply {
            this.initialize(lifecycleScope)

            setSearchViewListener(object :
                CustomSearchBar.SearchViewListener {
                override fun onSearchViewClosed() {
                    hide()
                    clearParticipants()
                }

                override fun crossClicked() {
                    clearParticipants()
                }

                override fun keywordEntered(keyword: String) {
                    scrollListener.resetData()
                    mAdapter.clearAndNotify()
                    searchKeyword = keyword
                    viewModel.fetchParticipants(
                        extras.isSecretChatroom,
                        extras.chatroomId,
                        1,
                        keyword
                    )
                }

                override fun emptyKeywordEntered() {
                    clearParticipants()
                }
            })
            observeSearchView(true)
        }
    }

    private fun initSearch() {
        binding.apply {
            ivSearch.setOnClickListener {
                searchBar.visibility = View.VISIBLE
                searchBar.post {
                    searchBar.openSearch()
                }
            }
        }
    }

    private fun clearParticipants() {
        scrollListener.resetData()
        mAdapter.clearAndNotify()
        searchKeyword = null
        viewModel.fetchParticipants(
            extras.isSecretChatroom,
            extras.chatroomId,
            1,
            null
        )
    }

    override fun observeData() {
        super.observeData()

        // observe errors
        observeErrors()

        // observes [fetchParticipantsResponse] live data
        viewModel.fetchParticipantsResponse.observe(viewLifecycleOwner) { response ->
            val list = response.first
            val totalParticipants = response.second

            mAdapter.addAll(list)
            setTotalParticipantsCount(totalParticipants)
        }
    }

    private fun observeErrors() {
        viewModel.errorMessageEventFlow.onEach { response ->
            when (response) {
                is ViewParticipantsViewModel.ErrorMessageEvent.GetParticipants -> {
                    ViewUtils.showErrorMessageToast(requireContext(), response.errorMessage)
                }
            }
        }
    }

    //set total participants in header's subtitle
    private fun setTotalParticipantsCount(totalParticipants: Int) {
        binding.tvToolbarSubTitle.text = resources.getQuantityString(
            R.plurals.participants_s,
            totalParticipants,
            totalParticipants
        )
    }

    override fun onMemberClick(memberViewData: MemberViewData) {
        super.onMemberClick(memberViewData)
        // todo: member clicked.
    }
}