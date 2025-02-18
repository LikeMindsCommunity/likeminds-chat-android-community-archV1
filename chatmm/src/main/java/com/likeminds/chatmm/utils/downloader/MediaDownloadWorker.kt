package com.likeminds.chatmm.utils.downloader

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.likeminds.chatmm.R
import com.likeminds.chatmm.media.model.IMAGE
import com.likeminds.chatmm.media.model.VIDEO
import com.likeminds.chatmm.utils.ValueUtils.getMediaType
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import kotlinx.coroutines.TimeoutCancellationException
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

@Suppress("BlockingMethodInNonBlockingContext", "DEPRECATION")
internal class MediaDownloadWorker(
    val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    companion object {
        const val NAME = "Download Worker"

        const val ARG_ATTACHMENT_URL = "ARG_ATTACHMENT_URL"
        const val ARG_FAILURE_REASON = "AGR_FAILURE_REASON"
        const val NOTIFICATION_ICON = "NOTIFICATION_ICON"
    }

    private val url = params.inputData.getString(ARG_ATTACHMENT_URL)
    private val notificationIcon = params.inputData.getInt(NOTIFICATION_ICON, 0)

    private val notificationId by lazy { System.currentTimeMillis().toInt() }

    override fun doWork(): Result {
        val remoteUri = DownloadUtil.getDownloadUri(url)
        val fileName = DownloadUtil.getFileName(remoteUri)

        if (url.isNullOrEmpty() || remoteUri == null || fileName == null) {
            return getFailureResult("Attachment url is empty")
        }

        val fileType = remoteUri.getMediaType(context)
        val contentResolver = context.contentResolver
        val localFileUri = DownloadUtil.getLocalFileUri(
            context,
            remoteUri,
            fileName,
            fileType,
        ) ?: return getFailureResult("Some internal error occurred")

        val outputStream = contentResolver.openOutputStream(localFileUri)
            ?: return getFailureResult("Some internal error occurred")

        var notificationBuilder: NotificationCompat.Builder? = null
        var notificationManager: NotificationManagerCompat? = null
        if (fileType == VIDEO) {
            val notificationTitle = getNotificationTitle(fileType)
            notificationBuilder =
                DownloadUtil.getNotificationBuilder(context, notificationTitle, notificationIcon)
            notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, notificationBuilder.build())
        }

        return try {
            val okHttpClient = getOkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = okHttpClient.newCall(request).execute()
            val body = response.body
            val responseCode = response.code

            if (responseCode >= HttpURLConnection.HTTP_OK &&
                responseCode < HttpURLConnection.HTTP_MULT_CHOICE &&
                body != null
            ) {
                val length = body.contentLength()
                val isIndeterminate = length == -1L
                body.byteStream().apply {
                    outputStream.use { fileOut ->
                        var bytesCopied = 0
                        val buffer = ByteArray(8 * 1024)
                        var bytes = read(buffer)
                        while (bytes >= 0) {
                            fileOut.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            bytes = read(buffer)
                            val progress = ((bytesCopied * 100) / length).toInt()
                            Log.d(NAME, "Indeterminate - $isIndeterminate, Progress : $progress")
                        }
                    }
                }

                showSuccessNotification(notificationBuilder, notificationManager)
                Result.success()
            } else {
                showFailedNotification(notificationBuilder, notificationManager)
                getFailureResult(context.getString(R.string.lm_chat_unknown_error_occurred))
            }
        } catch (e: TimeoutCancellationException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            showFailedNotification(notificationBuilder, notificationManager)
            getFailureResult(context.getString(R.string.lm_chat_connection_timed_out))
        } catch (t: Throwable) {
            LMChatLogger.getInstance()?.handleException(
                t.message ?: "",
                t.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            showFailedNotification(notificationBuilder, notificationManager)
            getFailureResult(context.getString(R.string.lm_chat_failed_to_connect))
        }
    }

    private fun showFailedNotification(
        builder: NotificationCompat.Builder?,
        manager: NotificationManagerCompat?
    ) {
        if (builder != null && manager != null) {
            builder.setContentText(context.getString(R.string.lm_chat_download_failed))
                .setProgress(0, 0, false)
            manager.notify(notificationId, builder.build())
        }
    }

    private fun showSuccessNotification(
        builder: NotificationCompat.Builder?,
        manager: NotificationManagerCompat?
    ) {
        if (builder != null && manager != null) {
            builder.setContentText(context.getString(R.string.lm_chat_download_completed))
                .setProgress(0, 0, false)
            manager.notify(notificationId, builder.build())
        }
    }

    private fun getNotificationTitle(type: String?): String {
        return when (type) {
            IMAGE -> context.getString(R.string.lm_chat_image_download)
            VIDEO -> context.getString(R.string.lm_chat_video_download)
            else -> context.getString(R.string.lm_chat_media_download)
        }
    }

    private fun getOkHttpClient(): OkHttpClient {
        return OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun getFailureResult(reason: String): Result {
        return Result.failure(
            Data.Builder().putString(ARG_FAILURE_REASON, reason).build()
        )
    }

}