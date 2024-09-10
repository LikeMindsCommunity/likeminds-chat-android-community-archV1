package com.likeminds.chatmm.search.viewmodel

import androidx.lifecycle.*
import com.likeminds.chatmm.LMAnalytics
import com.likeminds.chatmm.homefeed.model.HomeChatroomListShimmerViewData
import com.likeminds.chatmm.search.model.*
import com.likeminds.chatmm.utils.ViewDataConverter
import com.likeminds.chatmm.utils.coroutine.launchIO
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.search.model.SearchChatroomRequest
import com.likeminds.likemindschat.search.model.SearchConversationRequest
import javax.inject.Inject

class SearchViewModel @Inject constructor() : ViewModel() {

    private val lmChatClient = LMChatClient.getInstance()

    companion object {
        const val PAGE_SIZE = 100
        private const val PARAM_SEARCH_TYPE_HEADER = "header"
        private const val PARAM_SEARCH_TYPE_TITLE = "title"
        private const val MESSAGES = "Messages"
        private const val API_SEARCH_FOLLOWED_HEADERS = "followedHeaders"
        private const val API_SEARCH_UNFOLLOWED_HEADERS = "unFollowedHeaders"
        private const val API_SEARCH_FOLLOWED_TITLES = "followedTitles"
        private const val API_SEARCH_UNFOLLOWED_TITLES = "unFollowedTitles"
        private const val API_SEARCH_FOLLOWED_CONVERSATIONS = "followedConversations"
        const val API_SEARCH_UNFOLLOWED_CONVERSATIONS = "unFollowedConversations"
    }

    private var currentApiPage = 0

    /**
     * [searchLiveData] is used to pass paginated data from API to Fragment
     * [currentApi] is kept to SEARCH_FOLLOWED_HEADERS by default
     * [noUnfollowedConversationsFound] is used to check if there is no unfollowed conversation and then show noResultsView in Fragment
     */
    private val _searchLiveData = MutableLiveData<SearchViewData?>()
    val searchLiveData: LiveData<SearchViewData?> = _searchLiveData

    var currentApi = API_SEARCH_FOLLOWED_HEADERS
    val noUnfollowedConversationsFound by lazy { MutableLiveData<Pair<Boolean, String>>() }

    private val _keywordSearched = MutableLiveData<String>()
    val keywordSearched: LiveData<String> = _keywordSearched

    fun getSingleShimmerView() = SingleShimmerViewData.Builder().build()

    fun getShimmerView() = HomeChatroomListShimmerViewData.Builder().build()

    private fun getLineBreakView() = SearchLineBreakViewData.Builder().build()

    private fun getContentHeaderView(): SearchContentHeaderViewData {
        return SearchContentHeaderViewData.Builder().title(MESSAGES).build()
    }

    fun getNoSearchResultsView() = SearchNoResultsScreenViewData.Builder().build()

    private fun getKeyword() = _keywordSearched.value ?: ""

    /**
     * This function is used to call next page in case of various APIs
     * @param disablePagination is used to check if the API is already fetching data,
     * @param chatroomId is used to search conversations inside a chatroom
     * so not to fetch data using scroll listener
     */
    fun fetchNextPage(disablePagination: Boolean, chatroomId: String?) {
        currentApiPage++
        if (chatroomId == null) {
            when (currentApi) {
                API_SEARCH_FOLLOWED_HEADERS -> getFollowedHeaders(disablePagination)
                API_SEARCH_UNFOLLOWED_HEADERS -> getUnfollowedHeaders(disablePagination)
                API_SEARCH_FOLLOWED_TITLES -> getFollowedTitles(disablePagination)
                API_SEARCH_UNFOLLOWED_TITLES -> getUnfollowedTitles(disablePagination)
                API_SEARCH_FOLLOWED_CONVERSATIONS -> getFollowedConversations(
                    disablePagination,
                    null
                )

                API_SEARCH_UNFOLLOWED_CONVERSATIONS -> getUnfollowedConversations(disablePagination)
            }
        } else {
            getFollowedConversations(disablePagination, chatroomId)
        }
    }

    /**
     * This function is used to getFollowedHeaders
     * or else
     * getUnfollowedHeaders if result size < [PAGE_SIZE]
     */
    private fun getFollowedHeaders(disablePagination: Boolean) {
        viewModelScope.launchIO {
            val searchChatroomRequest = getSearchChatroomRequest(true, PARAM_SEARCH_TYPE_HEADER)
            searchChatroomHeadersApi(
                searchChatroomRequest,
                API_SEARCH_UNFOLLOWED_HEADERS,
                disablePagination
            )
        }
    }

    /**
     * This function is used to getUnfollowedHeaders
     * or else
     * getFollowedTitles if result size < [PAGE_SIZE]
     */
    private fun getUnfollowedHeaders(disablePagination: Boolean) {
        val searchChatroomRequest = getSearchChatroomRequest(false, PARAM_SEARCH_TYPE_HEADER)
        searchChatroomHeadersApi(
            searchChatroomRequest,
            API_SEARCH_FOLLOWED_TITLES,
            disablePagination
        )
    }

    /**
     * This function is used to getFollowedTitles
     * or else
     * getFollowedConversations if result size < [PAGE_SIZE]
     */
    private fun getFollowedTitles(disablePagination: Boolean) {
        val searchChatroomRequest = getSearchChatroomRequest(true, PARAM_SEARCH_TYPE_TITLE)
        searchChatroomTitlesApi(
            searchChatroomRequest,
            API_SEARCH_FOLLOWED_CONVERSATIONS,
            disablePagination
        )
    }

    /**
     * This function is used to getUnfollowedTitles
     * or else
     * getUnfollowedConversations if result size < [PAGE_SIZE]
     */
    private fun getUnfollowedTitles(disablePagination: Boolean) {
        val searchChatroomRequest = getSearchChatroomRequest(true, PARAM_SEARCH_TYPE_TITLE)
        searchChatroomTitlesApi(
            searchChatroomRequest,
            API_SEARCH_UNFOLLOWED_CONVERSATIONS,
            disablePagination
        )
    }

    /**
     * This function is used to getFollowedConversations
     * or else
     * getUnfollowedTitles if result size < [PAGE_SIZE]
     */
    private fun getFollowedConversations(disablePagination: Boolean, chatroomId: String?) {
        viewModelScope.launchIO {
            val request = getSearchConversationRequest(true, chatroomId)
            val response = lmChatClient.searchConversation(request)

            if (response.success) {
                val conversations = response.data?.conversations.orEmpty()
                if (conversations.size < PAGE_SIZE) {
                    if (chatroomId == null) {
                        callNextApi(API_SEARCH_UNFOLLOWED_TITLES)
                    } else {
                        _searchLiveData.postValue(
                            SearchViewData.Builder().dataList(emptyList()).build()
                        )
                    }
                }
                _searchLiveData.postValue(
                    SearchViewData.Builder()
                        .disablePagination(disablePagination)
                        .dataList(
                            ViewDataConverter.convertSearchConversations(
                                conversations,
                                true,
                                request.search
                            )
                        )
                        .keyword(request.search)
                        .checkForSeparator(true)
                        .build()
                )
            } else if (chatroomId == null) {
                callNextApi(API_SEARCH_UNFOLLOWED_TITLES)
            }
        }
    }

    /**
     * This function is used to getUnfollowedConversations
     * or else
     * update [noUnfollowedConversationsFound] if there are no conversations
     */
    private fun getUnfollowedConversations(disablePagination: Boolean) {
        viewModelScope.launchIO {
            val request = getSearchConversationRequest(false)
            val response = lmChatClient.searchConversation(request)
            if (response.success) {
                val conversations = response.data?.conversations.orEmpty()
                if (conversations.isEmpty()) {
                    noUnfollowedConversationsFound.postValue(Pair(true, request.search))
                } else {
                    _searchLiveData.postValue(
                        SearchViewData.Builder()
                            .disablePagination(disablePagination)
                            .dataList(
                                ViewDataConverter.convertSearchConversations(
                                    conversations,
                                    false,
                                    request.search
                                )
                            )
                            .keyword(request.search)
                            .checkForSeparator(true)
                            .build()
                    )
                }
            }
        }
    }

    private fun getSearchChatroomRequest(
        followStatus: Boolean,
        searchType: String
    ): SearchChatroomRequest {
        return SearchChatroomRequest.Builder()
            .search(getKeyword())
            .followStatus(followStatus)
            .page(currentApiPage)
            .pageSize(PAGE_SIZE)
            .searchType(searchType)
            .build()
    }

    private fun getSearchConversationRequest(
        followStatus: Boolean,
        chatroomId: String? = null
    ): SearchConversationRequest {
        return SearchConversationRequest.Builder()
            .search(getKeyword())
            .followStatus(followStatus)
            .chatroomId(chatroomId)
            .page(currentApiPage)
            .pageSize(PAGE_SIZE)
            .build()
    }

    private fun searchChatroomHeadersApi(
        request: SearchChatroomRequest,
        nextApi: String,
        disablePagination: Boolean
    ) {
        viewModelScope.launchIO {
            val response = lmChatClient.searchChatroom(request)
            if (response.success) {
                val chatrooms = response.data?.chatrooms.orEmpty()
                if (chatrooms.size < PAGE_SIZE) {
                    callNextApi(nextApi)
                }
                _searchLiveData.postValue(
                    SearchViewData.Builder()
                        .disablePagination(disablePagination)
                        .dataList(
                            ViewDataConverter.convertSearchChatroomHeaders(
                                chatrooms,
                                request.followStatus,
                                request.search
                            )
                        )
                        .keyword(request.search)
                        .build()
                )
            } else {
                callNextApi(nextApi)
            }
        }
    }

    private fun searchChatroomTitlesApi(
        request: SearchChatroomRequest,
        nextApi: String,
        disablePagination: Boolean
    ) {
        viewModelScope.launchIO {
            val response = lmChatClient.searchChatroom(request)
            if (response.success) {
                val chatrooms = response.data?.chatrooms.orEmpty()
                if (chatrooms.size < PAGE_SIZE) {
                    callNextApi(nextApi)
                }
                _searchLiveData.postValue(
                    SearchViewData.Builder()
                        .disablePagination(disablePagination)
                        .dataList(
                            ViewDataConverter.convertSearchChatroomTitles(
                                chatrooms,
                                request.followStatus,
                                request.search
                            )
                        )
                        .keyword(request.search)
                        .checkForSeparator(true)
                        .build()
                )
            } else {
                callNextApi(nextApi)
            }
        }
    }

    /**
     * This function is used to update api call being called
     */
    private fun callNextApi(nextApi: String) {
        currentApiPage = 0
        currentApi = nextApi
        fetchNextPage(true, null)
    }

    /**
     * first in pair is used to modify the last header view
     * if separator already present return
     * if headers not present only display content header messages
     * if header is present then display line break and content header
     */
    fun getRequiredSeparator(list: List<BaseViewType>): Pair<Boolean, List<BaseViewType>> {
        if (list.indexOfFirst { it is SearchContentHeaderViewData } > -1) {
            return Pair(false, emptyList())
        }
        if (list.indexOfFirst { it is SearchChatroomHeaderViewData } == -1) {
            return Pair(false, mutableListOf(getContentHeaderView()))
        }
        val lastItem =
            list.lastOrNull { it is SearchChatroomHeaderViewData } as? SearchChatroomHeaderViewData
        if (lastItem != null) {
            val separators = mutableListOf<BaseViewType>()

            separators.add(lastItem.toBuilder().isLast(true).build())
            separators.add(getLineBreakView())
            separators.add(getContentHeaderView())
            return Pair(true, separators)
        }
        return Pair(false, emptyList())
    }

    fun setKeyword(keyword: String) {
        currentApiPage = 0
        currentApi = API_SEARCH_FOLLOWED_HEADERS
        _keywordSearched.postValue(keyword)
    }

    fun setSearchedDataToNull() {
        _searchLiveData.postValue(null)
    }

    /**------------------------------------------------------------
     * Analytics functions
    ---------------------------------------------------------------*/

    // Triggers when the user taps on searched message
    fun sendMessageClickedEvent(chatroomId: String?, communityId: String?) {
        LMAnalytics.track(
            LMAnalytics.Events.MESSAGE_SEARCHED,
            mapOf(
                LMAnalytics.Keys.CHATROOM_ID to chatroomId,
                LMAnalytics.Keys.COMMUNITY_ID to communityId,
            )
        )
    }

    // Triggers when the user taps on searched chatroom
    fun sendChatroomClickedEvent(chatroomId: String, communityId: String?) {
        LMAnalytics.track(
            LMAnalytics.Events.CHATROOM_SEARCHED,
            mapOf(
                LMAnalytics.Keys.CHATROOM_ID to chatroomId,
                LMAnalytics.Keys.COMMUNITY_ID to communityId,
            )
        )
    }

    // Triggers when the user taps on search icon
    fun sendSearchIconClickedEvent() {
        LMAnalytics.track(
            LMAnalytics.Events.SEARCH_ICON_CLICKED,
            mapOf(
                LMAnalytics.Keys.SOURCE to LMAnalytics.Source.HOME_FEED,
            )
        )
    }

    // Triggers when the user closes search
    fun sendSearchClosedEvent() {
        LMAnalytics.track(LMAnalytics.Events.CHATROOM_SEARCH_CLOSED)
    }

    // Triggers when the user clicks cross icon on search
    fun sendSearchCrossIconClickedEvent() {
        LMAnalytics.track(
            LMAnalytics.Events.SEARCH_CROSS_ICON_CLICKED,
            mapOf(
                LMAnalytics.Keys.SOURCE to LMAnalytics.Source.HOME_FEED,
            )
        )
    }
}