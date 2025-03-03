package com.likeminds.chatmm.utils.mediauploader.worker

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.*
import com.google.gson.Gson
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.conversation.model.AttachmentViewData
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.utils.ViewDataConverter
import com.likeminds.chatmm.utils.mediauploader.model.*
import com.likeminds.chatmm.utils.mediauploader.utils.WorkerUtil.getIntOrNull
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.conversation.model.PostConversationRequest
import com.likeminds.likemindschat.conversation.model.UpdateConversationRequest
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.*

abstract class ConversationWorker(
    appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    val lmChatClient = LMChatClient.getInstance()

    private val gson = Gson()

    protected val transferUtility by lazy { SDKApplication.getInstance().transferUtility }

    protected var uploadedCount = 0
    protected val failedIndex by lazy { ArrayList<Int>() }
    protected lateinit var uploadList: ArrayList<GenericFileRequest>
    protected val thumbnailMediaMap by lazy { HashMap<Int, Pair<String?, String?>>() }
    private val progressMap by lazy { HashMap<Int, Pair<Long, Long>>() }

    protected lateinit var conversation: ConversationViewData
    protected var isOtherUserAI: Boolean = false

    abstract fun checkArgs()
    abstract fun init()
    abstract fun uploadFiles(continuation: Continuation<Int>)

    companion object {
        const val ARG_MEDIA_INDEX_LIST = "ARG_MEDIA_INDEX_LIST"
        const val ARG_PROGRESS = "ARG_PROGRESS"
        const val OUTPUT_POST_CONVERSATION_RESPONSE = "OUTPUT_POST_CONVERSATION_RESPONSE"

        fun getProgress(workInfo: WorkInfo): Pair<Long, Long>? {
            val progress = workInfo.progress.getLongArray(ARG_PROGRESS)
            if (progress == null || progress.size != 2) {
                return null
            }
            return Pair(progress[0], progress[1])
        }
    }

    fun require(key: String) {
        if (!containsParam(key)) {
            throw Error("$key is required")
        }
    }

    override suspend fun doWork(): Result {
        try {
            checkArgs()
            init()
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            e.printStackTrace()
            return Result.failure()
        }
        return withContext(Dispatchers.IO) {
            val result = suspendCoroutine<Int> { continuation ->
                uploadFiles(continuation)
            }
            return@withContext when (result) {
                WORKER_SUCCESS -> {
                    // update the attachments uploaded variables in the local DB
                    if (!conversation.attachments.isNullOrEmpty()) {
                        conversation = conversation.toBuilder()
                            .attachmentsUploaded(true)
                            .attachmentsUploadedEpoch(System.currentTimeMillis())
                            .build()
                        val updateConversationRequest = UpdateConversationRequest.Builder()
                            .conversation(ViewDataConverter.convertConversation(conversation))
                            .build()

                        lmChatClient.updateConversation(updateConversationRequest)
                    }

                    //call create conversation
                    val postConversationRequestBuilder = PostConversationRequest.Builder()
                        .chatroomId(conversation.chatroomId ?: "")
                        .text(conversation.answer)
                        .temporaryId(conversation.id)
                        .repliedConversationId(conversation.replyConversation?.id)
                        .repliedChatroomId(conversation.replyChatroomId)
                        .attachments(ViewDataConverter.convertAttachmentViewDataList(conversation.attachments))

                    val widget = conversation.widgetViewData

                    if (widget?.metadata != null) {
                        postConversationRequestBuilder.metadata(JSONObject(widget.metadata.toString()))
                    }

                    if (isOtherUserAI) {
                        postConversationRequestBuilder.triggerBot(true)
                    }

                    val postConversationRequest = postConversationRequestBuilder.build()

                    val postConversationResponse =
                        lmChatClient.postConversation(postConversationRequest)
                    if (postConversationResponse.success) {
                        // Serialize response to JSON
                        val outputJson = gson.toJson(postConversationResponse.data)

                        // Pass the created conversation as output
                        val outputData = workDataOf(OUTPUT_POST_CONVERSATION_RESPONSE to outputJson)

                        Result.success(outputData)
                    } else {
                        // Serialize response to JSON
                        val outputJson = gson.toJson(postConversationResponse.errorMessage)

                        // Pass the created conversation as output
                        val outputData = workDataOf(OUTPUT_POST_CONVERSATION_RESPONSE to outputJson)

                        Result.failure(outputData)
                    }
                }

                WORKER_RETRY -> {
                    Result.retry()
                }

                else -> {
                    getFailureResult(failedIndex.toIntArray())
                }
            }
        }
    }

    private fun getFailureResult(failedArrayIndex: IntArray = IntArray(0)): Result {
        return Result.failure(
            Data.Builder()
                .putIntArray(ARG_MEDIA_INDEX_LIST, failedArrayIndex)
                .build()
        )
    }

    protected fun setProgress(id: Int, bytesCurrent: Long, bytesTotal: Long) {
        progressMap[id] = Pair(bytesCurrent, bytesTotal)
        var averageBytesCurrent = 0L
        var averageBytesTotal = 0L
        progressMap.values.forEach {
            averageBytesCurrent += it.first
            averageBytesTotal += it.second
        }
        if (averageBytesCurrent > 0L && averageBytesTotal > 0L) {
            setProgressAsync(
                Data.Builder()
                    .putLongArray(ARG_PROGRESS, longArrayOf(averageBytesCurrent, averageBytesTotal))
                    .build()
            )
        }
    }

    protected fun getStringParam(key: String): String {
        return params.inputData.getString(key)
            ?: throw Error("$key is required")
    }

    protected fun getIntParam(key: String): Int {
        return params.inputData.getIntOrNull(key)
            ?: throw Error("$key is required")
    }

    protected fun getBooleanParam(key: String): Boolean {
        return params.inputData.getBoolean(key, false)
    }

    protected fun getStringArray(key: String): List<String> {
        return params.inputData.getStringArray(key)?.toList() ?: throw Error("$key is required")
    }

    protected fun containsParam(key: String): Boolean {
        return params.inputData.keyValueMap.containsKey(key)
    }

    protected fun createAWSRequestList(
        thumbnailsToUpload: List<AttachmentViewData>,
        attachmentsToUpload: List<AttachmentViewData>
    ): ArrayList<GenericFileRequest> {
        val awsFileRequestList = ArrayList<GenericFileRequest>()
        thumbnailsToUpload.forEach { attachment ->
            val request = GenericFileRequest.Builder()
                .fileType(attachment.type)
                .awsFolderPath(attachment.thumbnailAWSFolderPath ?: "")
                .localFilePath(attachment.thumbnailLocalFilePath)
                .index(attachment.index ?: 0)
                .isThumbnail(true)
                .build()
            awsFileRequestList.add(request)
        }
        attachmentsToUpload.forEach { attachment ->
            val request = GenericFileRequest.Builder()
                .name(attachment.name)
                .fileType(attachment.type)
                .awsFolderPath(attachment.awsFolderPath ?: "")
                .localFilePath(attachment.localFilePath)
                .index(attachment.index ?: 0)
                .width(attachment.width)
                .height(attachment.height)
                .hasThumbnail(attachment.thumbnailAWSFolderPath != null)
                .meta(attachment.meta)
                .build()
            awsFileRequestList.add(request)
        }
        return awsFileRequestList
    }

    protected fun updateAttachmentUploaded(
        response: AWSFileResponse,
        urls: Pair<String?, String?>,
        totalFileCount: Int,
        continuation: Continuation<Int>,
        isThumbnail: Boolean?
    ) {
        //updateConversation
        val attachments = conversation.attachments ?: return

        val index = attachments.indexOfFirst {
            it.index == response.index
        }

        var attachment = attachments[index]

        // if the uploaded item is a thumbnail then we reset its aws folder path otherwise keep it the same
        val thumbnailAWSFolderPath = if (isThumbnail == true) {
            ""
        } else {
            attachment.thumbnailAWSFolderPath
        }

        val attachmentBuilder = attachment.toBuilder()
            .url(urls.first)
            .uri(Uri.parse(urls.first))
            .thumbnail(urls.second)
            .thumbnailAWSFolderPath(thumbnailAWSFolderPath)

        // if the current uploaded media is not a thumbnail that means our attachment has been uploaded successfully
        if (isThumbnail != true) {
            attachmentBuilder.isUploaded(true)
        }

        attachment = attachmentBuilder.build()

        attachments[index] = attachment

        conversation = conversation.toBuilder()
            .attachments(attachments)
            .build()

        uploadedCount += 1

        //update local db
        val updateConversationRequest = UpdateConversationRequest.Builder()
            .conversation(ViewDataConverter.convertConversation(conversation))
            .build()
        lmChatClient.updateConversation(updateConversationRequest)

        checkWorkerComplete(totalFileCount, continuation)
    }

    protected fun checkWorkerComplete(
        totalFilesToUpload: Int,
        continuation: Continuation<Int>,
    ) {
        if (totalFilesToUpload == uploadedCount + failedIndex.size) {
            if (totalFilesToUpload == uploadedCount) {
                continuation.resume(WORKER_SUCCESS)
            } else {
                continuation.resume(WORKER_FAILURE)
            }
        }
    }
}