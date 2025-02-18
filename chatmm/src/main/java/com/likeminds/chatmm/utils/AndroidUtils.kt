package com.likeminds.chatmm.utils

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.util.TypedValue
import com.likeminds.chatmm.media.customviews.WrappedDrawable
import com.likeminds.chatmm.media.model.*
import com.likeminds.chatmm.utils.file.model.LocalAppData
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import kotlin.math.ceil

object AndroidUtils {

    private var density = 1f

    fun dp(value: Float, context: Context): Int {
        if (density == 1f) {
            checkDisplaySize(context)
        }
        return if (value == 0f) {
            0
        } else ceil((density * value).toDouble()).toInt()
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    private fun checkDisplaySize(context: Context) {
        try {
            density = context.resources.displayMetrics.density
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
        }
    }

    fun getPixels(dipValue: Int, context: Context): Int {
        val r: Resources = context.resources
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, dipValue.toFloat(), r.displayMetrics
        ).toInt()
    }

    fun moveAttachmentToCache(
        context: Context,
        vararg data: SingleUriData
    ): List<SingleUriData> {
        return data.mapNotNull { singleUriData ->
            val uri = when (singleUriData.fileType) {
                IMAGE -> {
                    FileUtil.getSharedImageUri(context, singleUriData.uri)
                }
                GIF -> {
                    FileUtil.getSharedGifUri(context, singleUriData.uri)
                }
                VIDEO -> {
                    FileUtil.getSharedVideoUri(context, singleUriData.uri)
                }
                PDF -> {
                    FileUtil.getSharedPdfUri(context, singleUriData.uri)
                }
                else -> null
            }
            if (uri != null) {
                singleUriData.toBuilder().uri(uri).build()
            } else {
                null
            }
        }
    }

    /**
     * Returns the list of apps which can be used pick images using intent
     * */
    fun getExternalImagePickerApps(context: Context): List<LocalAppData> {
        val intent = getExternalImagePickerIntent()
        return getLocalAppData(context, intent)
    }

    /**
     * Returns the list of apps which can be used pick videos using intent
     * */
    fun getExternalVideoPickerApps(context: Context): List<LocalAppData> {
        val intent = getExternalVideoPickerIntent()
        return getLocalAppData(context, intent)
    }

    /**
     * Returns the list of apps which can be used pick both images and videos using intent
     * */
    fun getExternalMediaPickerApps(context: Context): List<LocalAppData> {
        val intent = getExternalMediaPickerIntent()
        return getLocalAppData(context, intent)
    }


    /**
     * Returns the list of apps with basic information which can be queried for a particular intent
     * */
    private fun getLocalAppData(context: Context, intent: Intent): List<LocalAppData> {
        val packageManager = context.packageManager
        return packageManager.queryIntentActivities(intent, 0)
            .mapIndexedNotNull { index, resolveInfo ->
                val drawable = WrappedDrawable(resolveInfo.loadIcon(packageManager))
                drawable.setBounds(0, 0, dpToPx(50), dpToPx(50))
                LocalAppData(
                    index,
                    resolveInfo.loadLabel(packageManager).toString(),
                    drawable,
                    resolveInfo
                )
            }
    }

    /**
     * Returns the Intent to pick specific mediaTypes files from external storage
     * @param mediaTypes - All the mediaTypes for which intent will be called
     * @param allowMultipleSelect - Specify if multiple media files can be selected
     * @param browseClassName - Specify class package and class name of a specific app which needs to be called
     * */
    fun getExternalPickerIntent(
        mediaTypes: List<String>,
        allowMultipleSelect: Boolean,
        browseClassName: Pair<String, String>?
    ): Intent? {
        val intent = when {
            InternalMediaType.isBothImageAndVideo(mediaTypes) -> {
                getExternalMediaPickerIntent(allowMultipleSelect)
            }
            InternalMediaType.isImage(mediaTypes) -> {
                getExternalImagePickerIntent(allowMultipleSelect)
            }
            InternalMediaType.isVideo(mediaTypes) -> {
                getExternalVideoPickerIntent(allowMultipleSelect)
            }
            else -> null
        }
        if (intent != null && browseClassName != null) {
            intent.setClassName(browseClassName.first, browseClassName.second)
        }
        return intent
    }

    /**
     * Returns the Intent to pick images from external storage
     * */
    private fun getExternalImagePickerIntent(allowMultipleSelect: Boolean = true): Intent {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleSelect)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        return intent
    }

    /**
     * Returns the Intent to pick videos from external storage
     * */
    private fun getExternalVideoPickerIntent(allowMultipleSelect: Boolean = true): Intent {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "video/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleSelect)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        return intent
    }

    /**
     * Returns the Intent to pick both images and videos from external storage
     * */
    private fun getExternalMediaPickerIntent(allowMultipleSelect: Boolean = true): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleSelect)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        return intent
    }

    /**
     * Returns the Intent to pick pdfs from external storage
     * */
    fun getExternalDocumentPickerIntent(allowMultipleSelect: Boolean = true): Intent {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "application/pdf"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultipleSelect)
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        return intent
    }

    fun startDocumentViewer(context: Context, uri: Uri) {
        val pdfIntent = Intent(Intent.ACTION_VIEW, uri)
        pdfIntent.flags = Intent.FLAG_ACTIVITY_NO_HISTORY or Intent.FLAG_GRANT_READ_URI_PERMISSION
        try {
            context.startActivity(pdfIntent)
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
            ViewUtils.showShortToast(context, "No application found to open this document")
        }
    }
}