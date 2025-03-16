package com.likeminds.chatmm.dm.util

import android.util.Log
import com.likeminds.chatmm.utils.TimeUtil
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.dm.model.*

object LMChatDMUtil {

    const val TAG = "LMChatDMUtil"

    suspend fun createOrGetExistingDMChatroom(userUUID: String): Pair<String?, String?> {
        val lmChatClient = LMChatClient.getInstance()

        //Check if chatroom already exist in local db
        val getExistingDMChatroomRequest =
            GetExistingDMChatroomRequest.Builder()
                .userUUID(userUUID)
                .build()
        val existingDMChatroom = lmChatClient.getExistingDMChatroom(getExistingDMChatroomRequest)

        if (existingDMChatroom.success) {
            Log.d(TAG, "chatroom from existingDMChatroom function")
            return Pair(existingDMChatroom.data?.id, null)
        }

        //Call DM limit API to get chatroomId from Server or create new chatroom
        val checkDMLimitRequest = CheckDMLimitRequest.Builder()
            .uuid(userUUID)
            .build()

        val checkDMLimitResponse = lmChatClient.checkDMLimit(checkDMLimitRequest)

        if (checkDMLimitResponse.success) {
            val dmLimitData = checkDMLimitResponse.data

            val chatroomId = dmLimitData?.chatroomId
            val isRequestDMLimitExceeded = dmLimitData?.isRequestDMLimitExceeded
            return when {
                chatroomId != null -> {
                    Log.d(TAG, "chatroom from dm limit api")
                    Pair(chatroomId, null)
                }

                isRequestDMLimitExceeded == false -> {
                    val createDMChatroomRequest = CreateDMChatroomRequest.Builder()
                        .uuid(userUUID)
                        .build()
                    val createDMChatroomResponse =
                        lmChatClient.createDMChatroom(createDMChatroomRequest)
                    Log.d(TAG, "chatroom from create dm chatroom")

                    if (createDMChatroomResponse.success) {
                        Pair(createDMChatroomResponse.data?.chatroom?.id, null)
                    } else {
                        Pair(null, createDMChatroomResponse.errorMessage)
                    }
                }

                else -> {
                    val newRequestDMString =
                        TimeUtil.getRelativeTimeInString(
                            System.currentTimeMillis(),
                            dmLimitData?.newRequestDMTimestamp ?: 0
                        )
                    Log.e(TAG, "DM Limit Exceeded, Retry after $newRequestDMString")
                    Pair(null, "DM Limit Exceeded, Retry after $newRequestDMString")
                }
            }

        } else {
            Log.e(TAG, "Error in DM limit API")
            return Pair(null, checkDMLimitResponse.errorMessage)
        }
    }
}