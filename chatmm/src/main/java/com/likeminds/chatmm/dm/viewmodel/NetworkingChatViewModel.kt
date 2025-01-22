package com.likeminds.chatmm.dm.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.*
import androidx.work.WorkInfo
import com.likeminds.chatmm.dm.model.DMFeedEmptyViewData
import com.likeminds.chatmm.homefeed.model.HomeChatroomListShimmerViewData
import com.likeminds.chatmm.homefeed.model.HomeFeedItemViewData
import com.likeminds.chatmm.homefeed.util.HomeFeedPreferences
import com.likeminds.chatmm.homefeed.util.HomeFeedUtil
import com.likeminds.chatmm.member.model.MemberViewData
import com.likeminds.chatmm.member.util.UserPreferences
import com.likeminds.chatmm.utils.ViewDataConverter
import com.likeminds.chatmm.utils.coroutine.launchIO
import com.likeminds.chatmm.utils.model.BaseViewType
import com.likeminds.chatmm.utils.model.ITEM_DIRECT_MESSAGE
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.chatroom.model.Chatroom
import com.likeminds.likemindschat.dm.model.*
import com.likeminds.likemindschat.homefeed.util.HomeChatroomListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class NetworkingChatViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val homeFeedPreferences: HomeFeedPreferences
) : ViewModel() {

    companion object {
        private const val TAG = "DMFeed VM"
    }

    private val lmChatClient = LMChatClient.getInstance()

    private val compositeDisposable = CompositeDisposable()

    private val _userData = MutableLiveData<MemberViewData?>()
    val userData: LiveData<MemberViewData?> = _userData

    private val allChatRoomsData = mutableListOf<HomeFeedItemViewData>()

    sealed class DMFeedEvent {
        object ShowDMFeedData : DMFeedEvent()
    }

    private val dmFeedChannel = Channel<DMFeedEvent>(Channel.BUFFERED)
    val dmFeedFlow = dmFeedChannel.receiveAsFlow()

    private val errorMessageChannel = Channel<ErrorMessageEvent>(Channel.BUFFERED)
    val errorMessageEventFlow = errorMessageChannel.receiveAsFlow()

    private val _checkDMResponse by lazy { MutableLiveData<CheckDMStatusResponse>() }
    val checkDMResponse: LiveData<CheckDMStatusResponse> by lazy { _checkDMResponse }

    sealed class ErrorMessageEvent {
        data class GetDMChatroom(val errorMessage: String?) : ErrorMessageEvent()

        data class CheckDMStatus(val errorMessage: String?) : ErrorMessageEvent()
    }

    private val chatRoomListShimmerView by lazy {
        HomeChatroomListShimmerViewData.Builder().build()
    }

    fun isDBEmpty(): Boolean {
        return (lmChatClient.getDBEmpty().data?.isDBEmpty ?: false)
    }

    fun syncDMChatrooms(context: Context): Pair<LiveData<MutableList<WorkInfo>>?, LiveData<MutableList<WorkInfo>>?>? {
        return lmChatClient.loadDMChatrooms(context)
    }

    fun setWasChatroomFetched(value: Boolean) {
        homeFeedPreferences.setIsDMFeedShimmerShown(value)
    }

    // gets the user from local db
    fun getUserFromLocalDb() {
        val userResponse = lmChatClient.getLoggedInUser()
        _userData.postValue(ViewDataConverter.convertUser(userResponse.data?.user))
    }

    private val chatroomListener = object : HomeChatroomListener() {
        override fun initial(chatrooms: List<Chatroom>) {
            showInitialChatrooms(chatrooms)
        }

        override fun onChanged(
            removedIndex: List<Int>,
            inserted: List<Pair<Int, Chatroom>>,
            changed: List<Pair<Int, Chatroom>>
        ) {
            updateChatroomChanges(removedIndex, inserted, changed)
        }

        override fun onError(throwable: Throwable) {
            viewModelScope.launchIO {
                errorMessageChannel.send(ErrorMessageEvent.GetDMChatroom(throwable.message))
            }
            Log.e(TAG, "chatroomListener", throwable)
        }
    }

    //returns initial list of chatrooms
    private fun showInitialChatrooms(chatrooms: List<Chatroom>) {
        viewModelScope.launch {
            val chatViewDataList = chatrooms.map { chatroom ->
                HomeFeedUtil.getChatRoomViewData(
                    chatroom,
                    userPreferences,
                    ITEM_DIRECT_MESSAGE
                )
            }
            allChatRoomsData.clear()
            allChatRoomsData.addAll(chatViewDataList)
            dmFeedChannel.send(DMFeedEvent.ShowDMFeedData)
        }
    }

    //returns the updates to the intial list
    private fun updateChatroomChanges(
        removedId: List<Int>,
        inserted: List<Pair<Int, Chatroom>>,
        changed: List<Pair<Int, Chatroom>>
    ) = viewModelScope.launch {
        removedId.forEach { index ->
            allChatRoomsData.removeAt(index)
        }
        val insertedChatViewDataList = inserted.map { pair ->
            Pair(
                pair.first, HomeFeedUtil.getChatRoomViewData(
                    pair.second,
                    userPreferences,
                    ITEM_DIRECT_MESSAGE
                )
            )
        }
        val changedChatViewDataList = changed.map { pair ->
            Pair(
                pair.first, HomeFeedUtil.getChatRoomViewData(
                    pair.second,
                    userPreferences,
                    ITEM_DIRECT_MESSAGE
                )
            )
        }

        insertedChatViewDataList.forEach { pair ->
            val index = pair.first
            val chatViewData = pair.second
            allChatRoomsData.add(index, chatViewData)
        }

        changedChatViewDataList.forEach { pair ->
            val index = pair.first
            val chatViewData = pair.second
            allChatRoomsData[index] = chatViewData
        }
        dmFeedChannel.send(DMFeedEvent.ShowDMFeedData)
    }

    //returns the list of dm chatrooms based on conditions
    fun fetchDMChatrooms(): List<BaseViewType> {
        val dataList = mutableListOf<BaseViewType>()

        val wasDMChatroomsFetched = homeFeedPreferences.getIsDMFeedShimmerShown()
        when {
            !wasDMChatroomsFetched -> {
                dataList.add(chatRoomListShimmerView)
            }

            allChatRoomsData.isNotEmpty() -> {
                dataList.addAll(allChatRoomsData)
            }

            else -> {
                dataList.add(getEmptyChatView())
            }
        }
        return dataList
    }

    private fun getEmptyChatView(): DMFeedEmptyViewData {
        return DMFeedEmptyViewData.Builder().build()
    }

    fun refetchDMChatrooms() {
        compositeDisposable.clear()
        observeDMChatrooms()
    }

    //observes all dm chatrooms from local and return in [chatroomListener]
    fun observeDMChatrooms() {
        compositeDisposable.clear()
        val disposable =
            lmChatClient.observeDMChatrooms(chatroomListener)
                ?.subscribeOn(Schedulers.computation())
                ?.observeOn(Schedulers.newThread())
                ?.subscribe() ?: return
        compositeDisposable.add(disposable)
    }

    //check whether user can intiate DM or not
    fun checkDMStatus() {
        viewModelScope.launchIO {

            val request = CheckDMStatusRequest.Builder()
                .requestFrom(DMRequestFrom.DM_FEED)
                .build()

            val response = lmChatClient.checkDMStatus(request)

            if (response.success) {
                val data = response.data
                data?.let {
                    _checkDMResponse.postValue(it)
                }
            } else {
                errorMessageChannel.send(ErrorMessageEvent.CheckDMStatus(response.errorMessage))
            }
        }
    }

    fun observeLiveHomeFeed(context: Context) {
        lmChatClient.observeLiveDMChatroom(context)
    }

    fun removeLiveHomeFeedListener() {
        lmChatClient.removeLiveDMChatroomListener()
    }

    override fun onCleared() {
        compositeDisposable.dispose()
        super.onCleared()
    }
}