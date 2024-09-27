package com.likeminds.chatmm.utils.mediauploader.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.likeminds.chatmm.conversation.model.ConversationViewData
import com.likeminds.chatmm.media.model.IMAGE
import com.likeminds.chatmm.utils.ViewDataConverter
import com.likeminds.chatmm.utils.mediauploader.model.*
import com.likeminds.chatmm.utils.mediauploader.utils.FileHelper
import com.likeminds.likemindschat.conversation.model.GetConversationRequest
import com.likeminds.likemindschat.conversation.model.UpdateConversationRequest
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ConversationMediaUploadWorker(
    context: Context, workerParams: WorkerParameters
) : MediaUploadWorker(context, workerParams) {

    private val conversationId by lazy { getStringParam(ARG_CONVERSATION_ID) }
    private val totalMediaCount by lazy { getIntParam(ARG_TOTAL_MEDIA_COUNT) }

    private lateinit var conversation: ConversationViewData

    companion object {
        const val ARG_CONVERSATION_ID = "ARG_CONVERSATION_ID"
        const val ARG_TOTAL_MEDIA_COUNT = "ARG_TOTAL_MEDIA_COUNT"

        fun getInstance(conversationId: String, totalMediaCount: Int): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<ConversationMediaUploadWorker>()
                .setInputData(
                    workDataOf(
                        ARG_CONVERSATION_ID to conversationId,
                        ARG_TOTAL_MEDIA_COUNT to totalMediaCount
                    )
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(conversationId)
                .build()
        }
    }

    override fun checkArgs() {
        require(ARG_CONVERSATION_ID)
        require(ARG_TOTAL_MEDIA_COUNT)
    }

    override fun init() {
        val getConversationRequest = GetConversationRequest.Builder()
            .conversationId(conversationId)
            .build()
        val response = lmChatClient.getConversation(getConversationRequest)
        Log.d("PUI", "init upload worker: ${response.data?.conversation?.id}")
        conversation = ViewDataConverter.convertConversation(response.data?.conversation) ?: return

        Log.d(
            "PUI", """
                init upload worker
                attachment url: ${conversation.attachments?.map { it.url }}
                attachment uri: ${conversation.attachments?.map { it.uri }}
                attachment file path: ${conversation.attachments?.map { it.localFilePath }}
            """.trimIndent()
        )

    }

    override fun uploadFiles(continuation: Continuation<Int>) {
        val attachmentsToUpload = conversation.attachmentsToUpload() ?: emptyList()
        val thumbnailsToUpload = conversation.thumbnailsToUpload() ?: emptyList()
        val totalFilesToUpload = attachmentsToUpload.size

        if (attachmentsToUpload.isEmpty() && thumbnailsToUpload.isEmpty()) {
            continuation.resume(WORKER_SUCCESS)
            return
        }

        uploadList = createAWSRequestList(thumbnailsToUpload, attachmentsToUpload)
        uploadList.forEach { request ->
            val resumeAWSFileResponse =
                UploadHelper.getAWSFileResponse(request.awsFolderPath)
            if (resumeAWSFileResponse != null) {
                resumeAWSUpload(resumeAWSFileResponse, totalFilesToUpload, continuation, request)
            } else {
                createAWSUpload(request, totalFilesToUpload, continuation)
            }
        }
    }

    private fun resumeAWSUpload(
        resumeAWSFileResponse: AWSFileResponse,
        totalFilesToUpload: Int,
        continuation: Continuation<Int>,
        request: GenericFileRequest
    ) {
        val resume = transferUtility.resume(resumeAWSFileResponse.transferObserver!!.id)
        if (resume == null) {
            createAWSUpload(request, totalFilesToUpload, continuation)
        } else {
            uploadAWSFile(resumeAWSFileResponse, totalFilesToUpload, continuation)
        }
    }

    private fun createAWSUpload(
        request: GenericFileRequest,
        totalFilesToUpload: Int,
        continuation: Continuation<Int>
    ) {
        val awsFileResponse = uploadFile(request, conversation.uploadWorkerUUID)
        if (awsFileResponse != null) {
            UploadHelper.addAWSFileResponse(awsFileResponse)
            uploadAWSFile(awsFileResponse, totalFilesToUpload, continuation)
        }
    }

    /**
     * Starts Uploading file on AWS.
     * @param request A [GenericFileRequest] object
     * @return [AWSFileResponse] containing aws transfer utility objects and keys
     */
    private fun uploadFile(request: GenericFileRequest, uuid: String? = null): AWSFileResponse? {
        val filePath = request.localFilePath ?: return null
        val file = if (request.fileType == IMAGE) {
            FileHelper.compressFile(applicationContext, filePath)
        } else {
            File(filePath)
        }

        Log.d(
            "PUI", """
            upload file in worker
            file: $filePath
        """.trimIndent()
        )

        val observer = transferUtility.upload(
            request.awsFolderPath,
            file,
            CannedAccessControlList.PublicRead
        )
        return AWSFileResponse.Builder()
            .transferObserver(observer)
            .name(request.name ?: "")
            .awsFolderPath(request.awsFolderPath)
            .index(request.index)
            .fileType(request.fileType)
            .width(request.width)
            .height(request.height)
            .isThumbnail(request.isThumbnail)
            .hasThumbnail(request.hasThumbnail)
            .duration(request.meta?.duration)
            .pageCount(request.meta?.numberOfPage)
            .size(request.meta?.size)
            .uuid(uuid)
            .build()
    }

    private fun uploadAWSFile(
        awsFileResponse: AWSFileResponse,
        totalFilesToUpload: Int,
        continuation: Continuation<Int>
    ) {
        val observer = awsFileResponse.transferObserver ?: return
        setProgress(observer.id, observer.bytesTransferred, observer.bytesTotal)
        observer.setTransferListener(object : TransferListener {
            override fun onStateChanged(id: Int, state: TransferState?) {
                onStateChanged(awsFileResponse, state, totalFilesToUpload, continuation)
            }

            override fun onProgressChanged(id: Int, bytesCurrent: Long, bytesTotal: Long) {
                setProgress(id, bytesCurrent, bytesTotal)
            }

            override fun onError(id: Int, ex: Exception?) {
                ex?.printStackTrace()
                failedIndex.add(awsFileResponse.index)
                checkWorkerComplete(totalFilesToUpload, continuation)
            }
        })
    }

    private fun onStateChanged(
        response: AWSFileResponse,
        state: TransferState?,
        totalFilesToUpload: Int,
        continuation: Continuation<Int>
    ) {
        if (isStopped) {
            return
        }
        when (state) {
            TransferState.COMPLETED -> {
                UploadHelper.removeAWSFileResponse(response)
                uploadedCount += 1
                checkWorkerComplete(totalFilesToUpload, continuation)
                Log.d(
                    "UPL", """
                    upload complete
                    isThumbnail: ${response.isThumbnail}
                    hasThumbnail: ${response.hasThumbnail}
                    awsFolderPath: ${response.awsFolderPath}
                    responseURL: ${response.downloadUrl}
                """.trimIndent()
                )

                if (response.isThumbnail == true) {
                    Log.d("UPL","case1 isThumb")
                    val attachments = conversation.attachments ?: return

                    val index = attachments.indexOfFirst { attachmentViewData ->
                        attachmentViewData.thumbnailAWSFolderPath == response.awsFolderPath
                    }
                    Log.d("UPL","")

                    var attachment = attachments[index]

                    attachment = attachment.toBuilder()
                        .thumbnail(response.downloadUrl)
                        .build()

                    attachments[index] = attachment

                    conversation = conversation.toBuilder().attachments(attachments).build()
                } else {
                    val attachments = conversation.attachments ?: return

                    val index = attachments.indexOfFirst { attachmentViewData ->
                        attachmentViewData.awsFolderPath == response.awsFolderPath
                    }

                    var attachment = attachments[index]

                    attachment = attachment.toBuilder()
                        .url(response.downloadUrl)
                        .build()

                    attachments[index] = attachment

                    conversation = conversation.toBuilder().attachments(attachments).build()
                }

                val updateConversationRequest = UpdateConversationRequest.Builder()
                    .conversation(ViewDataConverter.convertConversation(conversation))
                    .build()

                lmChatClient.updateConversation(updateConversationRequest)
            }

            TransferState.FAILED -> {
                failedIndex.add(response.index)
                checkWorkerComplete(totalFilesToUpload, continuation)
            }

            else -> {
            }
        }
    }
}