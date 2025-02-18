package com.likeminds.chatmm.utils.downloader

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.lifecycle.MediatorLiveData
import androidx.work.*
import com.likeminds.chatmm.R
import com.likeminds.chatmm.media.model.IMAGE
import com.likeminds.chatmm.media.model.InternalMediaType
import com.likeminds.chatmm.media.model.VIDEO
import com.likeminds.chatmm.utils.ValueUtils.getMimeType
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import java.io.File
import java.util.concurrent.TimeUnit

object DownloadUtil {

    private const val CHANNEL_ID = "notification_downloader"
    private const val CHANNEL_NAME = "Downloader notifications"
    private const val CHANNEL_DESCRIPTION = "Receive notifications when you download any media file"
    const val DOWNLOAD_DIRECTORY = "LikeMinds"

    fun getToastMessage(context: Context, @InternalMediaType type: String?): String {
        return when (type) {
            IMAGE -> {
                context.getString(R.string.lm_chat_photo_saved_to_gallery)
            }
            VIDEO -> {
                context.getString(R.string.lm_chat_video_saved_to_gallery)
            }
            else -> {
                context.getString(R.string.lm_chat_saved_to_gallery)
            }
        }
    }

    fun getNotificationBuilder(
        context: Context,
        title: String,
        notificationIcon: Int
    ): NotificationCompat.Builder {
        createNotificationChannel(context)
        val icon = if (notificationIcon == 0) {
            R.drawable.lm_chat_ic_notification
        } else {
            notificationIcon
        }
        return NotificationCompat.Builder(
            context,
            CHANNEL_ID
        ).apply {
            setContentTitle(title)
            setContentText(context.getString(R.string.lm_chat_download_in_progress))
            setSmallIcon(icon)
            setProgress(100, 0, true)
            setNotificationSilent()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                vibrationPattern = longArrayOf(0)
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun getDownloadUri(url: String?): Uri? {
        return try {
            Uri.parse(url)
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            null
        }
    }

    fun getFileName(uri: Uri?): String? {
        return uri?.lastPathSegment
    }

    fun getLocalFileUri(
        context: Context,
        remoteUri: Uri,
        fileName: String,
        fileType: String?
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getLocalFileUriForQ(context, remoteUri, fileName, fileType)
        } else {
            getLocalFileUriPreQ(fileName, fileType)
        }
    }

    @Suppress("DEPRECATION")
    private fun getLocalFileUriPreQ(fileName: String, fileType: String?): Uri? {
        val directory = when (fileType) {
            IMAGE -> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            }
            VIDEO -> {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            }
            else -> null
        } ?: return null
        directory.mkdir()
        return File(directory, fileName).toUri()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getLocalFileUriForQ(
        context: Context,
        remoteUri: Uri,
        fileName: String,
        fileType: String?
    ): Uri? {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, remoteUri.getMimeType(context))
        return when (fileType) {
            IMAGE -> {
                contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/" + DOWNLOAD_DIRECTORY
                )
                context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
            }
            VIDEO -> {
                contentValues.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_MOVIES + "/" + DOWNLOAD_DIRECTORY
                )
                context.contentResolver.insert(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
            }
            else -> null
        }
    }

    fun startDownload(
        context: Context,
        url: String,
        notificationIcon: Int
    ): MediatorLiveData<WorkInfo.State>? {
        val workInfo = WorkManager.getInstance(context).getWorkInfosByTag(url).get().firstOrNull()
        if (workInfo != null && !workInfo.state.isFinished) {
            return null
        }
        val worker = OneTimeWorkRequestBuilder<MediaDownloadWorker>()
            .setInputData(
                workDataOf(
                    MediaDownloadWorker.ARG_ATTACHMENT_URL to url,
                    MediaDownloadWorker.NOTIFICATION_ICON to notificationIcon
                )
            )
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setBackoffCriteria(
                BackoffPolicy.LINEAR,
                WorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .addTag(url)
            .build()
        val work = WorkManager.getInstance(context)
            .beginUniqueWork(
                MediaDownloadWorker.NAME,
                ExistingWorkPolicy.REPLACE,
                worker
            )
        work.enqueue()
        /**
         * The former returns a live data containing a list of work info and the later converts it to a live data containing a single work info
         */
        return MediatorLiveData<WorkInfo.State>().apply {
            addSource(work.workInfosLiveData) { workInfoList ->
                workInfoList.forEachIndexed { index, workInfo ->
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        if (index == workInfoList.size - 1) {
                            value = workInfo.state
                        } else {
                            return@forEachIndexed
                        }
                    } else {
                        value = workInfo.state
                        return@addSource
                    }
                }
            }
        }
    }

}