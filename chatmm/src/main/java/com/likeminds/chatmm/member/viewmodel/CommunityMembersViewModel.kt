package com.likeminds.chatmm.member.viewmodel

import android.util.Log
import androidx.lifecycle.*
import com.likeminds.chatmm.SDKApplication.Companion.LOG_TAG
import com.likeminds.chatmm.dm.model.CheckDMLimitViewData
import com.likeminds.chatmm.member.model.*
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.utils.ViewDataConverter
import com.likeminds.chatmm.utils.coroutine.launchIO
import com.likeminds.chatmm.utils.model.ITEM_COMMUNITY_MEMBER
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.community.model.*
import com.likeminds.likemindschat.dm.model.CheckDMLimitRequest
import com.likeminds.likemindschat.dm.model.CreateDMChatroomRequest
import com.likeminds.likemindschat.user.model.MemberRole
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class CommunityMembersViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 20
    }

    private val lmChatClient = LMChatClient.getInstance()

    private val _membersResponse by lazy { MutableLiveData<Pair<List<MemberViewData>, Int>>() }
    val membersResponse: LiveData<Pair<List<MemberViewData>, Int>> by lazy { _membersResponse }

    private val _checkDMLimitResponse by lazy { MutableLiveData<Pair<CheckDMLimitViewData, String>>() }
    val checkDMLimitResponse: LiveData<Pair<CheckDMLimitViewData, String>> by lazy { _checkDMLimitResponse }

    private val _dmChatroomId by lazy { MutableLiveData<String>() }
    val dmChatroomId: LiveData<String> by lazy { _dmChatroomId }

    sealed class ErrorMessageEvent {
        data class GetAllMembers(val errorMessage: String?) : ErrorMessageEvent()
        data class SearchMembers(val errorMessage: String?) : ErrorMessageEvent()
        data class CheckDMLimit(val errorMessage: String?) : ErrorMessageEvent()
        data class CreateDMChatroom(val errorMessage: String?) : ErrorMessageEvent()
    }

    private val errorEventChannel by lazy { Channel<ErrorMessageEvent>(Channel.BUFFERED) }
    val errorEventFlow by lazy { errorEventChannel.receiveAsFlow() }

    //gets list of all community members
    fun getAllMembers(showList: Int, page: Int) {
        viewModelScope.launchIO {
            Log.d("PUI", "getAllMembers: ")
            val requestBuilder = GetAllMemberRequest.Builder()
                .page(page)
                .excludeSelfUser(true)

            val request = when (showList) {
                CommunityMembersFilter.ALL_MEMBERS.value -> {
                    requestBuilder.filterMemberRoles(
                        listOf(
                            MemberRole.ADMIN,
                            MemberRole.MEMBER
                        )
                    ).build()
                }

                CommunityMembersFilter.ONLY_CMS.value -> {
                    requestBuilder.filterMemberRoles(
                        listOf(
                            MemberRole.ADMIN
                        )
                    ).build()
                }

                else -> {
                    requestBuilder.filterMemberRoles(
                        listOf(
                            MemberRole.ADMIN,
                            MemberRole.MEMBER
                        )
                    ).build()
                }
            }

            val response = lmChatClient.getAllMember(request)

            if (response.success) {
                onCommunityMemberResponse(response.data, showList)
            } else {
                val errorMessage = response.errorMessage
                Log.e(LOG_TAG, "community/member error: $errorMessage")
                errorEventChannel.send(ErrorMessageEvent.GetAllMembers(errorMessage))
            }
        }
    }

    //process response of getAllMembers
    private fun onCommunityMemberResponse(data: GetAllMemberResponse?, showList: Int) {
        if (data == null) return
        val members = data.members
        val totalOnlyMembers = if (showList == CommunityMembersFilter.ONLY_CMS.value) {
            data.adminsCount ?: 0
        } else {
            data.totalMembers ?: 0
        }

        val membersViewData = members.map {
            ViewDataConverter.convertMember(it).toBuilder()
                .dynamicViewType(ITEM_COMMUNITY_MEMBER)
                .build()
        }

        _membersResponse.postValue(Pair(membersViewData, totalOnlyMembers))
    }

    //search member from community members
    fun searchMembers(showList: Int, page: Int, searchKeyword: String) {
        viewModelScope.launchIO {
            val requestBuilder = SearchMembersRequest.Builder()
                .page(page)
                .pageSize(PAGE_SIZE)
                .search(searchKeyword)
                .searchType(MemberSearchType.NAME)
                .excludeSelfUser(true)

            val request = when (showList) {
                CommunityMembersFilter.ALL_MEMBERS.value -> {
                    requestBuilder.memberStates(listOf(STATE_ADMIN, STATE_MEMBER))
                        .build()
                }

                CommunityMembersFilter.ONLY_CMS.value -> {
                    requestBuilder.memberStates(listOf(STATE_ADMIN))
                        .build()
                }

                else -> {
                    requestBuilder.memberStates(listOf(STATE_ADMIN, STATE_MEMBER))
                        .build()
                }
            }

            val response = lmChatClient.searchMember(request)

            if (response.success) {
                onMemberSearched(response.data)
            } else {
                val errorMessage = response.errorMessage
                Log.e(LOG_TAG, "community/member/search error: $errorMessage")
                errorEventChannel.send(ErrorMessageEvent.SearchMembers(errorMessage))
            }
        }
    }

    //process response of searchMembers
    private fun onMemberSearched(data: SearchMembersResponse?) {
        if (data == null) return
        val members = data.members
        val membersCount = data.recordsCount

        val membersViewData = members.map {
            ViewDataConverter.convertMember(it).toBuilder()
                .dynamicViewType(ITEM_COMMUNITY_MEMBER)
                .build()
        }

        _membersResponse.postValue(Pair(membersViewData, membersCount))
    }

    //checks dm limit
    fun checkDMLimit(uuid: String) {
        viewModelScope.launchIO {
            val request = CheckDMLimitRequest.Builder()
                .uuid(uuid)
                .build()

            val response = lmChatClient.checkDMLimit(request)

            if (response.success) {
                val data = response.data
                data?.let {
                    val checkDMViewData = ViewDataConverter.convertCheckDMLimit(it)
                    _checkDMLimitResponse.postValue(Pair(checkDMViewData, uuid))
                }
            } else {
                val errorMessage = response.errorMessage
                Log.e(LOG_TAG, "check/dm/limit failed -> $errorMessage")
                errorEventChannel.send(ErrorMessageEvent.CheckDMLimit(errorMessage))
            }
        }
    }

    // creates a DM chatroom with the user
    fun createDMChatroom(uuid: String) {
        viewModelScope.launchIO {
            val request = CreateDMChatroomRequest.Builder()
                .uuid(uuid)
                .build()

            val response = lmChatClient.createDMChatroom(request)

            if (response.success) {
                val data = response.data

                data?.let {
                    val chatroomLocal = it.chatroom

                    _dmChatroomId.postValue(chatroomLocal.id)
                }
            } else {
                errorEventChannel.send(ErrorMessageEvent.CreateDMChatroom(response.errorMessage))
            }
        }
    }

    fun createDMAllMemberResult(chatroomId: String): CommunityMembersResultExtras {
        return CommunityMembersResultExtras.Builder()
            .chatroomId(chatroomId)
            .build()
    }
}