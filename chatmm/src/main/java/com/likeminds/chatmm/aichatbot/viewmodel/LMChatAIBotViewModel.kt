package com.likeminds.chatmm.aichatbot.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.likeminds.chatmm.chatroom.detail.viewmodel.ChatroomDetailViewModel.ErrorMessageEvent
import com.likeminds.chatmm.utils.coroutine.launchIO
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.community.model.GetAIChatbotsRequest
import com.likeminds.likemindschat.dm.model.CheckDMStatusRequest
import com.likeminds.likemindschat.dm.model.CreateDMChatroomRequest
import com.likeminds.likemindschat.dm.model.DMRequestFrom
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

class LMChatAIBotViewModel @Inject constructor() :
    ViewModel() {

    private val lmChatClient = LMChatClient.getInstance()

    private val _chatroomId: MutableLiveData<String> by lazy { MutableLiveData("") }
    val chatroomId: LiveData<String> = _chatroomId

    private val errorEventChannel = Channel<ErrorMessageEvent>(Channel.BUFFERED)
    val errorMessageFlow = errorEventChannel.receiveAsFlow()

    sealed class ErrorMessageEvent {
        data class GetAIChatbots(val errorMessage: String?) : ErrorMessageEvent()
        data class CheckDMStatus(val errorMessage: String?) : ErrorMessageEvent()
        data class CreateDMChatroom(val errorMessage: String?) : ErrorMessageEvent()
    }

    fun getAIChatbots() {
        viewModelScope.launchIO {
            val request = GetAIChatbotsRequest.Builder()
                .page(1)
                .pageSize(10)
                .build()

            val response = lmChatClient.getAIChatbots(request)

            if (response.success) {
                val chatBot = response.data?.users?.get(0) ?: return@launchIO

                val chatBotUUID = chatBot.sdkClientInfo?.uuid ?: return@launchIO
                checkDMStatus(chatBotUUID)
            } else {
                errorEventChannel.send(ErrorMessageEvent.GetAIChatbots(response.errorMessage))
            }
        }
    }

    private fun checkDMStatus(chatBotUUID: String) {
        viewModelScope.launchIO {
            val request = CheckDMStatusRequest.Builder()
                .uuid(chatBotUUID)
                .requestFrom(DMRequestFrom.MEMBER_PROFILE)
                .build()

            val response = lmChatClient.checkDMStatus(request)

            if (response.success) {
                val ctaUrl = response.data?.cta

                if (!ctaUrl.isNullOrEmpty()) {
                    val chatroomId = Uri.parse(ctaUrl).getQueryParameter("chatroom_id")

                    if (!chatroomId.isNullOrEmpty()) {
                        _chatroomId.postValue(chatroomId)
                    } else {
                        createDMChatroom(chatBotUUID)
                    }
                } else {
                    createDMChatroom(chatBotUUID)
                }
            } else {
                errorEventChannel.send(ErrorMessageEvent.CheckDMStatus(response.errorMessage))
            }
        }
    }

    private fun createDMChatroom(chatBotUUID: String) {
        viewModelScope.launchIO {
            val request = CreateDMChatroomRequest.Builder()
                .uuid(chatBotUUID)
                .build()

            val response = lmChatClient.createDMChatroom(request)

            if (response.success) {
                val chatroomId = response.data?.chatroom?.id ?: return@launchIO
                _chatroomId.postValue(chatroomId)
            } else {
                errorEventChannel.send(ErrorMessageEvent.CreateDMChatroom(response.errorMessage))
            }
        }
    }
}