package com.likeminds.chatmm.media.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Log
import androidx.core.content.res.ResourcesCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.likeminds.chatmm.R
import com.likeminds.chatmm.media.model.MediaViewData
import com.likeminds.chatmm.media.model.SingleUriData
import com.likeminds.chatmm.member.util.MemberImageUtil
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.chatmm.utils.file.util.FileUtil
import com.likeminds.chatmm.utils.file.util.FileUtil.isLargeFile
import com.likeminds.chatmm.utils.file.util.MemoryUnitFormat
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import kotlinx.coroutines.*

object MediaUtils {

    private const val TAG = "MediaUtils"

    /**
     * Fetches the pdf preview using Android's core [PdfRenderer]
     */
    fun getDocumentPreview(context: Context, uri: Uri): Uri? {
        try {
            //creates a file descriptor
            val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null

            //creates an object of renderer of pdf
            val renderer = PdfRenderer(pfd)

            if (renderer.pageCount >= 0) {
                //opens first page
                val page = renderer.openPage(0)

                //create bitmap for the preview
                val bitmap = Bitmap.createBitmap(
                    page.width,
                    page.height,
                    Bitmap.Config.ARGB_8888
                )

                //Make background white, if pdf is transparent
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                //render first page into the bitmap
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                //close the page
                page.close()

                //close the renderer
                renderer.close()

                //convert the bitmap into uri and return
                return FileUtil.getUriFromBitmapWithRandomName(context, bitmap)
            }
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
            Log.e(TAG, "getDocumentPreview", e)
        }
        return null
    }

    fun getTextSizes(): Array<Float> {
        return arrayOf(
            ViewUtils.spToPx(16), ViewUtils.spToPx(20), ViewUtils.spToPx(24),
            ViewUtils.spToPx(28), ViewUtils.spToPx(32), ViewUtils.spToPx(36)
        )
    }

    fun getTextTypeFaces(context: Context): Array<Typeface?> {
        val regular = ResourcesCompat.getFont(context, R.font.lm_chat_roboto_regular)
        val bold = ResourcesCompat.getFont(context, R.font.lm_chat_roboto_bold)
        val signature = ResourcesCompat.getFont(context, R.font.lm_chat_signature)
        val condensed = ResourcesCompat.getFont(context, R.font.lm_chat_roboto_condensed_bold)
        return arrayOf(regular, bold, signature, condensed)
    }

    fun getTextIcons(): Array<Int> {
        return arrayOf(
            R.drawable.lm_chat_ic_text,
            R.drawable.lm_chat_ic_text_bold,
            R.drawable.lm_chat_ic_text_signature,
            R.drawable.lm_chat_ic_text_condensed
        )
    }

    /**
     * Return the file size in string format e.g. 10 MB
     * */
    fun getFileSizeText(sizeBytes: Long): String {
        return MemoryUnitFormat.formatBytes(sizeBytes)
    }

    fun isVideoType(contentType: String?): Boolean {
        return null != contentType && contentType.startsWith("video/")
    }

    fun isImageType(contentType: String?): Boolean {
        return null != contentType && contentType.startsWith("image/")
    }

    fun isPdfType(contentType: String?): Boolean {
        return null != contentType && contentType == "application/pdf"
    }

    fun isAudioType(contentType: String?): Boolean {
        return null != contentType && contentType.startsWith("audio/")
    }

    fun formatSeconds(timeInSeconds: Int): String {
        val hours = timeInSeconds / 3600
        val secondsLeft = timeInSeconds - hours * 3600
        val minutes = secondsLeft / 60
        val seconds = secondsLeft - minutes * 60
        var formattedTime = ""
        if (hours in 1..9) formattedTime += "0"
        if (hours > 0)
            formattedTime += "$hours:"
        if (minutes < 10) formattedTime += "0"
        formattedTime += "$minutes:"
        if (seconds < 10) formattedTime += "0"
        formattedTime += seconds
        if (formattedTime.startsWith("00")) {
            return formattedTime.substring(1)
        }
        return formattedTime
    }

    fun convertMediaViewDataToSingleUriData(
        context: Context,
        medias: List<MediaViewData>?
    ): ArrayList<SingleUriData> {
        var largeFileSelected = false
        val mediaUris = arrayListOf<SingleUriData>()

        if (!medias.isNullOrEmpty()) {
            medias.forEach { mediaViewData ->
                if (!mediaViewData.size.isLargeFile) {
                    mediaUris.add(
                        SingleUriData.Builder()
                            .mediaName(mediaViewData.mediaName)
                            .uri(mediaViewData.uri)
                            .fileType(mediaViewData.mediaType)
                            .size(mediaViewData.size)
                            .duration(mediaViewData.duration)
                            .mediaName(mediaViewData.mediaName)
                            .duration(mediaViewData.duration)
                            .pdfPageCount(mediaViewData.pdfPageCount)
                            .build()
                    )
                } else {
                    largeFileSelected = true
                }
            }
        }

        if (largeFileSelected) {
            ViewUtils.showShortToast(
                context, context.getString(R.string.lm_chat_large_file_select_error_message)
            )
        }
        return mediaUris
    }

    fun getExternalIntentPickerUris(data: Intent?): ArrayList<Uri> {
        val uris = arrayListOf<Uri>()
        if (data != null) {
            val mediaUriCount = data.clipData?.itemCount ?: 0
            if (mediaUriCount > 0) {
                for (i in 0 until mediaUriCount) {
                    val mediaUri = data.clipData?.getItemAt(i)?.uri
                    if (mediaUri != null) uris.add(mediaUri)
                }
            } else {
                val mediaUri = data.data
                if (mediaUri != null) uris.add(mediaUri)
            }
        }
        return uris
    }


    fun fetchImage(
        context: Context,
        image: String?,
        isRound: Boolean = false,
        px: Int? = null,
        callback: (bitmap: Bitmap?) -> Unit
    ) {
        if (image.isNullOrBlank()) {
            callback(null)
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            callback(getBitmap(context, image, isRound, px))
        }
    }

    /**
     * Fetches the Uris asynchronously
     */
    fun fetchImageUri(
        context: Context,
        image: String?,
        callback: (bitmap: Uri?) -> Unit
    ) {
        if (image.isNullOrBlank()) {
            callback(null)
            return
        }
        CoroutineScope(Dispatchers.Default).launch {
            callback(
                FileUtil.getUriFromBitmapWithRandomName(
                    context,
                    getBitmap(context, image),
                    true
                )
            )
        }
    }

    /**
     * Fetches the Bitmap synchronously. Be sure to call it from a background thread
     */
    private fun getBitmap(
        context: Context,
        url: String?,
        isRound: Boolean = false,
        px: Int? = null
    ): Bitmap? {
        return try {
            var glideBuilder = Glide.with(context).asBitmap().load(url)
            if (isRound) {
                glideBuilder = glideBuilder.apply(RequestOptions().circleCrop())
            }
            if (px != null) {
                glideBuilder = glideBuilder.override(MemberImageUtil.SIXTY_PX)
            }
            glideBuilder.submit().get()
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            e.printStackTrace()
            null
        }
    }
}