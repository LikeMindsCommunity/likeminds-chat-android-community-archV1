package com.likeminds.chatmm.utils.mediauploader.worker

import com.likeminds.chatmm.media.model.InternalMediaType
import com.likeminds.chatmm.utils.mediauploader.model.AWSFileResponse
import java.io.File

object UploadHelper {
    private val awsFileResponses: ArrayList<AWSFileResponse> = arrayListOf()

    private const val FOLDER_COLLABCARD = "files/collabcard"
    private const val FOLDER_CONVERSATION = "conversation"

    fun addAWSFileResponse(awsFileResponse: AWSFileResponse) {
        awsFileResponses.add(awsFileResponse)
    }

    fun removeAWSFileResponse(awsFileResponse: AWSFileResponse) {
        awsFileResponses.remove(awsFileResponse)
    }

    fun getAWSFileResponse(awsFolderPath: String?): AWSFileResponse? {
        if (awsFolderPath == null)
            return null
        for (awsFileResponse in awsFileResponses) {
            if (awsFileResponse.awsFolderPath == awsFolderPath)
                return awsFileResponse
        }
        return null
    }

    fun getAWSFileResponses(uuid: String?): List<AWSFileResponse> {
        return awsFileResponses.filter { it.uuid == uuid }
    }

    /**
     * return the attachment file path on AWS S3
     */
    fun getAttachmentFilePath(
        chatroomId: String?,
        loggedInUserUUID: String,
        attachmentType: String,
        file: File,
        isThumbnail: Boolean = false
    ): String {
        return "$FOLDER_COLLABCARD/$chatroomId/$FOLDER_CONVERSATION/$loggedInUserUUID/${
            InternalMediaType.getMediaFileInitial(attachmentType, isThumbnail)
        }${System.currentTimeMillis()}.${
            InternalMediaType.getMediaFileExtension(attachmentType, file, isThumbnail)
        }"
    }
}