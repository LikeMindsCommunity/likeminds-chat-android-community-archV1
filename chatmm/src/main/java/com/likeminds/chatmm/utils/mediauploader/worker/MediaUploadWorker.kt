package com.likeminds.chatmm.utils.mediauploader.worker

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.likeminds.chatmm.SDKApplication
import com.likeminds.chatmm.conversation.model.AttachmentViewData
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.utils.ViewDataConverter
import com.likeminds.chatmm.utils.mediauploader.model.*
import com.likeminds.chatmm.utils.mediauploader.utils.WorkerUtil.getIntOrNull
import com.likeminds.likemindschat.LMChatClient
import com.likeminds.likemindschat.conversation.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.*

abstract class MediaUploadWorker(
    appContext: Context,
    private val params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    val lmChatClient = LMChatClient.getInstance()

    protected val transferUtility by lazy { SDKApplication.getInstance().transferUtility }

    protected var uploadedCount = 0
    protected val failedIndex by lazy { ArrayList<Int>() }
    protected lateinit var uploadList: ArrayList<GenericFileRequest>
    protected val thumbnailMediaMap by lazy { HashMap<Int, Pair<String?, String?>>() }
    private val progressMap by lazy { HashMap<Int, Pair<Long, Long>>() }

    protected lateinit var conversation: ConversationViewData
    protected lateinit var listOfTaggerUsers: List<String>
    protected var isOtherUserAI: Boolean = false

    abstract fun checkArgs()
    abstract fun init()
    abstract fun uploadFiles(continuation: Continuation<Int>)

    companion object {
        const val ARG_MEDIA_INDEX_LIST = "ARG_MEDIA_INDEX_LIST"
        const val ARG_PROGRESS = "ARG_PROGRESS"
        const val ARG_WORKER_RESULT_TAGGED_USER = "ARG_WORKER_RESULT_TAGGED_USER"

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
            e.printStackTrace()
            return Result.failure()
        }
        return withContext(Dispatchers.IO) {
            val result = suspendCoroutine<Int> { continuation ->
                uploadFiles(continuation)
            }
            return@withContext when (result) {
                WORKER_SUCCESS -> {
                    //call create conversation
                    val postConversationRequestBuilder = PostConversationRequest.Builder()
                        .chatroomId(conversation.chatroomId ?: "")
                        .text(conversation.answer)
                        .temporaryId(conversation.id)
                        .repliedConversationId(conversation.replyConversation?.id)
                        .repliedChatroomId(conversation.replyChatroomId)
                        .attachments(ViewDataConverter.convertAttachmentViewDataList(conversation.attachments))
                        .metadata(JSONObject(json))

//                    val widget = conversation.widgetViewData
//
//                    if (widget?.metadata != null) {
//                        postConversationRequestBuilder.metadata(JSONObject(widget.metadata.toString()))
//                    }

                    if (isOtherUserAI) {
                        postConversationRequestBuilder.triggerBot(true)
                    }

                    val postConversationRequest = postConversationRequestBuilder.build()

                    val postConversationResponse =
                        lmChatClient.postConversation(postConversationRequest)
                    if (postConversationResponse.success) {
                        onConversationPosted(postConversationResponse.data)
                        Result.success(
                            workDataOf(
                                ARG_WORKER_RESULT_TAGGED_USER to listOfTaggerUsers.toTypedArray()
                            )
                        )
                    } else {
                        getFailureResult(failedIndex.toIntArray())
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

    protected fun postConversation(
        response: AWSFileResponse,
        urls: Pair<String?, String?>,
        totalFileCount: Int,
        continuation: Continuation<Int>
    ) {
        //updateConversation
        val attachments = conversation.attachments ?: return

        val index = attachments.indexOfFirst {
            it.index == response.index
        }

        var attachment = attachments[index]
        attachment = attachment.toBuilder()
            .url(urls.first)
            .uri(Uri.parse(urls.first))
            .thumbnail(urls.second)
            .build()
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

    private fun onConversationPosted(data: PostConversationResponse?) {
        val conversation = data?.conversation
        if (conversation != null) {
            //Get widget from widgetMap and add it to updatedConversation
            val widgetId = conversation.widgetId
            val widget = data.widgets[widgetId]

            //update conversation with widget
            val updatedConversation = conversation.toBuilder()
                .widget(widget)
                .build()

            // request to save the posted conversation
            val request = SavePostedConversationRequest.Builder()
                .conversation(updatedConversation)
                .isFromNotification(false)
                .build()

            // update db with response
            lmChatClient.savePostedConversation(request)
        }
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