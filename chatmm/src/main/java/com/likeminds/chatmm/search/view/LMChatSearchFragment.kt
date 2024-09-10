package com.likeminds.chatmm.search.view

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.chatroom.detail.model.ChatroomDetailExtras
import com.likeminds.chatmm.chatroom.detail.view.ChatroomDetailActivity
import com.likeminds.chatmm.databinding.LmChatFragmentSearchBinding
import com.likeminds.chatmm.homefeed.model.HomeChatroomListShimmerViewData
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.search.model.*
import com.likeminds.chatmm.search.util.LMChatCustomSearchBar
import com.likeminds.chatmm.search.util.SearchScrollListener
import com.likeminds.chatmm.search.view.LMChatSearchActivity.Companion.SEARCH_EXTRAS
import com.likeminds.chatmm.search.view.adapter.SearchAdapter
import com.likeminds.chatmm.search.view.adapter.SearchAdapterListener
import com.likeminds.chatmm.search.viewmodel.SearchViewModel
import com.likeminds.chatmm.utils.ExtrasUtil
import com.likeminds.chatmm.utils.ValueUtils.isValidIndex
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.customview.BaseFragment
import javax.inject.Inject

class LMChatSearchFragment : BaseFragment<LmChatFragmentSearchBinding, SearchViewModel>(),
    SearchAdapterListener {
    override fun getViewModelClass(): Class<SearchViewModel> {
        return SearchViewModel::class.java
    }

    override fun getViewBinding(): LmChatFragmentSearchBinding {
        return LmChatFragmentSearchBinding.inflate(layoutInflater)
    }

    override fun attachDagger() {
        super.attachDagger()
        SDKApplication.getInstance().searchComponent()?.inject(this)
    }

    @Inject
    lateinit var userPreferences: UserPreferences

    private lateinit var mAdapter: SearchAdapter

    private var searchExtras: LMChatSearchExtras? = null

    companion object {
        const val TAG = "SearchFragment"

        fun getInstance(): LMChatSearchFragment {
            return LMChatSearchFragment()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun receiveExtras() {
        super.receiveExtras()
        if (arguments == null || arguments?.containsKey(SEARCH_EXTRAS) == false) {
            requireActivity().supportFragmentManager.popBackStack()
            return
        }

        searchExtras = ExtrasUtil.getParcelable(
            requireArguments(),
            SEARCH_EXTRAS,
            LMChatSearchExtras::class.java
        ) ?: return
    }

    private val scrollListener = object : SearchScrollListener() {
        override fun onLoadMore() {
            showSingleShimmer()
            viewModel.fetchNextPage(false, searchExtras?.chatroomId)
        }
    }

    override fun setUpViews() {
        super.setUpViews()
        initSearchBar()
        initRecyclerView()
    }

    override fun observeData() {
        super.observeData()

        viewModel.keywordSearched.observe(viewLifecycleOwner) {
            mAdapter.clearAndNotify()
            showFullShimmer()
            viewModel.fetchNextPage(false, searchExtras?.chatroomId)
        }

        viewModel.searchLiveData.observe(viewLifecycleOwner) { searchData ->
            if (searchData != null) {
                //if search results are present remove whole shimmer
                if (searchData.dataList.isNotEmpty()) {
                    removeShimmer()
                }

                //if pagination is disabled then, scroll listener will not call [loadMore()]
                if (!searchData.disablePagination) {
                    scrollListener.setBottomLoadingToTrue()
                }

                //add line break into the adapter to show chatrooms are loaded
                if (searchData.checkForSeparator && searchData.dataList.isNotEmpty()) {
                    addSeparatorIfRequired()
                }
                //add list to adapter
                mAdapter.addAll(searchData.dataList)

                //if next page is called add single shimmer
                if (searchData.dataList.size < SearchViewModel.PAGE_SIZE
                    && (viewModel.currentApi != SearchViewModel.API_SEARCH_UNFOLLOWED_CONVERSATIONS)
                ) {
                    showSingleShimmer()
                }
            } else {
                //if search is closed or search keyword is cleared then clear the list.
                mAdapter.clearAndNotify()
            }
        }

        viewModel.noUnfollowedConversationsFound.observe(viewLifecycleOwner) {
            if (viewModel.searchLiveData.value != null) {
                if (it.first && it.second == viewModel.keywordSearched.value) {
                    removeShimmer()
                    if (mAdapter.itemCount == 0) {
                        showNoResultsView()
                    }
                }
            }
        }
    }

    override fun onChatroomClicked(searchChatroomHeaderViewData: SearchChatroomHeaderViewData) {
        super.onChatroomClicked(searchChatroomHeaderViewData)
        viewModel.sendChatroomClickedEvent(
            searchChatroomHeaderViewData.chatroom.id,
            searchChatroomHeaderViewData.chatroom.communityId
        )
        startActivity(
            ChatroomDetailActivity.getIntent(
                requireContext(), ChatroomDetailExtras.Builder()
                    .chatroomId(searchChatroomHeaderViewData.chatroom.id)
                    .communityId(searchChatroomHeaderViewData.chatroom.communityId)
                    .communityName(searchChatroomHeaderViewData.chatroom.communityName)
                    .source(LMAnalytics.Source.HOME_FEED)
                    .isFromSearchChatroom(true)
                    .build()
            )
        )
    }

    override fun onTitleClicked(searchChatroomTitleViewData: SearchChatroomTitleViewData) {
        super.onTitleClicked(searchChatroomTitleViewData)
        viewModel.sendChatroomClickedEvent(
            searchChatroomTitleViewData.chatroom.id,
            searchChatroomTitleViewData.chatroom.communityId
        )

        startActivity(
            ChatroomDetailActivity.getIntent(
                requireContext(), ChatroomDetailExtras.Builder()
                    .chatroomId(searchChatroomTitleViewData.chatroom.id)
                    .communityId(searchChatroomTitleViewData.chatroom.communityId)
                    .communityName(searchChatroomTitleViewData.chatroom.communityName)
                    .source(LMAnalytics.Source.HOME_FEED)
                    .isFromSearchChatroom(true)
                    .scrollToExtremeTopForHighlightingTitle(true)
                    .build()
            )
        )
    }

    override fun onMessageClicked(searchConversationViewData: SearchConversationViewData) {
        super.onMessageClicked(searchConversationViewData)
        viewModel.sendMessageClickedEvent(
            searchConversationViewData.chatroom?.id,
            searchConversationViewData.chatroom?.communityId
        )

        startActivity(
            ChatroomDetailActivity.getIntent(
                requireContext(), ChatroomDetailExtras.Builder()
                    .chatroomId(searchConversationViewData.chatroom?.id ?: "")
                    .communityId(searchConversationViewData.chatroom?.communityId)
                    .communityName(searchConversationViewData.chatroom?.communityName)
                    .conversationId(searchConversationViewData.chatroomAnswer.id)
                    .source(LMAnalytics.Source.HOME_FEED)
                    .isFromSearchMessage(true)
                    .build()
            )
        )
    }

    private fun initSearchBar() {
        binding.searchBar.apply {
            this.initialize(lifecycleScope)
            //open search bar with animation
            post {
                openSearch()
            }

            //set search listener
            setSearchViewListener(
                object : LMChatCustomSearchBar.SearchViewListener {
                    override fun onSearchViewOpened() {
                        ViewUtils.showKeyboard(context)
                        viewModel.sendSearchIconClickedEvent()
                    }

                    override fun onSearchViewClosed() {
                        viewModel.setSearchedDataToNull()
                        viewModel.sendSearchClosedEvent()
                        requireActivity().finish()
                    }

                    override fun keywordEntered(keyword: String) {
                        viewModel.setKeyword(keyword)
                    }

                    override fun emptyKeywordEntered() {
                        viewModel.setSearchedDataToNull()
                    }

                    override fun crossClicked() {
                        viewModel.sendSearchCrossIconClickedEvent()
                        viewModel.setSearchedDataToNull()
                    }
                }
            )

            //start observing the search keywords
            observeSearchView(debounce = true)
        }
    }

    private fun initRecyclerView() {
        mAdapter = SearchAdapter(this, userPreferences)
        binding.rvSearch.apply {
            adapter = mAdapter
            setHasFixedSize(true)
            addOnScrollListener(scrollListener)
        }
    }

    private fun showSingleShimmer() {
        val shimmer = mAdapter.items().firstOrNull {
            it is HomeChatroomListShimmerViewData || it is SingleShimmerViewData
        }
        if (shimmer == null) {
            mAdapter.add(viewModel.getSingleShimmerView())
        }
    }

    /**
     * [SearchViewModel.getRequiredSeparator] returns a pair of a boolean and a list
     * the boolean is used to remove the last element from the list, if it is a header type view
     * the list is added in the view
     * if separator already present will get an empty list
     * if headers not present only display content header messages
     * if header is present then display line break and content header
     */
    private fun addSeparatorIfRequired() {
        val separator = viewModel.getRequiredSeparator(mAdapter.items())
        if (separator.first) {
            val index = mAdapter.items().indexOfLast { it is SearchChatroomHeaderViewData }
            mAdapter.removeIndex(index)
        }
        mAdapter.addAll(separator.second)
    }

    private fun showFullShimmer() {
        mAdapter.add(viewModel.getShimmerView())
    }

    private fun removeShimmer() {
        val index = mAdapter.items().indexOfFirst {
            it is HomeChatroomListShimmerViewData || it is SingleShimmerViewData
        }
        if (index.isValidIndex()) {
            mAdapter.removeIndex(index)
        }
    }

    private fun showNoResultsView() {
        mAdapter.add(viewModel.getNoSearchResultsView())
    }
}