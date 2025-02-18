package com.likeminds.chatmm.utils.file.util

import android.content.ContentResolver
import android.net.Uri
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

object ContentUriUtils {

    /**
     * Get the value of the column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param uri to Query
     * @param column
     * @param selection Optional Filter used in the query
     * @param selectionArgs Optional arguments used in the query
     * @return Value of the column, which is typically a file path or null.
     */
    fun getPathFromColumn(
        contentResolver: ContentResolver,
        uri: Uri,
        column: String,
        selection: String? = null,
        selectionArgs: Array<String?>? = null
    ): String {
        var path = ""
        val projection = arrayOf<String?>(column)
        try {
            getCursor(contentResolver, uri, projection, selection, selectionArgs)
                ?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndexOrThrow(column)
                        path = it.getString(index)
                    }
                }
        } catch (e: Exception) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.EMERGENCY
            )
            e.message?.let {
                //Checks whether the exception message does not contain the following string.
                if (!it.contains("column '$column' does not exist")) {
                    throw e
                }
            }
        } finally {
            return path
        }
    }

    /**
     * Helper for get cursor
     *
     */
    fun getCursor(
        contentResolver: ContentResolver,
        uri: Uri,
        projection: Array<String?>? = null,
        selection: String? = null,
        selectionArgs: Array<String?>? = null
    ) = contentResolver.query(uri, projection, selection, selectionArgs, null)
}