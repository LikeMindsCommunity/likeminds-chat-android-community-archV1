package com.likeminds.chatmm.utils.mediauploader.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileHelper {
    private const val TAG = "FileHelper"

    fun compressFile(applicationContext: Context, filePath: String): File? {
        try {
            val oldExifOrientation =
                ExifInterface(filePath).getAttribute(ExifInterface.TAG_ORIENTATION)
            val bitmap = BitmapFactory.decodeFile(filePath) ?: return null
            val imagesFolder = File(applicationContext.cacheDir, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
            stream.flush()
            stream.close()
            // Update the old image orientation attributes to the compressed one
            if (oldExifOrientation != null) {
                val newExif = ExifInterface(file.absolutePath)
                newExif.setAttribute(ExifInterface.TAG_ORIENTATION, oldExifOrientation)
                newExif.saveAttributes()
            }
            return file
        } catch (e: IOException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            Log.e(
                TAG,
                "IOException while trying to compress file: " + e.localizedMessage
            )
            return null
        }
    }
}