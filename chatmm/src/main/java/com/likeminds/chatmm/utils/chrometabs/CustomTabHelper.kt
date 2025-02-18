package com.likeminds.chatmm.utils.chrometabs

import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import androidx.core.content.ContextCompat
import com.likeminds.chatmm.utils.ViewUtils
import com.likeminds.likemindschat.helper.LMChatLogger
import com.likeminds.likemindschat.helper.model.LMSeverity

internal object CustomTabHelper {

    fun getBitmapFromVectorDrawable(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    fun openLinkViaBrowser(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(Intent.createChooser(intent, "View link"))
        } catch (e: ActivityNotFoundException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
            ViewUtils.showShortToast(context, "No app to open the link")
        }
    }

    fun copyToClipboard(context: Context, uri: Uri?) {
        val clipboardManager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", uri.toString())
        clipboardManager.setPrimaryClip(clip)
        ViewUtils.showShortToast(context, "Link copied")
    }

    fun shareLink(context: Context, uri: Uri?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, uri.toString())
        try {
            val chooserIntent = Intent.createChooser(intent, "Share link")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooserIntent)
        } catch (e: ActivityNotFoundException) {
            LMChatLogger.getInstance()?.handleException(
                e.message ?: "",
                e.stackTraceToString(),
                LMSeverity.CRITICAL
            )
            e.printStackTrace()
            ViewUtils.showShortToast(context, "No app to share the link")
        }
    }
}